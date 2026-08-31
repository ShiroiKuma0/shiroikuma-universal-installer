package app.pwhs.updater.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.updater.data.remote.AppDownloader
import app.pwhs.updater.data.repo.AppUpdateRepository
import app.pwhs.updater.domain.matcher.SmartAbiMatcher
import app.pwhs.updater.domain.model.TrackedApp
import app.pwhs.updater.domain.model.UpdateSourceType
import app.pwhs.updater.domain.provider.GitHubReleaseProvider
import app.pwhs.updater.domain.provider.UpdateSourceProvider
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
    private val providers: List<UpdateSourceProvider> = listOf(GitHubReleaseProvider()),
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

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateAppCategory(packageName: String, category: String?) {
        viewModelScope.launch {
            val app = _uiState.value.trackedApps.firstOrNull { it.packageName == packageName } ?: return@launch
            val updated = app.copy(category = category?.takeIf { it.isNotBlank() })
            repository.saveTrackedApp(updated)
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show, error = null) }
    }

    fun showAppPickerDialog(show: Boolean) {
        _uiState.update { it.copy(showAppPickerDialog = show) }
    }

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingInstalledApps = true) }
            val pm = context.packageManager
            val trackedPkgSet = _uiState.value.trackedApps.map { it.packageName }.toSet()

            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getInstalledPackages(0)
            }

            val appList = installedPackages.mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem && pkg.packageName != "app.pwhs.universalinstaller") return@mapNotNull null

                val appName = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg.packageName)
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                InstalledAppItem(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = vCode,
                    isTracked = trackedPkgSet.contains(pkg.packageName),
                )
            }.sortedBy { it.appName.lowercase() }

            _uiState.update { it.copy(installedApps = appList, isLoadingInstalledApps = false) }
        }
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
        targetPackageName: String? = null,
        category: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, error = null) }
            try {
                val provider = providers.firstOrNull { it.canHandle(url) }
                if (provider == null) {
                    _uiState.update { it.copy(isAdding = false, error = "Unsupported URL source") }
                    return@launch
                }

                val releaseResult = provider.fetchLatestRelease(
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
                val rawRepoName = url.substringBefore('?').removeSuffix(".git").substringAfterLast('/')
                val cleanAppName = rawRepoName.split('-', '_', '.').joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

                val pm = context.packageManager
                val matchResult = if (!targetPackageName.isNullOrBlank()) {
                    val pkgInfo = runCatching { pm.getPackageInfo(targetPackageName, 0) }.getOrNull()
                    if (pkgInfo != null) {
                        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pkgInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pkgInfo.versionCode.toLong()
                        }
                        val appLabel = runCatching {
                            pkgInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
                        }.getOrNull() ?: cleanAppName
                        app.pwhs.updater.domain.matcher.InstalledAppMatchResult(
                            packageName = targetPackageName,
                            appName = appLabel,
                            versionName = pkgInfo.versionName ?: "1.0",
                            versionCode = vCode,
                        )
                    } else null
                } else {
                    app.pwhs.updater.domain.matcher.InstalledAppMatcher.findMatch(
                        pm = pm,
                        repoUrl = url,
                        candidateName = cleanAppName,
                        assetNames = release.assets.map { it.name },
                    )
                }

                val finalPkg = matchResult?.packageName ?: targetPackageName
                    ?: "tracked.${rawRepoName.lowercase().replace(Regex("[^a-z0-9_]"), "_")}"
                val finalAppName = matchResult?.appName ?: cleanAppName

                val trackedApp = TrackedApp(
                    packageName = finalPkg,
                    appName = finalAppName,
                    sourceType = UpdateSourceType.fromUrl(url),
                    sourceUrl = url,
                    currentVersionName = matchResult?.versionName ?: "Not Installed",
                    currentVersionCode = matchResult?.versionCode ?: 0L,
                    latestVersionName = release.versionName,
                    latestReleaseTag = release.tagName,
                    latestDownloadUrl = bestAsset?.downloadUrl,
                    releaseNotes = release.releaseNotes,
                    publishedAt = release.publishedAt,
                    lastCheckedAt = System.currentTimeMillis(),
                    includePrereleases = includePrereleases,
                    category = category?.trim()?.takeIf { it.isNotBlank() },
                    eTag = release.eTag,
                )

                repository.saveTrackedApp(trackedApp)
                _uiState.update { it.copy(isAdding = false, showAddDialog = false, showAppPickerDialog = false) }
            } catch (e: Exception) {
                Timber.e(e, "Add tracked app failed for $url")
                _uiState.update { it.copy(isAdding = false, error = e.message ?: "Failed to add app") }
            }
        }
    }

    fun exportTrackedAppsJson(): String {
        return app.pwhs.updater.domain.backup.TrackedAppsBackupHelper.exportToJson(_uiState.value.trackedApps)
    }

    fun importTrackedAppsFromJson(jsonContent: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true) }
            try {
                val imported = app.pwhs.updater.domain.backup.TrackedAppsBackupHelper.importFromJson(jsonContent)
                for (app in imported) {
                    repository.saveTrackedApp(app)
                }
                onComplete(imported.size)
            } catch (e: Exception) {
                Timber.e(e, "Import tracked apps failed")
                _uiState.update { it.copy(error = "Failed to import apps: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isAdding = false) }
            }
        }
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
        onComplete: (() -> Unit)? = null,
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
                    launchInstallerForFile(context, apkFile)
                    onComplete?.invoke()
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

    fun updateAll(context: Context, apiToken: String? = null) {
        val appsToUpdate = _uiState.value.trackedApps.filter { it.hasUpdate && !it.latestDownloadUrl.isNullOrBlank() }
        if (appsToUpdate.isEmpty() || _uiState.value.isUpdatingAll) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAll = true) }
            for (app in appsToUpdate) {
                val downloadUrl = app.latestDownloadUrl ?: continue
                _uiState.update { it.copy(downloadingPackage = app.packageName, downloadProgress = 0f) }

                try {
                    val result = downloader.downloadApk(
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

                    val apkFile = result.getOrNull()
                    if (apkFile != null && apkFile.exists()) {
                        withContext(Dispatchers.Main) {
                            launchInstallerForFile(context, apkFile)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Batch update failed for ${app.packageName}")
                }
            }
            _uiState.update { it.copy(isUpdatingAll = false, downloadingPackage = null, downloadProgress = 0f) }
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
            setClassName(context.packageName, "app.pwhs.universalinstaller.presentation.install.DialogInstallActivity")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            context.startActivity(fallbackIntent)
        }
    }
}
