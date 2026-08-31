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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import timber.log.Timber

/**
 * Update source provider for F-Droid and IzzyOnDroid repositories.
 */
class FDroidReleaseProvider(
    private val client: HttpClient = HttpClient(CIO),
) : UpdateSourceProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase().trim()
        return (lower.contains("f-droid.org") || lower.contains("apt.izzysoft.de")) &&
            extractPackageName(url) != null
    }

    override suspend fun fetchLatestRelease(
        url: String,
        includePrereleases: Boolean,
        eTag: String?,
        apiToken: String?,
    ): Result<ReleaseDetails?> = withContext(Dispatchers.IO) {
        val packageName = extractPackageName(url)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid F-Droid package URL: $url"))

        try {
            val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"
            val response = client.get(apiUrl) {
                header("Accept", "application/json")
                header("User-Agent", "UniversalInstaller-AppUpdater")
                if (!eTag.isNullOrBlank()) {
                    header("If-None-Match", eTag)
                }
            }

            if (response.status == HttpStatusCode.NotModified) {
                return@withContext Result.success(null)
            }

            if (response.status != HttpStatusCode.OK) {
                return@withContext Result.failure(
                    RuntimeException("F-Droid API returned error: ${response.status.value} ${response.status.description}")
                )
            }

            val responseBody = response.bodyAsText()
            val newETag = response.headers["ETag"]

            val rootObj = json.parseToJsonElement(responseBody).jsonObject
            val suggestedVersionName = rootObj["suggestedVersionName"]?.jsonPrimitive?.content ?: ""
            val packagesArray = rootObj["packages"]?.jsonArray

            if (packagesArray.isNullOrEmpty()) {
                return@withContext Result.failure(NoSuchElementException("No release packages found on F-Droid for $packageName"))
            }

            val latestPkgObj = packagesArray.first().jsonObject
            val versionName = latestPkgObj["versionName"]?.jsonPrimitive?.content ?: suggestedVersionName
            val apkName = latestPkgObj["apkName"]?.jsonPrimitive?.content ?: ""
            val sizeBytes = latestPkgObj["size"]?.jsonPrimitive?.longOrNull ?: 0L
            val addedTimestamp = latestPkgObj["added"]?.jsonPrimitive?.longOrNull

            val downloadUrl = if (apkName.isNotBlank()) "https://f-droid.org/repo/$apkName" else ""

            val details = ReleaseDetails(
                tagName = versionName,
                versionName = versionName,
                title = packageName,
                releaseNotes = "Published on F-Droid repository",
                publishedAt = addedTimestamp,
                isPrerelease = false,
                assets = listOf(
                    AssetArtifact(
                        name = apkName.ifBlank { "$packageName-$versionName.apk" },
                        downloadUrl = downloadUrl,
                        sizeBytes = sizeBytes,
                    )
                ),
                eTag = newETag,
            )

            Result.success(details)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch release from F-Droid for $url")
            Result.failure(e)
        }
    }

    companion object {
        fun extractPackageName(url: String): String? {
            val clean = url.trim().removeSuffix("/")
            return when {
                clean.contains("f-droid.org") -> clean.substringAfterLast("/packages/").substringAfterLast("/")
                clean.contains("apt.izzysoft.de") -> clean.substringAfterLast("/apk/").substringAfterLast("/")
                else -> null
            }?.takeIf { it.isNotBlank() && it.contains(".") }
        }
    }
}
