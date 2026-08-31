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
import timber.log.Timber
import java.net.URLEncoder

/**
 * Update source provider for GitLab Releases using GitLab REST API v4.
 * Supports gitlab.com and self-hosted GitLab instances.
 */
class GitLabReleaseProvider(
    private val client: HttpClient = HttpClient(CIO),
) : UpdateSourceProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase().trim()
        return (lower.contains("gitlab.com") || lower.contains("/gitlab")) && extractProjectPath(url) != null
    }

    override suspend fun fetchLatestRelease(
        url: String,
        includePrereleases: Boolean,
        eTag: String?,
        apiToken: String?,
    ): Result<ReleaseDetails?> = withContext(Dispatchers.IO) {
        val (host, projectPath) = extractHostAndProjectPath(url)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid GitLab repository URL: $url"))

        try {
            val encodedProjectPath = URLEncoder.encode(projectPath, "UTF-8")
            val apiUrl = "https://$host/api/v4/projects/$encodedProjectPath/releases"

            val response = client.get(apiUrl) {
                header("Accept", "application/json")
                header("User-Agent", "UniversalInstaller-AppUpdater")
                if (!eTag.isNullOrBlank()) {
                    header("If-None-Match", eTag)
                }
                if (!apiToken.isNullOrBlank()) {
                    header("PRIVATE-TOKEN", apiToken)
                }
            }

            if (response.status == HttpStatusCode.NotModified) {
                return@withContext Result.success(null)
            }

            if (response.status != HttpStatusCode.OK) {
                return@withContext Result.failure(
                    RuntimeException("GitLab API returned error: ${response.status.value} ${response.status.description}")
                )
            }

            val responseBody = response.bodyAsText()
            val newETag = response.headers["ETag"]

            val array = json.parseToJsonElement(responseBody).jsonArray
            if (array.isEmpty()) {
                return@withContext Result.failure(NoSuchElementException("No releases found on GitLab"))
            }

            val targetRelease = if (includePrereleases) {
                array.first().jsonObject
            } else {
                array.firstOrNull { elem ->
                    val isUpcoming = elem.jsonObject["upcoming_release"]?.jsonPrimitive?.booleanOrNull ?: false
                    !isUpcoming
                }?.jsonObject ?: array.first().jsonObject
            }

            val details = parseReleaseObject(targetRelease, newETag)
            Result.success(details)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch release from GitLab for $url")
            Result.failure(e)
        }
    }

    private fun parseReleaseObject(obj: JsonObject, eTag: String?): ReleaseDetails {
        val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: ""
        val title = obj["name"]?.jsonPrimitive?.content
        val description = obj["description"]?.jsonPrimitive?.content
        val isUpcoming = obj["upcoming_release"]?.jsonPrimitive?.booleanOrNull ?: false
        val releasedAtStr = obj["released_at"]?.jsonPrimitive?.content

        val assetsObj = obj["assets"]?.jsonObject
        val linksArray = assetsObj?.get("links")?.jsonArray

        val assets = mutableListOf<AssetArtifact>()

        linksArray?.forEach { item ->
            val linkObj = item.jsonObject
            val name = linkObj["name"]?.jsonPrimitive?.content ?: return@forEach
            val url = linkObj["url"]?.jsonPrimitive?.content ?: return@forEach
            val directUrl = linkObj["direct_asset_url"]?.jsonPrimitive?.content ?: url
            assets.add(
                AssetArtifact(
                    name = name,
                    downloadUrl = directUrl,
                    sizeBytes = 0L,
                    contentType = null,
                )
            )
        }

        // Also check description for direct apk download links if no assets links found
        if (assets.isEmpty() && !description.isNullOrBlank()) {
            val apkRegex = Regex("""https?://[^\s)]+\.apk""")
            apkRegex.findAll(description).forEach { match ->
                val link = match.value
                val fileName = link.substringAfterLast('/')
                assets.add(
                    AssetArtifact(
                        name = fileName,
                        downloadUrl = link,
                        sizeBytes = 0L,
                    )
                )
            }
        }

        val cleanVersion = GitHubReleaseProvider.extractCleanVersion(tagName)

        return ReleaseDetails(
            tagName = tagName,
            versionName = cleanVersion,
            title = title,
            releaseNotes = description,
            publishedAt = parseIsoDate(releasedAtStr),
            isPrerelease = isUpcoming,
            assets = assets,
            eTag = eTag,
        )
    }

    companion object {
        fun extractProjectPath(url: String): String? {
            return extractHostAndProjectPath(url)?.second
        }

        fun extractHostAndProjectPath(url: String): Pair<String, String>? {
            val clean = url.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")

            val host = clean.substringBefore('/')
            var path = clean.substringAfter('/', "").removeSuffix(".git")
            // Remove /-/releases or /-/tree, etc.
            if (path.contains("/-/")) {
                path = path.substringBefore("/-/")
            }

            val parts = path.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return Pair(host, parts.joinToString("/"))
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
