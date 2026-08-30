package app.pwhs.core.install

import android.content.Context
import android.net.Uri
import app.pwhs.core.util.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.ApkSplits.validate
import ru.solrudev.ackpine.splits.CloseableSequence
import ru.solrudev.ackpine.splits.SplitPackage.Companion.toSplitPackage
import ru.solrudev.ackpine.splits.ZippedApkSplits
import ru.solrudev.ackpine.splits.get
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Silent installer for rooted TV boxes. Stages the APK (or each split of a bundle) into the
 * app's cache — readable by the uid-0 shell — then drives `pm install-create -r` →
 * `pm install-write` → `pm install-commit` from the root shell. Committing from the shell
 * installs with **no system confirmation dialog**, unlike the [ApkInstaller] session path.
 *
 * Mirrors the mobile app's proven `RootTargetedInstaller` command sequence (path-arg write with
 * a `cat | ... -` pipe fallback for older `pm`), trimmed of the mobile-only preference plumbing.
 * Returns the same [ApkInstaller.Result] so callers can treat the two backends uniformly.
 */
class RootInstaller(private val context: Context) {

    private companion object {
        // "Success: created install session [1234567]" — anchor on "session" to skip stray brackets.
        val SESSION_ID = Regex("""session\s*\[(\d+)]""", RegexOption.IGNORE_CASE)
    }

    /**
     * Install from [uri]. [isBundle] true unzips split APKs and writes each into one session.
     * [onProgress] reports a 0f..1f fraction: staging (the byte-heavy copy) drives the first
     * 80%, the commit the rest. Safe to call off the main thread.
     */
    suspend fun install(
        uri: Uri,
        isBundle: Boolean,
        onProgress: (Float) -> Unit = {},
    ): ApkInstaller.Result = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "root_install_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val splits = stage(uri, isBundle, stagingDir, onProgress)
            require(splits.isNotEmpty()) { "No APK found to install" }

            val sessionId = createSession()
            splits.forEachIndexed { idx, file ->
                writeSplit(sessionId, idx, file)
                onProgress(0.8f + 0.15f * ((idx + 1f) / splits.size))
            }
            commitSession(sessionId)
            onProgress(1f)
            ApkInstaller.Result.Success
        } catch (t: Throwable) {
            ApkInstaller.Result.Failure(t.message ?: t::class.java.simpleName)
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    /** Copies the source into world-readable cache files so the root shell can read them. */
    private suspend fun stage(uri: Uri, isBundle: Boolean, dir: File, onProgress: (Float) -> Unit): List<File> {
        dir.setReadable(true, false)
        dir.setExecutable(true, false)
        val files = if (isBundle) {
            val ackpineFiles = runCatching {
                val splitPackage = ZippedApkSplits.getApksForUri(uri, context)
                    .validate()
                    .toSplitPackage()
                    .filterCompatible(context)
                val sequence = splitPackage.get()
                val entries = try {
                    sequence.toList()
                } finally {
                    (sequence as? CloseableSequence<*>)?.close()
                }
                val out = mutableListOf<File>()
                for (entry in entries) {
                    val apk = entry.apk
                    val name = if (apk is Apk.Base) "base.apk" else apk.name.substringAfterLast('/')
                    val f = File(dir, name)
                    context.contentResolver.openInputStream(apk.uri)?.use { input ->
                        f.outputStream().use { input.copyTo(it) }
                    }
                    if (f.exists() && f.length() > 0) out += f
                }
                out
            }.getOrNull()

            if (!ackpineFiles.isNullOrEmpty()) {
                ackpineFiles
            } else {
                var index = 0
                val out = mutableListOf<File>()
                ZipInputStream(openInput(uri).buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val rawName = entry.name.substringAfterLast('/')
                        if (!entry.isDirectory && rawName.endsWith(".apk", ignoreCase = true)) {
                            val fName = if (index == 0 || rawName.equals("base.apk", ignoreCase = true)) {
                                "base.apk"
                            } else if (rawName.startsWith("split_", ignoreCase = true)) {
                                rawName
                            } else {
                                "split_$index.apk"
                            }
                            index++
                            val f = File(dir, fName)
                            f.outputStream().use { zip.copyTo(it) }
                            out += f
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                out
            }
        } else {
            val f = File(dir, "base.apk")
            openInput(uri).use { input -> f.outputStream().use { input.copyTo(it) } }
            listOf(f)
        }
        files.forEach { it.setReadable(true, false) }
        onProgress(0.8f)
        return files
    }

    private fun openInput(uri: Uri) =
        context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open $uri")

    private suspend fun createSession(): String {
        val r = RootShell.exec("pm install-create -r")
        if (!r.isSuccess) throw RuntimeException("pm install-create failed: ${r.output.trim()}")
        return SESSION_ID.find(r.output)?.groupValues?.get(1)
            ?: throw RuntimeException("Could not parse session id from: ${r.output.trim()}")
    }

    private suspend fun writeSplit(sessionId: String, index: Int, file: File) {
        val size = file.length()
        val path = file.absolutePath.replace(" ", "\\ ")
        val name = file.nameWithoutExtension
        var r = RootShell.exec("pm install-write -S $size $sessionId $name $path")
        if (!r.isSuccess) {
            // Older `pm` rejects the path-arg form — pipe the bytes in via stdin instead.
            r = RootShell.exec("cat $path | pm install-write -S $size $sessionId $name -")
            if (!r.isSuccess) throw RuntimeException("pm install-write failed for $name: ${r.output.trim()}")
        }
    }

    private suspend fun commitSession(sessionId: String) {
        val r = RootShell.exec("pm install-commit $sessionId")
        // `pm` prints soft failures ("Failure [INSTALL_FAILED_...]") with exit 0, so check the token too.
        if (!r.isSuccess || !r.output.contains("Success", ignoreCase = true)) {
            throw RuntimeException(r.output.trim().ifBlank { "pm install-commit failed" })
        }
    }
}
