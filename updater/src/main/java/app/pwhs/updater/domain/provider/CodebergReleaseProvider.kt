package app.pwhs.updater.domain.provider

import app.pwhs.updater.domain.model.AssetArtifact
import app.pwhs.updater.domain.model.ReleaseDetails
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import timber.log.Timber

/**
 * Update source provider for Codeberg, Gitea, and Forgejo instances using Gitea REST API v1.
 */
class CodebergReleaseProvider(
    private val client: HttpClient = HttpClient(CIO),
) : UpdateSourceProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase().trim()
        return (lower.contains("codeberg.org") || lower.contains("gitea") || lower.contains("forgejo")) &&
            extractHostAndRepo(url) != null
    }

    override suspend fun fetchLatestRelease(
        url: String,
        includePrereleases: Boolean,
        eTag: String?,
        apiToken: String?,
    ): Result<ReleaseDetails?> = withContext(Dispatchers.IO) {
        val (host, repoPath) = extractHostAndRepo(url)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Codeberg/Gitea URL: $url"))

        try {
            val apiUrl = "https://$host/api/v1/repos/$repoPath/releases"

            val response = client.get(apiUrl) {
                header("Accept", "application/json")
                header("User-Agent", "UniversalInstaller-AppUpdater")
                if (!eTag.isNullOrBlank()) {
                    header("If-None-Match", eTag)
                }
                if (!apiToken.isNullOrBlank()) {
                    header("Authorization", "token $apiToken")
                }
            }

            if (response.status == HttpStatusCode.NotModified) {
                return@withContext Result.success(null)
            }

            if (response.status != HttpStatusCode.OK) {
                return@withContext Result.failure(
                    RuntimeException("Codeberg/Gitea API returned error: ${response.status.value} ${response.status.description}")
                )
            }

            val responseBody = response.bodyAsText()
            val newETag = response.headers["ETag"]

            val array = json.parseToJsonElement(responseBody).jsonArray
            if (array.isEmpty()) {
                return@withContext Result.failure(NoSuchElementException("No releases found on Codeberg/Gitea"))
            }

            val targetRelease = if (includePrereleases) {
                array.first().jsonObject
            } else {
                array.firstOrNull { elem ->
                    val isPrerelease = elem.jsonObject["prerelease"]?.jsonPrimitive?.booleanOrNull ?: false
                    !isPrerelease
                }?.jsonObject ?: array.first().jsonObject
            }

            val details = parseReleaseObject(targetRelease, newETag)
            Result.success(details)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch release from Codeberg/Gitea for $url")
            Result.failure(e)
        }
    }

    private fun parseReleaseObject(obj: JsonObject, eTag: String?): ReleaseDetails {
        val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: ""
        val title = obj["name"]?.jsonPrimitive?.content
        val body = obj["body"]?.jsonPrimitive?.content
        val isPrerelease = obj["prerelease"]?.jsonPrimitive?.booleanOrNull ?: false
        val publishedAtStr = obj["published_at"]?.jsonPrimitive?.content

        val assets = obj["assets"]?.jsonArray?.mapNotNull { item ->
            val assetObj = item.jsonObject
            val name = assetObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val downloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val size = assetObj["size"]?.jsonPrimitive?.longOrNull ?: 0L
            AssetArtifact(
                name = name,
                downloadUrl = downloadUrl,
                sizeBytes = size,
            )
        } ?: emptyList()

        val cleanVersion = GitHubReleaseProvider.extractCleanVersion(tagName)

        return ReleaseDetails(
            tagName = tagName,
            versionName = cleanVersion,
            title = title,
            releaseNotes = body,
            publishedAt = parseIsoDate(publishedAtStr),
            isPrerelease = isPrerelease,
            assets = assets,
            eTag = eTag,
        )
    }

    companion object {
        fun extractHostAndRepo(url: String): Pair<String, String>? {
            val clean = url.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")

            val host = clean.substringBefore('/')
            var path = clean.substringAfter('/', "").removeSuffix(".git")
            if (path.contains("/releases")) {
                path = path.substringBefore("/releases")
            }

            val parts = path.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val owner = parts[0]
                val repo = parts[1]
                return Pair(host, "$owner/$repo")
            }
            return null
        }

        private fun parseIsoDate(iso: String?): Long? {
            if (iso.isNullOrBlank()) return null
            return runCatching {
                java.time.Instant.parse(iso).toEpochMilli()
            }.getOrNull()
        }
    }
}
