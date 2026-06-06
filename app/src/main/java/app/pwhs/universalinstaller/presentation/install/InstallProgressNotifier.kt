package app.pwhs.universalinstaller.presentation.install

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.repository.SessionDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.progress
import ru.solrudev.ackpine.session.state
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Posts and updates a persistent notification for installs that the user backgrounded
 * from [DialogInstallActivity]. Lives at the process scope, independent of any
 * activity/VM scope so the install can complete (and be reported) after the dialog
 * activity is destroyed.
 *
 * - One progress notification (NOTIF_ID_PROGRESS), updated as sessions move. When
 *   multiple sessions are active, the text rolls up to "Đang cài N ứng dụng" with the
 *   most recent app's icon as the avatar.
 * - One result notification per completed install (auto-incrementing id), shown after
 *   Success/Failed, so the user can see what finished even after dismissing the
 *   progress notification.
 * - Tapping any notification reopens [DialogInstallActivity] with the session id as
 *   an extra so it can restore the Installing/Success/Failed state.
 */
class InstallProgressNotifier(
    private val context: Context,
    private val packageInstaller: PackageInstaller,
    private val sessionDataRepository: SessionDataRepository,
) {
    private data class TrackedInstall(
        val sessionId: UUID,
        val packageName: String,
        val appName: String,
        val iconPath: String?,
        var progress: Int = 0,
        var indeterminate: Boolean = true,
        var watcher: Job? = null,
    )

    private val tracked = ConcurrentHashMap<UUID, TrackedInstall>()
    private val nm = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        ensureChannel()
    }

    fun track(sessionId: UUID, packageName: String, appName: String, iconPath: String?) {
        if (tracked.containsKey(sessionId)) return
        val entry = TrackedInstall(sessionId, packageName, appName, iconPath)
        tracked[sessionId] = entry
        entry.watcher = scope.launch {
            val session = packageInstaller.getSession(sessionId)
            if (session == null) {
                Timber.w("InstallProgressNotifier: session $sessionId not found at track time")
                tracked.remove(sessionId)
                refreshProgressNotification()
                return@launch
            }
            session.progress
                .onEach { p ->
                    val pct = if (p.max > 0) (p.progress * 100 / p.max).coerceIn(0, 100) else 0
                    entry.progress = pct
                    entry.indeterminate = p.max <= 0
                    // Keep the repo in sync so a reopened dialog sees real progress —
                    // the controller's own awaitSession watcher died with the VM scope.
                    sessionDataRepository.updateSessionProgress(sessionId, p)
                    refreshProgressNotification()
                }
                .launchIn(this)
            session.state
                .onEach { state ->
                    when (state) {
                        is Session.State.Succeeded -> {
                            // Remove from repo so a reopened dialog reads this as Success.
                            sessionDataRepository.removeSessionData(sessionId)
                            finishTracked(entry, success = true, errorText = null)
                        }
                        is Session.State.Failed<*> -> {
                            val failure = state.failure
                            val msg = if (failure is ru.solrudev.ackpine.installer.InstallFailure) {
                                val info = InstallErrorHelper.getErrorInfo(context, failure)
                                info.title
                            } else {
                                runCatching { failure.toString() }.getOrNull() ?: "Install failed"
                            }
                            sessionDataRepository.setError(sessionId, ResolvableString.raw(msg))
                            finishTracked(entry, success = false, errorText = msg)
                        }
                        Session.State.Cancelled -> {
                            // Treat as a quiet failure — surface in the result notif so the user
                            // knows the install didn't complete. Don't push an error string to
                            // the repo (the dialog already treats Cancelled as user-initiated).
                            sessionDataRepository.removeSessionData(sessionId)
                            finishTracked(
                                entry,
                                success = false,
                                errorText = context.getString(R.string.install_error_cancelled_title),
                            )
                        }
                        else -> Unit // Pending/Active/Awaiting/Committed handled by progress flow
                    }
                }
                .launchIn(this)
        }
        refreshProgressNotification()
    }

    fun untrack(sessionId: UUID) {
        val entry = tracked.remove(sessionId) ?: return
        entry.watcher?.cancel()
        if (tracked.isEmpty()) {
            nm.cancel(NOTIF_ID_PROGRESS)
        } else {
            refreshProgressNotification()
        }
    }

    private fun finishTracked(entry: TrackedInstall, success: Boolean, errorText: String?) {
        tracked.remove(entry.sessionId)
        entry.watcher?.cancel()
        postResultNotification(entry, success, errorText)
        if (tracked.isEmpty()) {
            nm.cancel(NOTIF_ID_PROGRESS)
        } else {
            refreshProgressNotification()
        }
    }

    private fun refreshProgressNotification() {
        if (!canPost()) return
        val active = tracked.values.toList()
        if (active.isEmpty()) {
            nm.cancel(NOTIF_ID_PROGRESS)
            return
        }
        val newest = active.last()
        val (title, text, max, prog, indet) = if (active.size == 1) {
            val a = active.first()
            ProgressArgs(
                title = context.getString(R.string.install_notif_progress_title_single, a.appName.ifBlank { a.packageName }),
                text = context.getString(R.string.install_notif_progress_text, a.progress),
                max = 100,
                progress = a.progress,
                indeterminate = a.indeterminate,
            )
        } else {
            val avg = active.map { it.progress }.average().toInt()
            ProgressArgs(
                title = context.getString(R.string.install_notif_progress_title_multi, active.size),
                text = active.joinToString(", ") { it.appName.ifBlank { it.packageName } },
                max = 100,
                progress = avg,
                indeterminate = active.any { it.indeterminate },
            )
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_gradient)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(max, prog, indet)
            .setContentIntent(buildOpenDialogIntent(newest.sessionId))
        loadIconBitmap(newest.iconPath)?.let { builder.setLargeIcon(it) }
        nm.notify(NOTIF_ID_PROGRESS, builder.build())
    }

    private fun postResultNotification(entry: TrackedInstall, success: Boolean, errorText: String?) {
        if (!canPost()) return
        val title = if (success) {
            context.getString(R.string.install_notif_result_success_title)
        } else {
            context.getString(R.string.install_notif_result_failed_title)
        }
        val appLabel = entry.appName.ifBlank { entry.packageName }
        val text = if (success) appLabel else errorText?.takeIf { it.isNotBlank() }?.let { "$appLabel · $it" } ?: appLabel
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_gradient)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(buildResultIntent(entry, success))
        loadIconBitmap(entry.iconPath)?.let { builder.setLargeIcon(it) }
        nm.notify(nextResultId(), builder.build())
    }

    private fun buildOpenDialogIntent(sessionId: UUID): PendingIntent {
        // Reopen the full install screen — it already shows the session list with progress
        // for every active install. Restoring the dialog state from a sessionId alone is
        // tricky (no URI to re-parse), so we route to InstallActivity instead.
        val intent = Intent(context, InstallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildResultIntent(entry: TrackedInstall, success: Boolean): PendingIntent {
        // On Success, try to launch the installed app directly. Fall back to reopening the dialog.
        if (success && entry.packageName.isNotBlank()) {
            val launch = runCatching { context.packageManager.getLaunchIntentForPackage(entry.packageName) }
                .getOrNull()
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return PendingIntent.getActivity(
                    context,
                    entry.sessionId.hashCode() xor 0x5151,
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        }
        return buildOpenDialogIntent(entry.sessionId)
    }

    private fun loadIconBitmap(iconPath: String?): android.graphics.Bitmap? {
        if (iconPath.isNullOrBlank()) return null
        return runCatching {
            if (!File(iconPath).exists()) return@runCatching null
            BitmapFactory.decodeFile(iconPath)
        }.getOrNull()
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return nm.areNotificationsEnabled()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sys = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (sys.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.install_notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.install_notif_channel_description)
            setShowBadge(false)
        }
        sys.createNotificationChannel(channel)
    }

    private data class ProgressArgs(
        val title: String,
        val text: String,
        val max: Int,
        val progress: Int,
        val indeterminate: Boolean,
    )

    companion object {
        private const val CHANNEL_ID = "install_progress"
        private const val NOTIF_ID_PROGRESS = 5000
        private val resultIdSeq = AtomicInteger(5001)
        private fun nextResultId(): Int = resultIdSeq.incrementAndGet()
    }
}
