package app.pwhs.updater.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.core.data.ApkMetadataReader
import app.pwhs.updater.data.remote.AppDownloader
import app.pwhs.updater.data.repo.AppUpdateRepository
import app.pwhs.updater.domain.matcher.SmartAbiMatcher
import app.pwhs.updater.domain.model.TrackedApp
import app.pwhs.updater.domain.model.UpdateSourceType
import app.pwhs.updater.domain.provider.GitHubReleaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class UpdatesViewModel(
    private val repository: AppUpdateRepository,
    private val downloader: AppDownloader,
    private val gitHubProvider: GitHubReleaseProvider = GitHubReleaseProvider(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdatesUiState())
    val uiState: StateFlow<UpdatesUiState> = _uiState.asStateFlow()

    init {
        loadTrackedApps()
    }

    private fun loadTrackedApps() {
        viewModelScope.launch {
            repository.getAllTrackedApps().collect { apps ->
                _uiState.update { it.copy(trackedApps = apps) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show, error = null) }
    }

    fun checkAllUpdates(apiToken: String? = null) {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, error = null) }
            try {
                repository.checkAllUpdates(apiToken)
            } catch (e: Exception) {
                Timber.e(e, "Check all updates failed")
                _uiState.update { it.copy(error = e.message ?: "Failed to check updates") }
            } finally {
                _uiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun checkSingleUpdate(packageName: String, apiToken: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            try {
                repository.checkForUpdate(packageName, apiToken)
            } catch (e: Exception) {
                Timber.e(e, "Check update failed for $packageName")
            } finally {
                _uiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun addTrackedAppFromUrl(
        context: Context,
        url: String,
        includePrereleases: Boolean = false,
        apiToken: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, error = null) }
            try {
                val sourceType = UpdateSourceType.fromUrl(url)
                if (sourceType == UpdateSourceType.UNKNOWN) {
                    _uiState.update { it.copy(isAdding = false, error = "Unsupported URL source") }
                    return@launch
                }

                val releaseResult = gitHubProvider.fetchLatestRelease(
                    url = url,
                    includePrereleases = includePrereleases,
                    apiToken = apiToken,
                )

                val release = releaseResult.getOrNull()
                if (release == null) {
                    _uiState.update { it.copy(isAdding = false, error = "Could not fetch release information") }
                    return@launch
                }

                val bestAsset = SmartAbiMatcher.selectBestAsset(release.assets)
                val repoPath = GitHubReleaseProvider.extractRepoPath(url) ?: url
                val defaultAppName = repoPath.substringAfterLast('/')

                // Try to check if already installed on device
                val pm = context.packageManager
                val (installedPkg, installedVerName, installedVerCode) = findInstalledAppMatch(pm, defaultAppName)

                val trackedApp = TrackedApp(
                    packageName = installedPkg ?: "tracked.${defaultAppName.lowercase().replace('-', '_')}",
                    appName = defaultAppName,
                    sourceType = sourceType,
                    sourceUrl = url,
                    currentVersionName = installedVerName ?: "Not Installed",
                    currentVersionCode = installedVerCode ?: 0L,
                    latestVersionName = release.versionName,
                    latestReleaseTag = release.tagName,
                    latestDownloadUrl = bestAsset?.downloadUrl,
                    releaseNotes = release.releaseNotes,
                    publishedAt = release.publishedAt,
                    lastCheckedAt = System.currentTimeMillis(),
                    includePrereleases = includePrereleases,
                    eTag = release.eTag,
                )

                repository.saveTrackedApp(trackedApp)
                _uiState.update { it.copy(isAdding = false, showAddDialog = false) }
            } catch (e: Exception) {
                Timber.e(e, "Add tracked app failed for $url")
                _uiState.update { it.copy(isAdding = false, error = e.message ?: "Failed to add app") }
            }
        }
    }

    private fun findInstalledAppMatch(pm: PackageManager, appName: String): Triple<String?, String?, Long?> {
        return runCatching {
            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getInstalledPackages(0)
            }
            val match = installedApps.firstOrNull { pkg ->
                val label = runCatching {
                    pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
                }.getOrNull()
                label?.equals(appName, ignoreCase = true) == true || pkg.packageName.contains(appName, ignoreCase = true)
            }
            if (match != null) {
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    match.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    match.versionCode.toLong()
                }
                Triple(match.packageName, match.versionName, vCode)
            } else {
                Triple(null, null, null)
            }
        }.getOrDefault(Triple(null, null, null))
    }

    fun removeTrackedApp(packageName: String) {
        viewModelScope.launch {
            repository.removeTrackedApp(packageName)
        }
    }

    fun downloadAndInstall(
        context: Context,
        app: TrackedApp,
        apiToken: String? = null,
    ) {
        val downloadUrl = app.latestDownloadUrl ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingPackage = app.packageName, downloadProgress = 0f) }
            try {
                val downloadResult = downloader.downloadApk(
                    downloadUrl = downloadUrl,
                    packageName = app.packageName,
                    versionName = app.latestVersionName ?: "latest",
                    apiToken = apiToken,
                    onProgress = { written, total ->
                        if (total > 0) {
                            val fraction = (written.toFloat() / total).coerceIn(0f, 1f)
                            _uiState.update { it.copy(downloadProgress = fraction) }
                        }
                    },
                )

                val apkFile = downloadResult.getOrNull()
                if (apkFile != null && apkFile.exists()) {
                    // Launch installer
                    launchInstallerForFile(context, apkFile)
                } else {
                    _uiState.update { it.copy(error = "Download failed") }
                }
            } catch (e: Exception) {
                Timber.e(e, "Download and install failed for ${app.packageName}")
                _uiState.update { it.copy(error = e.message ?: "Download failed") }
            } finally {
                _uiState.update { it.copy(downloadingPackage = null, downloadProgress = 0f) }
            }
        }
    }

    private fun launchInstallerForFile(context: Context, file: File) {
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.getOrElse { Uri.fromFile(file) }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
