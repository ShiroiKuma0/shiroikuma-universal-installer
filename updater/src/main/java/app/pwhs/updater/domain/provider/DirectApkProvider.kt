package app.pwhs.updater.domain.provider

import app.pwhs.updater.domain.model.AssetArtifact
import app.pwhs.updater.domain.model.ReleaseDetails
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Update source provider for direct APK links or self-hosted static files.
 */
class DirectApkProvider(
    private val client: HttpClient = HttpClient(CIO),
) : UpdateSourceProvider {

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.endsWith(".apk") || lower.contains("/download") || lower.contains(".apk?")
    }

    override suspend fun fetchLatestRelease(
        url: String,
        includePrereleases: Boolean,
        eTag: String?,
        apiToken: String?,
    ): Result<ReleaseDetails?> = withContext(Dispatchers.IO) {
        try {
            val response = client.request(url) {
                method = HttpMethod.Head
                header("User-Agent", "UniversalInstaller-AppUpdater")
                if (!eTag.isNullOrBlank()) {
                    header("If-None-Match", eTag)
                }
                if (!apiToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $apiToken")
                }
            }

            if (response.status == HttpStatusCode.NotModified) {
                return@withContext Result.success(null)
            }

            val newETag = response.headers["ETag"] ?: response.headers["Last-Modified"]
            val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: 0L
            val contentDisposition = response.headers["Content-Disposition"]

            val fileName = extractFileName(url, contentDisposition)
            val extractedVersion = extractVersionFromFileName(fileName)

            val details = ReleaseDetails(
                tagName = extractedVersion ?: fileName,
                versionName = extractedVersion ?: fileName,
                title = fileName,
                releaseNotes = "Direct download from $url",
                publishedAt = System.currentTimeMillis(),
                isPrerelease = false,
                assets = listOf(
                    AssetArtifact(
                        name = fileName,
                        downloadUrl = url,
                        sizeBytes = contentLength,
                    )
                ),
                eTag = newETag,
            )

            Result.success(details)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check direct APK update for $url")
            Result.failure(e)
        }
    }

    companion object {
        fun extractFileName(url: String, contentDisposition: String?): String {
            if (!contentDisposition.isNullOrBlank() && contentDisposition.contains("filename=")) {
                val match = Regex("""filename=["']?([^"';]+)["']?""").find(contentDisposition)
                if (match != null && match.groupValues.size >= 2) {
                    return match.groupValues[1]
                }
            }
            val cleanUrl = url.substringBefore('?').substringBefore('#')
            val name = cleanUrl.substringAfterLast('/')
            return if (name.isNotBlank()) name else "app-update.apk"
        }

        fun extractVersionFromFileName(fileName: String): String? {
            val match = Regex("""(?:v|version|[-_])?(\d+\.\d+(?:\.\d+)?(?:-[a-zA-Z0-9.]+)?)(?:\.apk)?""", RegexOption.IGNORE_CASE).find(fileName)
            return match?.groupValues?.getOrNull(1)
        }
    }
}
