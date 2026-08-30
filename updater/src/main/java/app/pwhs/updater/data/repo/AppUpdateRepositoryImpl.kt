package app.pwhs.updater.data.repo

import app.pwhs.updater.data.local.TrackedAppDao
import app.pwhs.updater.data.local.TrackedAppEntity
import app.pwhs.updater.domain.matcher.SmartAbiMatcher
import app.pwhs.updater.domain.model.TrackedApp
import app.pwhs.updater.domain.provider.GitHubReleaseProvider
import app.pwhs.updater.domain.provider.UpdateSourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppUpdateRepositoryImpl(
    private val dao: TrackedAppDao,
    private val providers: List<UpdateSourceProvider> = listOf(GitHubReleaseProvider()),
) : AppUpdateRepository {

    override fun getAllTrackedApps(): Flow<List<TrackedApp>> {
        return dao.getAllTrackedApps().map { list -> list.map { it.toDomain() } }
    }

    override fun getTrackedApp(packageName: String): Flow<TrackedApp?> {
        return dao.getByPackageNameFlow(packageName).map { it?.toDomain() }
    }

    override fun getUpdateCount(): Flow<Int> {
        return dao.getUpdateCountFlow()
    }

    override suspend fun saveTrackedApp(app: TrackedApp) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(TrackedAppEntity.fromDomain(app))
    }

    override suspend fun removeTrackedApp(packageName: String) = withContext(Dispatchers.IO) {
        dao.deleteByPackageName(packageName)
    }

    override suspend fun checkForUpdate(
        packageName: String,
        apiToken: String?,
    ): Result<TrackedApp> = withContext(Dispatchers.IO) {
        val entity = dao.getByPackageName(packageName)
            ?: return@withContext Result.failure(NoSuchElementException("App $packageName not tracked"))

        val currentApp = entity.toDomain()
        val provider = providers.firstOrNull { it.canHandle(currentApp.sourceUrl) }
            ?: return@withContext Result.failure(
                IllegalArgumentException("No provider available for URL: ${currentApp.sourceUrl}")
            )

        val releaseResult = provider.fetchLatestRelease(
            url = currentApp.sourceUrl,
            includePrereleases = currentApp.includePrereleases,
            eTag = currentApp.eTag,
            apiToken = apiToken,
        )

        val now = System.currentTimeMillis()

        releaseResult.fold(
            onSuccess = { releaseDetails ->
                if (releaseDetails == null) {
                    // HTTP 304 Not Modified: Still up to date
                    val updated = currentApp.copy(lastCheckedAt = now)
                    dao.update(TrackedAppEntity.fromDomain(updated))
                    return@withContext Result.success(updated)
                }

                val bestAsset = SmartAbiMatcher.selectBestAsset(
                    assets = releaseDetails.assets,
                    customFilterRegex = currentApp.customRegexFilter,
                )

                val updated = currentApp.copy(
                    latestVersionName = releaseDetails.versionName,
                    latestReleaseTag = releaseDetails.tagName,
                    latestDownloadUrl = bestAsset?.downloadUrl,
                    releaseNotes = releaseDetails.releaseNotes,
                    publishedAt = releaseDetails.publishedAt,
                    lastCheckedAt = now,
                    eTag = releaseDetails.eTag,
                )

                dao.update(TrackedAppEntity.fromDomain(updated))
                Result.success(updated)
            },
            onFailure = { error ->
                Timber.e(error, "Failed update check for $packageName")
                val updated = currentApp.copy(lastCheckedAt = now)
                dao.update(TrackedAppEntity.fromDomain(updated))
                Result.failure(error)
            },
        )
    }

    override suspend fun checkAllUpdates(apiToken: String?): List<TrackedApp> = withContext(Dispatchers.IO) {
        val apps = dao.getAllTrackedApps().firstOrNull() ?: emptyList()
        apps.map { entity ->
            async {
                checkForUpdate(entity.packageName, apiToken).getOrNull() ?: entity.toDomain()
            }
        }.awaitAll()
    }
}
