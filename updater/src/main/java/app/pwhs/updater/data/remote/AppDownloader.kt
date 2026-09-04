package app.pwhs.updater.data.remote

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder

/**
 * Downloads release APK files with true network-streaming real-time progress reporting.
 */
class AppDownloader(
    private val context: Context,
    private val client: HttpClient = HttpClient(CIO) {
        followRedirects = true
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000 // 10 mins for large APKs
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    },
) {

    suspend fun downloadApk(
        downloadUrl: String,
        packageName: String,
        versionName: String,
        apiToken: String? = null,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = updatesDownloadDir().apply { mkdirs() }
            var outputFile = File(
                downloadDir,
                uniqueFileName(downloadDir, fallbackFileName(packageName, versionName)),
            )

            client.prepareGet(downloadUrl) {
                header("User-Agent", "UniversalInstaller-AppUpdater")
                if (!apiToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $apiToken")
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw RuntimeException("Download failed with HTTP status: ${response.status.value}")
                }

                val totalBytes = response.contentLength() ?: -1L
                val responseFileName = response.headers[HttpHeaders.ContentDisposition]
                    ?.let { parseFileNameFromContentDisposition(it) }
                    ?: downloadUrl.substringAfterLast('/').substringBefore('?')
                if (responseFileName.isNotBlank()) {
                    outputFile = File(
                        downloadDir,
                        uniqueFileName(downloadDir, sanitizeFileName(responseFileName)),
                    )
                }
                val channel: ByteReadChannel = response.bodyAsChannel()

                var downloadedBytes = 0L
                val buffer = ByteArray(16 * 1024) // 16KB chunk buffer
                var lastReportTime = 0L

                FileOutputStream(outputFile).use { output ->
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        if (bytesRead <= 0) break

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (
                            now - lastReportTime >= 50L ||
                            channel.isClosedForRead ||
                            (totalBytes > 0 && downloadedBytes >= totalBytes)
                        ) {
                            lastReportTime = now
                            onProgress(downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }

                // Final 100% progress callback
                onProgress(downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes)
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)
                Result.success(outputFile)
            } else {
                Result.failure(IllegalStateException("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading APK from $downloadUrl")
            Result.failure(e)
        }
    }

    private fun updatesDownloadDir(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            UPDATES_DOWNLOADS_SUBFOLDER,
        )
    }

    private fun fallbackFileName(packageName: String, versionName: String): String {
        return sanitizeFileName("${packageName}_${versionName.replace('/', '_')}.apk")
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .ifBlank { "download.apk" }
        return if (sanitized.substringAfterLast('.', "").isBlank()) "$sanitized.apk" else sanitized
    }

    private fun uniqueFileName(dir: File, desired: String): String {
        if (!File(dir, desired).exists()) return desired
        val dot = desired.lastIndexOf('.')
        val stem = if (dot > 0) desired.take(dot) else desired
        val ext = if (dot > 0) desired.substring(dot) else ""
        var i = 1
        while (File(dir, "$stem ($i)$ext").exists()) i++
        return "$stem ($i)$ext"
    }

    private fun parseFileNameFromContentDisposition(header: String): String? {
        val starRegex = Regex("""filename\*\s*=\s*(?:[^']*'[^']*')?([^;\n]+)""", RegexOption.IGNORE_CASE)
        starRegex.find(header)?.groupValues?.get(1)?.let { raw ->
            val trimmed = raw.trim().trim('"')
            runCatching { return URLDecoder.decode(trimmed, Charsets.UTF_8.name()) }
        }
        val regex = Regex("""filename\s*=\s*"?([^";\n]+)"?""", RegexOption.IGNORE_CASE)
        return regex.find(header)?.groupValues?.get(1)?.trim()
    }

    companion object {
        private const val UPDATES_DOWNLOADS_SUBFOLDER = "UniversalInstaller"
    }
}
