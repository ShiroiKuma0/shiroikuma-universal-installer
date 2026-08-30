package app.pwhs.updater.data.remote

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Downloads release APK files with progress reporting.
 */
class AppDownloader(
    private val context: Context,
    private val client: HttpClient = HttpClient(CIO) {
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
            val downloadDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outputFile = File(downloadDir, "${packageName}_${versionName.replace('/', '_')}.apk")

            val response = client.get(downloadUrl) {
                header("User-Agent", "UniversalInstaller-AppUpdater")
                if (!apiToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $apiToken")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    RuntimeException("Download failed with HTTP status: ${response.status.value}")
                )
            }

            val totalBytes = response.contentLength() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()

            var downloadedBytes = 0L
            FileOutputStream(outputFile).use { output ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        val bytes = packet.readByteArray()
                        output.write(bytes)
                        downloadedBytes += bytes.size
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                Result.success(outputFile)
            } else {
                Result.failure(IllegalStateException("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading APK from $downloadUrl")
            Result.failure(e)
        }
    }
}
