package app.pwhs.tv.presentation.receive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import app.pwhs.core.receiver.ReceivedApk
import app.pwhs.core.receiver.TvReceiverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File

/**
 * Takes an APK handed to the TV app by another app ("Open with") and feeds it into the same
 * pending-install pipeline the phone-to-TV receiver uses (issue #94).
 *
 * The pipeline is built around [ReceivedApk], which carries a real filesystem path — the metadata
 * reader and both installer backends open it as a file. An incoming `content://` URI has no path,
 * so it is copied into the cache first. That costs a copy, but it is the same thing the network
 * receiver already does, and it also detaches us from a provider that may revoke access the moment
 * the sending app goes away.
 */
object ExternalApkIntake {

    private const val INCOMING_DIR = "incoming"

    /** Marks an intent as already taken, so an Activity recreate doesn't ingest it twice. */
    private const val EXTRA_CONSUMED = "app.pwhs.tv.INTAKE_CONSUMED"

    /** Every URI an install intent can arrive under: VIEW data, SEND stream, or ClipData. */
    fun urisFrom(intent: Intent?): List<Uri> {
        if (intent == null || intent.getBooleanExtra(EXTRA_CONSUMED, false)) return emptyList()
        val out = mutableListOf<Uri>()
        intent.data?.takeIf { it.scheme == "content" || it.scheme == "file" }?.let(out::add)
        @Suppress("DEPRECATION")
        when (intent.action) {
            Intent.ACTION_SEND ->
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let(out::add)
            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.filterNotNull()?.let(out::addAll)
        }
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                val u = clip.getItemAt(i).uri ?: continue
                if (u.scheme == "content" || u.scheme == "file") out.add(u)
            }
        }
        return out.distinct()
    }

    fun markConsumed(intent: Intent?) {
        intent?.putExtra(EXTRA_CONSUMED, true)
    }

    /**
     * Stage [uri] and publish it as the pending install. Returns false if the file could not be
     * read — the caller shows nothing rather than a card for an APK that isn't there.
     */
    suspend fun accept(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = displayName(context, uri)
            val staged = stage(context, uri, fileName) ?: return@withContext false
            TvReceiverState.emitReceived(
                ReceivedApk(
                    path = staged.absolutePath,
                    fileName = fileName,
                    sizeBytes = staged.length(),
                )
            )
            true
        } catch (e: Exception) {
            Log.e("ExternalApkIntake", "Could not take in $uri", e)
            false
        }
    }

    private fun stage(context: Context, uri: Uri, fileName: String): File? {
        // A file:// URI is already on disk and readable — MANAGE_EXTERNAL_STORAGE is declared —
        // so use it where it lies instead of duplicating a package that can be hundreds of MB.
        if (uri.scheme == "file") {
            val existing = uri.path?.let(::File)
            if (existing != null && existing.canRead()) return existing
        }
        val dir = File(context.cacheDir, INCOMING_DIR).apply { mkdirs() }
        // One slot per name: re-opening the same APK overwrites rather than filling the cache.
        val target = File(dir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        return target
    }

    private fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return uri.lastPathSegment ?: "package.apk"
        val fromProvider = runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()
        // The name decides bundle-vs-single downstream (`isBundleName()`), so a provider that
        // reports nothing must not silently turn an APKS into a plain APK.
        return fromProvider?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "package.apk"
    }
}
