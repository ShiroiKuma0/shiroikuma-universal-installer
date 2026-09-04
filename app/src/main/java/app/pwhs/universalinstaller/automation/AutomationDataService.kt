package app.pwhs.universalinstaller.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.setting.ui.UiConfigBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Where a data-door export or import actually runs. Sister-app contract v2 §2a.
 *
 * ## Why a foreground service and not the provider call
 *
 * [AutomationProvider.call] returns in milliseconds; this can run for a while. Two hard reasons it
 * cannot be done anywhere cheaper:
 *
 * - **A binder call holds the caller.** 応用管理 is drawing a list; a multi-second synchronous call
 *   would freeze its UI, report no progress and refuse cancellation.
 * - **A backgrounded app writing for minutes is frozen mid-stream on this phone**, which yields a
 *   truncated archive underneath a success reply — the worst possible failure, because it is
 *   indistinguishable from a good backup until the day it is restored.
 *
 * ## The descriptor
 *
 * Already duplicated by [AutomationProvider] before it got here, because the original belongs to
 * the binder transaction and is closed the moment `call()` returns. This service owns the copy and
 * closes it in a `finally` — leaking one holds the caller's file open, and a caller cannot checksum
 * or encrypt a file that is still open.
 */
class AutomationDataService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val importing = intent?.getBooleanExtra(EXTRA_IMPORTING, false) ?: false
        val jobId = intent?.getStringExtra(EXTRA_JOB)
        val items = intent?.getStringExtra(AutomationProvider.KEY_ITEMS)
        val replyAction = intent?.getStringExtra(AutomationProvider.KEY_REPLY_ACTION)
        val replyPackage = intent?.getStringExtra(AutomationProvider.KEY_REPLY_PACKAGE)
        val progressAction = intent?.getStringExtra(AutomationProvider.KEY_PROGRESS_ACTION)

        val replied = AtomicBoolean(false)
        // A `val` lambda rather than a local `fun`: a local function and a capturing anonymous
        // object in the same method crash AGP lint, after Kotlin, Java and dex have all succeeded.
        // The counter that used to be that anonymous object is now [CountingOutputStream].
        val reply: (String) -> Unit = { result ->
            // Exactly one terminal answer per job, whatever path got here — a synchronous failure
            // and an asynchronous success must never both fire. The same guard the broadcast
            // contract has carried since the first sister app.
            if (replied.compareAndSet(false, true)) {
                jobId?.let(AutomationJobs::finish)
                if (!replyAction.isNullOrEmpty() && !replyPackage.isNullOrEmpty()) {
                    sendBroadcast(
                        Intent(replyAction).apply {
                            setPackage(replyPackage)
                            // Without this a backgrounded caller never hears the answer, and on a
                            // clean phone the caller may not have been launched at all.
                            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                            putExtra(AutomationProvider.KEY_JOB_ID, jobId)
                            putExtra(AutomationProvider.KEY_RESULT, result)
                        },
                    )
                }
            }
        }

        // BEFORE ANY RETURN, and guarded. Once [AutomationDataService.start] has called
        // `startForegroundService`, the platform requires this service to go foreground whatever it
        // then decides, and kills the process with ForegroundServiceDidNotStartInTimeException if it
        // does not — so bailing out early on a stale job id would KILL THIS APP rather than ignore a
        // duplicate request. It must also land within 5 s of the service starting, for the same
        // class of reason. `startForeground` itself can throw when the declared
        // foregroundServiceType disagrees with the manifest, or on API 31+ when a background start
        // is refused — which a provider `call()` always is.
        try {
            startForeground(NOTIFICATION_ID, notification(importing))
        } catch (e: Exception) {
            jobId?.let(::discardHandover)
            reply("ERROR:cannot go foreground: ${e.javaClass.simpleName}")
            return stop(startId)
        }

        // Silent on a stale or already-claimed id: we have no descriptor, and the request this id
        // belonged to has already had its one terminal reply. Answering again would break the
        // single-reply rule that the whole contract rests on.
        val fd = jobId?.let { HANDOVER.remove(it) } ?: return stop(startId)

        scope.launch {
            try {
                if (importing) {
                    runImport(fd, reply)
                } else {
                    runExport(jobId, fd, items, progressAction, replyPackage, reply)
                }
            } catch (t: Throwable) {
                reply("ERROR:${t.message ?: t.javaClass.simpleName}")
            } finally {
                // Ours to close, exactly once. Idempotent on the PFD, but the stream wrappers below
                // may already have done it — hence the runCatching rather than a bare close.
                runCatching { fd.close() }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runExport(
        jobId: String,
        fd: ParcelFileDescriptor,
        items: String?,
        progressAction: String?,
        replyPackage: String?,
        reply: (String) -> Unit,
    ) {
        val selection = resolve(items)
            ?: run { reply("ERROR:unknown category in items: $items"); return }
        if (selection.isEmpty) { reply("ERROR:unknown category in items: $items"); return }

        val appLabel = packageManager.getApplicationLabel(applicationInfo).toString()
        var lastProgressMs = 0L
        val progress: (Int, Int, String, String) -> Unit = { done, total, label, id ->
            // An implicit broadcast has not reached a manifest receiver since API 26, so progress
            // without `setPackage` is not weak progress — it is none, silently.
            val now = SystemClock.elapsedRealtime()
            val due = done >= total || now - lastProgressMs >= PROGRESS_MIN_INTERVAL_MS
            if (!progressAction.isNullOrEmpty() && !replyPackage.isNullOrEmpty() && due) {
                lastProgressMs = now
                sendBroadcast(
                    Intent(progressAction).apply {
                        setPackage(replyPackage)
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        // Both names carry the job id: the data door correlates on `job_id`, while
                        // the §3 progress relay the family already ships reads `reply_id`.
                        putExtra(AutomationProvider.KEY_JOB_ID, jobId)
                        putExtra(StateExportReceiver.EXTRA_REPLY_ID, jobId)
                        putExtra(StateExportReceiver.EXTRA_PROGRESS_APP, appLabel)
                        putExtra(StateExportReceiver.EXTRA_PROGRESS_ITEM, id)
                        putExtra(StateExportReceiver.EXTRA_PROGRESS_TEXT, "$PROGRESS_UNIT $done/$total — $label")
                        putExtra(StateExportReceiver.EXTRA_PROGRESS_CURRENT, done.toLong())
                        putExtra(StateExportReceiver.EXTRA_PROGRESS_TOTAL, total.toLong())
                        putExtra(StateExportReceiver.EXTRA_PROGRESS_UNIT, PROGRESS_UNIT)
                    },
                )
            }
        }

        var written = 0L
        ParcelFileDescriptor.AutoCloseOutputStream(fd).use { out ->
            val counting = CountingOutputStream(out)
            UiConfigBackup.export(
                context = this,
                selection = selection,
                output = counting,
                onProgress = progress,
                isCancelled = { AutomationJobs.isCancelled(jobId) },
            )
            written = counting.count
        }
        if (AutomationJobs.isCancelled(jobId)) reply("ERROR:cancelled")
        else reply("OK:$written|${selection.categoryCount} categories")
    }

    /**
     * Read the whole archive before touching anything.
     *
     * [UiConfigBackup.import] wants the bytes, and that is the right shape here for a reason beyond
     * convenience: a partial read that failed halfway would import half an archive, and a
     * half-restored app is worse than one that refused.
     *
     * The selection is everything: import already iterates the categories the archive actually
     * carries and skips the absent ones, so asking for all of them restores exactly what is there.
     */
    private suspend fun runImport(fd: ParcelFileDescriptor, reply: (String) -> Unit) {
        // Bounded, because the descriptor is the caller's and its length is not ours to trust:
        // an unbounded readBytes() on a hostile or simply wrong fd is an OOM, not an error message.
        // The cap is this app's own archive ceiling — UiConfigBackup already refuses more than this
        // much content — so nothing legitimate is turned away.
        val bytes = ParcelFileDescriptor.AutoCloseInputStream(fd).use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                if (buffer.size().toLong() + n > MAX_IMPORT_BYTES) {
                    reply("ERROR:archive too large")
                    return
                }
                buffer.write(chunk, 0, n)
            }
            buffer.toByteArray()
        }
        if (bytes.isEmpty()) { reply("ERROR:empty archive"); return }
        UiConfigBackup.import(this, bytes, UiConfigBackup.Selection.all())
            .onSuccess { summary -> reply("OK:${summary.replace('\n', ' ')}") }
            .onFailure { reply("ERROR:${it.message ?: it.javaClass.simpleName}") }
        // 応用管理 force-stops us straight after a successful import. That is deliberate and lives
        // on its side: a running process writes its cached preferences back out at orderly shutdown
        // and would silently undo the import that just happened.
    }

    /** `items` absent or blank means this app's default set, which here is every category. */
    private fun resolve(items: String?): UiConfigBackup.Selection? {
        if (items.isNullOrBlank()) return UiConfigBackup.Selection.all()
        val ids = items.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return UiConfigBackup.selectionOf(ids)
    }

    private fun notification(importing: Boolean): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    getString(R.string.automation_data_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val title =
            if (importing) R.string.automation_data_importing else R.string.automation_data_exporting
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(getString(title))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Every return path now runs AFTER `startForeground`, so this leaves the foreground state as
     * well as stopping. Harmless on the path where `startForeground` itself threw.
     */
    private fun stop(startId: Int): Int {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun discardHandover(jobId: String) = discard(jobId)

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "automation_data"
        private const val NOTIFICATION_ID = 9714
        private const val EXTRA_JOB = "job"
        private const val EXTRA_IMPORTING = "importing"
        private const val PROGRESS_MIN_INTERVAL_MS = 500L
        private const val PROGRESS_UNIT = "区分"

        /** Matches [UiConfigBackup]'s own total-content ceiling; see the note at the read. */
        private const val MAX_IMPORT_BYTES = 256L * 1024 * 1024

        /**
         * The descriptor's way across, because an Intent is the wrong vehicle for one.
         *
         * A [ParcelFileDescriptor] in an Intent extra is duplicated by the system on delivery and
         * the copy's lifetime stops being ours to reason about. Handing it through a map keyed by
         * the job id keeps exactly one open descriptor with exactly one owner — this service, which
         * closes it in a `finally`.
         */
        private val HANDOVER = ConcurrentHashMap<String, ParcelFileDescriptor>()

        fun start(
            context: Context,
            jobId: String,
            fd: ParcelFileDescriptor,
            importing: Boolean,
            extras: Bundle?,
        ) {
            HANDOVER[jobId] = fd
            ContextCompat.startForegroundService(
                context,
                Intent(context, AutomationDataService::class.java).apply {
                    putExtra(EXTRA_JOB, jobId)
                    putExtra(EXTRA_IMPORTING, importing)
                    putExtra(
                        AutomationProvider.KEY_ITEMS,
                        extras?.getString(AutomationProvider.KEY_ITEMS),
                    )
                    putExtra(
                        AutomationProvider.KEY_REPLY_ACTION,
                        extras?.getString(AutomationProvider.KEY_REPLY_ACTION),
                    )
                    putExtra(
                        AutomationProvider.KEY_REPLY_PACKAGE,
                        extras?.getString(AutomationProvider.KEY_REPLY_PACKAGE),
                    )
                    putExtra(
                        AutomationProvider.KEY_PROGRESS_ACTION,
                        extras?.getString(AutomationProvider.KEY_PROGRESS_ACTION),
                    )
                },
            )
        }

        /**
         * Drop a descriptor whose service never started, so the handover map cannot become a leak
         * of the caller's open files. Only [AutomationProvider] calls this, on the path where
         * `startForegroundService` itself threw.
         */
        fun discard(jobId: String) {
            HANDOVER.remove(jobId)?.let { runCatching { it.close() } }
        }
    }
}

/**
 * Counts the bytes that actually reach the caller's descriptor.
 *
 * Counted as it goes rather than stat'ed afterwards: the caller owns the file and this app may not
 * be able to see it at all — it can be an anonymous pipe, or a descriptor into a directory this app
 * cannot list.
 *
 * A named top-level class rather than the anonymous `object : OutputStream()` it replaces: an
 * anonymous object capturing a local, sharing a method with a local function or lambda, crashes AGP
 * lint after Kotlin, Java and dex have all succeeded.
 */
private class CountingOutputStream(private val out: OutputStream) : OutputStream() {
    var count: Long = 0L
        private set

    override fun write(b: Int) {
        out.write(b)
        count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        count += len
    }

    override fun flush() = out.flush()
}
