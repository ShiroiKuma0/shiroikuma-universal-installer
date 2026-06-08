package app.pwhs.core.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume

/**
 * Installs an APK (or a split bundle) via [PackageInstaller] — the only install path that
 * works on Android TV, where there is no installer `VIEW` intent and no SAF picker.
 *
 * The commit reports back through a [PendingIntent] broadcast. A non-privileged installer
 * first receives [PackageInstaller.STATUS_PENDING_USER_ACTION] carrying a confirm Intent
 * (the system package-installer UI, D-pad navigable on TV); after the user accepts, the
 * same IntentSender receives the terminal status.
 *
 * Bundles (.apks/.xapk/.apkm/.zip) are unzipped in memory and every contained `.apk` is
 * written into one session, so split apps install in a single transaction.
 */
class ApkInstaller(private val context: Context) {

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    private companion object {
        const val ACTION = "app.pwhs.core.install.STATUS"
        val BUNDLE_EXTS = setOf("apks", "xapk", "apkm", "apk+", "zip")
    }

    /** Convenience for installing a staged file (e.g. an upload received over LAN). */
    suspend fun install(source: File): Result =
        install(Uri.fromFile(source), isBundle = source.extension.lowercase() in BUNDLE_EXTS)

    /**
     * Install from a content/file [uri]. [isBundle] true unzips split APKs into one session.
     * Suspends until the install reaches a terminal state (the user-action confirm screen is
     * launched mid-flow). Safe to call off the main thread.
     */
    suspend fun install(uri: Uri, isBundle: Boolean): Result {
        val pm = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = pm.createSession(params)
        try {
            pm.openSession(sessionId).use { session ->
                if (isBundle) {
                    writeBundle(session, uri)
                } else {
                    openInput(uri).use { writeEntry(session, "base.apk", it, -1L) }
                }
                return commitAndAwait(session, sessionId)
            }
        } catch (t: Throwable) {
            runCatching { pm.abandonSession(sessionId) }
            return Result.Failure(t.message ?: t::class.java.simpleName)
        }
    }

    private fun openInput(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open $uri")

    private fun writeBundle(session: PackageInstaller.Session, bundle: Uri) {
        var wrote = 0
        ZipInputStream(openInput(bundle).buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && name.endsWith(".apk", ignoreCase = true)) {
                    // size unknown for zip entries → use -1 so PackageInstaller streams it.
                    writeEntry(session, "$wrote-$name", zip, -1L)
                    wrote++
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        require(wrote > 0) { "No APK entries found in bundle" }
    }

    private fun writeEntry(session: PackageInstaller.Session, name: String, input: InputStream, size: Long) {
        session.openWrite(name, 0, size).use { out ->
            input.copyTo(out)
            session.fsync(out)
        }
    }

    private suspend fun commitAndAwait(
        session: PackageInstaller.Session,
        sessionId: Int,
    ): Result = suspendCancellableCoroutine { cont ->
        val action = "$ACTION.$sessionId"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val status = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE
                )
                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        @Suppress("DEPRECATION")
                        val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(confirm) }
                        // not terminal — wait for the follow-up status
                    }
                    PackageInstaller.STATUS_SUCCESS -> {
                        runCatching { context.unregisterReceiver(this) }
                        if (cont.isActive) cont.resume(Result.Success)
                    }
                    else -> {
                        runCatching { context.unregisterReceiver(this) }
                        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            ?: "Install failed (status $status)"
                        if (cont.isActive) cont.resume(Result.Failure(msg))
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }

        // FLAG_MUTABLE: the system fills in EXTRA_INTENT for the pending-user-action step.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pi = PendingIntent.getBroadcast(
            context, sessionId, Intent(action).setPackage(context.packageName), flags
        )
        session.commit(pi.intentSender)
    }
}
