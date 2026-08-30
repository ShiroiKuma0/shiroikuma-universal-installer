package app.pwhs.universalinstaller.presentation.install

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.data.local.DownloadHistoryDao
import app.pwhs.universalinstaller.data.local.InstallHistoryDao
import app.pwhs.universalinstaller.data.remote.PackageDownloadService
import app.pwhs.universalinstaller.data.remote.VirusTotalNotifier
import app.pwhs.universalinstaller.data.remote.VirusTotalService
import app.pwhs.universalinstaller.domain.manager.InstallBlacklist
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.InstallerProfile
import app.pwhs.universalinstaller.domain.model.SplitType
import app.pwhs.universalinstaller.domain.model.VtResult
import app.pwhs.universalinstaller.domain.model.VtStatus
import app.pwhs.universalinstaller.domain.repository.SessionDataRepository
import app.pwhs.universalinstaller.presentation.install.controller.BaseInstallController
import app.pwhs.universalinstaller.presentation.install.controller.DefaultInstallController
import app.pwhs.universalinstaller.presentation.install.controller.DhizukuInstallController
import app.pwhs.universalinstaller.presentation.install.controller.InstallerBackendFactory
import app.pwhs.universalinstaller.presentation.install.controller.ManualInstallController
import app.pwhs.universalinstaller.presentation.install.controller.ShizukuInstallController
import app.pwhs.universalinstaller.presentation.install.util.BatchInstallHelper
import app.pwhs.universalinstaller.presentation.install.util.InstallActionDelegate
import app.pwhs.universalinstaller.presentation.install.util.InstallApkSplitsHelper
import app.pwhs.universalinstaller.presentation.install.util.InstallDownloadHelper
import app.pwhs.universalinstaller.presentation.install.util.InstallExecutionCoordinator
import app.pwhs.universalinstaller.presentation.install.util.InstallObbHelper
import app.pwhs.universalinstaller.presentation.install.util.InstallParseCoordinator
import app.pwhs.universalinstaller.presentation.install.util.InstallScanHelper
import app.pwhs.universalinstaller.presentation.install.util.InstallSessionManager
import app.pwhs.universalinstaller.presentation.install.util.InstallUiStateBuilder
import app.pwhs.universalinstaller.presentation.install.util.InstallVirusTotalHelper
import app.pwhs.universalinstaller.presentation.install.util.ObbCopyJob
import app.pwhs.universalinstaller.presentation.install.util.PendingInstallStager
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.telemetry.Telemetry
import app.pwhs.universalinstaller.telemetry.TelemetryEvents
import app.pwhs.universalinstaller.util.DhizukuCompat
import app.pwhs.universalinstaller.util.SignatureCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.splits.SplitPackage
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import java.io.File
import java.util.UUID

class InstallViewModel(
    private val application: Application,
    packageInstaller: PackageInstaller,
    private val sessionDataRepository: SessionDataRepository,
    private val virusTotalService: VirusTotalService,
    private val virusTotalNotifier: VirusTotalNotifier,
    private val packageDownloadService: PackageDownloadService,
    private val historyDao: InstallHistoryDao,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val backendFactory: InstallerBackendFactory,
    private val packageUninstaller: PackageUninstaller,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val defaultController = DefaultInstallController(application, packageInstaller, sessionDataRepository, historyDao)
    private val shizukuController = ShizukuInstallController(application, packageInstaller, sessionDataRepository, historyDao)
    private val manualController = ManualInstallController(application, packageInstaller, sessionDataRepository, historyDao, backendFactory)

    private val dhizukuController: BaseInstallController? by lazy {
        if (!DhizukuCompat.isSupported) null
        else DhizukuInstallController(application, packageInstaller, sessionDataRepository, historyDao)
    }

    private val rootController: BaseInstallController? = backendFactory.createRootController(
        application, packageInstaller, sessionDataRepository, historyDao,
    )

    private val _isLoading = MutableStateFlow(false)
    private val _isApk = MutableStateFlow(false)
    private val _pendingApkInfo = MutableStateFlow<ApkInfo?>(null)
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    private val _obbCopyState = MutableStateFlow<ObbCopyState>(ObbCopyState.Idle)
    private val _attachedObbFiles = MutableStateFlow<List<AttachedObb>>(emptyList())
    private val _batchState = MutableStateFlow<BatchInstallState>(BatchInstallState.Idle)
    private val _batchDetailUri = MutableStateFlow<Uri?>(null)
    private val _dialogStage = MutableStateFlow<DialogStage>(DialogStage.None)
    private val _mergeSplits = MutableStateFlow(false)
    private val _selectedProfileId = MutableStateFlow<String?>(null)
    private val _dialogTarget = MutableStateFlow<DialogTarget?>(null)
    val dialogTarget: StateFlow<DialogTarget?> = _dialogTarget.asStateFlow()

    private var pendingApkUris: List<Uri>? = null
    private var pendingFileName: String? = null
    private var pendingOriginalUri: Uri? = null
    private var pendingObbEntries: List<ObbEntry> = emptyList()
    private var pendingObbCopyJob: ObbCopyJob? = null
    private var scanJob: Job? = null
    private var parseJob: Job? = null
    private var batchParseJob: Job? = null
    private var downloadJob: Job? = null
    private var deviceScanJob: Job? = null
    private var obbWorkerObserverJob: Job? = null
    private val downloadNotifier by lazy { DownloadNotifier(application) }

    private val blacklist: StateFlow<Set<String>> = application.dataStore.data
        .map { InstallBlacklist.read(it) }.catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val history = historyDao.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val uiState = combine(
        listOf(
            sessionDataRepository.sessions, sessionDataRepository.sessionsProgress,
            _isLoading, _pendingApkInfo, _downloadState, _scanState, _obbCopyState, _attachedObbFiles,
            _batchState, _dialogStage, _mergeSplits,
            application.dataStore.data.map { it[PreferencesKeys.INSTALLER_PROFILES] },
            application.dataStore.data.map { it[PreferencesKeys.APP_PROFILE_MAPPING] },
            app.pwhs.universalinstaller.presentation.sync.SyncManager.state,
            _selectedProfileId,
            application.dataStore.data.map { it[PreferencesKeys.SHIZUKU_ALL_USERS] ?: false },
            application.dataStore.data.map { it[PreferencesKeys.INSTALL_USER_ID] },
            _isApk, _batchDetailUri,
        )
    ) { flows -> InstallUiStateBuilder.build(flows) }
        .onStart { activeController().restoreSessionsFromSavedState(viewModelScope) }
        .stateIn(viewModelScope, SharingStarted.Lazily, InstallUiState())

    init {
        viewModelScope.launch {
            try {
                val wm = WorkManager.getInstance(application)
                val active = wm.getWorkInfosByTag(ObbCopyWorker.WORK_TAG).get().firstOrNull { !it.state.isFinished } ?: return@launch
                observeObbWorker(active.id, "", "")
            } catch (_: Throwable) {}
        }
    }

    private suspend fun activeController(profileId: String? = null): BaseInstallController =
        InstallSessionManager.activeController(
            context = application,
            profileId = profileId,
            defaultController = defaultController,
            shizukuController = shizukuController,
            rootController = rootController,
            dhizukuController = dhizukuController,
            backendFactory = backendFactory,
        )

    fun clearHistory() { viewModelScope.launch { historyDao.clearAll() } }
    fun deleteHistoryEntry(id: Long) { viewModelScope.launch { historyDao.deleteById(id) } }
    fun setAllUsers(enabled: Boolean) { viewModelScope.launch { InstallActionDelegate.setAllUsers(application, enabled) } }
    fun setUserId(id: Int?) { viewModelScope.launch { InstallActionDelegate.setUserId(application, id) } }

    fun dialogStartLoading() { _dialogStage.value = DialogStage.Loading }
    fun dialogShowPrepare() { _dialogStage.value = DialogStage.Prepare }
    fun dialogShowMenu() { _dialogStage.value = DialogStage.Menu }
    fun dialogBackToPrepare() { _dialogStage.value = DialogStage.Prepare }
    fun dialogStartInstalling() { _dialogStage.value = DialogStage.Installing }
    fun dialogInstallSuccess() { _dialogStage.value = DialogStage.Success }
    fun dialogInstallFailed(error: String) { _dialogStage.value = DialogStage.Failed(error) }
    fun dialogReadFailed(reason: String) { _dialogStage.value = DialogStage.ReadFailed(reason) }
    fun dialogParseFailed(reason: String) { _dialogStage.value = DialogStage.ParseFailed(reason) }
    fun dialogPermissionRequired() { _dialogStage.value = DialogStage.PermissionRequired }
    fun dialogClose() { _dialogStage.value = DialogStage.None }

    fun setMergeSplits(merge: Boolean) {
        _mergeSplits.value = merge
        val state = _batchState.value
        if (state is BatchInstallState.Ready) parseBatch(application, state.entries.map { it.uri })
    }

    fun applyProfile(profile: InstallerProfile) {
        Telemetry.feature(TelemetryEvents.FEATURE_INSTALLER_PROFILE)
        _selectedProfileId.value = profile.id
    }

    fun selectProfile(profileId: String?) { _selectedProfileId.value = profileId }
    fun clearDialogTarget() { _dialogTarget.value = null; _selectedProfileId.value = null }
    fun getAppLaunchIntent(packageName: String) = application.packageManager.getLaunchIntentForPackage(packageName)

    fun parseApkInfo(context: Context, uri: Uri, splitPackage: SplitPackage.Provider, fileName: String, isAndroidAuto: Boolean? = null) {
        scanJob?.cancel(); pendingObbEntries = emptyList(); parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _isLoading.value = true
            _isApk.value = fileName.substringAfterLast('.', "").lowercase() == "apk"
            pendingFileName = fileName
            pendingOriginalUri = uri
            val result = InstallParseCoordinator.parseApkInfo(
                context, uri, splitPackage, fileName, isAndroidAuto, blacklist.value,
                uiState.value.installerProfiles, uiState.value.appProfileMapping,
            )
            pendingApkUris = result.splitUris
            pendingObbEntries = result.obbEntries
            _pendingApkInfo.value = result.info
            _isLoading.value = false
            if (result.matchingProfileId != null) _selectedProfileId.value = result.matchingProfileId
            launchHashLookupOnly(context, uri)
        }
    }

    fun setAppProfileMapping(packageName: String, profileId: String?) {
        viewModelScope.launch { InstallActionDelegate.setAppProfileMapping(application, packageName, profileId) }
    }

    fun toggleSplit(index: Int) {
        val info = _pendingApkInfo.value ?: return
        val entries = info.splitEntries.toMutableList()
        if (index !in entries.indices) return
        val entry = entries[index]
        if (entry.type == SplitType.Base) return
        entries[index] = entry.copy(selected = !entry.selected)
        _pendingApkInfo.value = info.copy(splitEntries = entries)
        pendingApkUris = entries.filter { it.selected }.map { it.uri }
    }

    fun confirmInstall(trackDialogTarget: Boolean = false) {
        _scanState.value = ScanState.Idle
        val apkInfo = _pendingApkInfo.value
        val uris = if (apkInfo != null && apkInfo.splitEntries.isNotEmpty()) apkInfo.splitEntries.filter { it.selected }.map { it.uri } else pendingApkUris
        if (uris.isNullOrEmpty()) {
            android.widget.Toast.makeText(application, application.getString(R.string.install_no_splits_error), android.widget.Toast.LENGTH_LONG).show(); return
        }
        val blockedPackage = apkInfo?.packageName.orEmpty()
        if (blockedPackage.isNotBlank() && blockedPackage in blacklist.value) {
            android.widget.Toast.makeText(application, application.getString(R.string.install_blocked_by_blacklist, blockedPackage), android.widget.Toast.LENGTH_LONG).show()
            dismissPendingInstall(); return
        }
        val fn = pendingFileName ?: return
        val originalUri = pendingOriginalUri
        val obbEntries = pendingObbEntries
        val attachedObbs = _attachedObbFiles.value
        dismissPendingInstall()

        viewModelScope.launch {
            InstallExecutionCoordinator.executeSingleInstall(
                application = application, scope = viewModelScope, appScope = appScope, trackDialogTarget = trackDialogTarget,
                apkInfo = apkInfo, fileName = fn, originalUri = originalUri, uris = uris, obbEntries = obbEntries, attachedObbs = attachedObbs,
                currentProfileId = _selectedProfileId.value, rootController = rootController, backendFactory = backendFactory,
                manualController = manualController, resolveActiveController = { activeController(it) },
                onDialogTargetCreated = { _dialogTarget.value = it },
                onCopyObbs = { src, obbs, attached, pkg, name -> copyObbFiles(src, obbs, attached, pkg, name) },
            )
        }
    }

    fun dismissPendingInstall() {
        _pendingApkInfo.value = null; pendingApkUris = null; pendingFileName = null
        pendingOriginalUri = null; pendingObbEntries = emptyList(); _attachedObbFiles.value = emptyList()
    }

    suspend fun stashPendingInstall(): PendingInstallStore.Entry? {
        val entry = PendingInstallStager.stashPendingInstall(
            application, _pendingApkInfo.value, pendingFileName, pendingOriginalUri,
            pendingApkUris, pendingObbEntries, _attachedObbFiles.value, InstallSessionManager.cacheIcon(application, _pendingApkInfo.value),
        )
        if (entry != null) dismissPendingInstall()
        return entry
    }

    fun restorePendingInstall(entry: PendingInstallStore.Entry) {
        _pendingApkInfo.value = entry.apkInfo; pendingApkUris = entry.apkUris; pendingFileName = entry.fileName
        pendingOriginalUri = entry.originalUri; pendingObbEntries = entry.obbEntries; _attachedObbFiles.value = entry.attachedObbs
    }

    private suspend fun copyObbFiles(src: Uri?, obbs: List<ObbEntry>, attached: List<AttachedObb>, pkg: String, name: String) {
        InstallObbHelper.copyObbFiles(
            application, src, obbs, attached, pkg, name,
            onStateChanged = { _obbCopyState.value = it },
            onJobCreated = { pendingObbCopyJob = it },
            onObserveWork = { id, app, p -> observeObbWorker(id, app, p) }
        )
    }

    private fun observeObbWorker(workId: UUID, appName: String, packageName: String) {
        obbWorkerObserverJob?.cancel()
        obbWorkerObserverJob = viewModelScope.launch {
            InstallObbHelper.observeObbWorker(application, workId, appName, packageName) {
                _obbCopyState.value = it
                if (it !is ObbCopyState.Running) pendingObbCopyJob = null
            }
        }
    }

    fun onObbTreeGranted(uri: Uri?) {
        val job = pendingObbCopyJob ?: return
        if (uri == null) {
            pendingObbCopyJob = null; _obbCopyState.value = ObbCopyState.Error(job.appName, "OBB folder access not granted"); return
        }
        if (!SafObbWriter.isTreeForObbOf(uri, job.packageName)) {
            pendingObbCopyJob = null; _obbCopyState.value = ObbCopyState.Error(job.appName, "Wrong folder picked — expected Android/obb/${job.packageName}/"); return
        }
        try { application.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) {}
        viewModelScope.launch {
            InstallObbHelper.saveObbTreeGrant(application, job.packageName, uri)
            copyObbFiles(job.sourceUri, job.entries, job.attached, job.packageName, job.appName)
        }
    }

    fun obbTreeHintUri(): Uri? = pendingObbCopyJob?.let { SafObbWriter.buildObbTreeHintUri(it.packageName) }
    fun dismissObbCopy() { _obbCopyState.value = ObbCopyState.Idle }
    fun attachObbFile(context: Context, uri: Uri) { _attachedObbFiles.value = InstallObbHelper.attachObbFile(context, uri, _attachedObbFiles.value) }
    fun removeAttachedObb(uri: Uri) { _attachedObbFiles.value = _attachedObbFiles.value.filterNot { it.uri == uri } }

    fun parseBatch(context: Context, uris: List<Uri>) {
        if (uris.size <= 1) return
        _batchState.value = BatchInstallState.Parsing(uris = uris, processed = 0, total = uris.size)
        batchParseJob?.cancel()
        batchParseJob = viewModelScope.launch {
            val entries = BatchInstallHelper.parseBatchUrisWithAckpine(context, uris, _mergeSplits.value) { processed, total ->
                _batchState.value = BatchInstallState.Parsing(uris, processed, total)
            }
            _batchState.value = BatchInstallState.Ready(entries)
        }
    }

    fun toggleBatchSelection(uri: Uri) { _batchState.value = BatchInstallHelper.toggleBatchSelection(_batchState.value, uri) }
    fun setBatchAllSelected(selected: Boolean) { _batchState.value = BatchInstallHelper.setBatchAllSelected(_batchState.value, selected) }
    fun dismissBatchInstall() { _batchState.value = BatchInstallState.Idle }
    fun openBatchDetail(uri: Uri) { _batchDetailUri.value = uri }
    fun closeBatchDetail() { _batchDetailUri.value = null }
    fun saveBatchDetail(uri: Uri, newSplitUris: List<Uri>) {
        _batchState.value = BatchInstallHelper.saveBatchDetail(_batchState.value, uri, newSplitUris)
        _batchDetailUri.value = null
    }

    fun confirmBatchInstall() {
        _scanState.value = ScanState.Idle
        val ready = _batchState.value as? BatchInstallState.Ready ?: return
        val picked = ready.entries.filter { it.selected && it.splitUris.isNotEmpty() }
        _batchState.value = BatchInstallState.Idle
        if (picked.isEmpty()) return
        viewModelScope.launch {
            InstallExecutionCoordinator.executeBatchInstall(application, viewModelScope, picked, _selectedProfileId.value) { activeController(it) }
        }
    }

    fun skipParseAndInstallSingle() {
        _scanState.value = ScanState.Idle; parseJob?.cancel()
        val uri = pendingOriginalUri ?: return
        val fileName = pendingFileName ?: uri.lastPathSegment ?: "Unknown"
        _isLoading.value = false
        _dialogTarget.value = DialogTarget(UUID.randomUUID(), "", fileName, null)
        dialogStartInstalling()
        parseJob = viewModelScope.launch {
            InstallExecutionCoordinator.executeSkipSingle(application, viewModelScope, uri, fileName, _dialogTarget.value!!.sessionId, { activeController() }) {
                dialogInstallSuccess(); _dialogTarget.value = null
            }
        }
    }

    fun skipBatchParseAndInstall() {
        _scanState.value = ScanState.Idle
        val parsing = _batchState.value as? BatchInstallState.Parsing ?: return
        val uris = parsing.uris
        batchParseJob?.cancel(); batchParseJob = null; _batchState.value = BatchInstallState.Idle
        viewModelScope.launch {
            InstallExecutionCoordinator.executeSkipBatch(application, viewModelScope, uris) { activeController() }
        }
    }

    fun downloadFromUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            _downloadState.value = DownloadState.Error(context.getString(R.string.remote_download_invalid_url))
            return
        }
        downloadJob?.cancel()
        Telemetry.feature(TelemetryEvents.FEATURE_URL_DOWNLOAD)
        downloadJob = viewModelScope.launch {
            InstallDownloadHelper.executeDownload(
                context, trimmed, packageDownloadService, downloadNotifier, downloadHistoryDao,
                onProgress = { _downloadState.value = it },
                onSuccess = { file, name, ext -> handleDownloadedFile(context, file, name, ext) }
            )
        }
    }

    fun cancelDownload() { downloadJob?.cancel(); downloadJob = null; _downloadState.value = DownloadState.Idle; downloadNotifier.cancel() }
    fun dismissDownloadError() {
        if (_downloadState.value is DownloadState.Error) {
            _downloadState.value = DownloadState.Idle; downloadNotifier.cancel()
        }
    }

    fun startDeviceScan(context: Context) {
        deviceScanJob?.cancel()
        _scanState.value = ScanState.Scanning
        deviceScanJob = viewModelScope.launch { _scanState.value = InstallScanHelper.performDeviceScan(application) }
    }

    fun dismissDeviceScan() { deviceScanJob?.cancel(); deviceScanJob = null; _scanState.value = ScanState.Idle }

    fun deleteFoundFiles(context: Context, files: List<FoundPackageFile>) {
        if (files.isEmpty()) return
        viewModelScope.launch { InstallScanHelper.deleteFoundFiles(files); startDeviceScan(context) }
    }

    fun pickFromScan(context: Context, found: FoundPackageFile) {
        val file = File(found.path)
        if (!file.exists()) return
        val uri = InstallScanHelper.resolveUriForFile(context, file)
        val splitProvider = InstallApkSplitsHelper.buildSplitProvider(context, uri, found.extension)
        parseApkInfo(context, uri, splitProvider, found.name, found.isAndroidAutoSupported)
    }

    fun pickManyFromScan(context: Context, found: List<FoundPackageFile>) {
        val uris = InstallScanHelper.collectUrisFromScan(context, found)
        if (uris.size >= 2) parseBatch(context, uris)
        else if (uris.size == 1) found.firstOrNull { File(it.path).exists() }?.let { pickFromScan(context, it) }
    }

    private fun handleDownloadedFile(context: Context, file: File, displayName: String, extension: String) {
        val uri = InstallScanHelper.resolveUriForFile(context, file)
        val splitProvider = InstallApkSplitsHelper.buildSplitProvider(context, uri, extension)
        parseApkInfo(context, uri, splitProvider, displayName)
    }

    fun scanVirusTotal(context: Context) {
        val uri = pendingOriginalUri ?: return
        val fileName = pendingFileName ?: "APK"
        val current = _pendingApkInfo.value ?: return
        scanJob?.cancel()
        Telemetry.feature(TelemetryEvents.FEATURE_VIRUSTOTAL)
        scanJob = viewModelScope.launch {
            InstallVirusTotalHelper.scanVirusTotal(
                context, uri, fileName, current, virusTotalService, virusTotalNotifier,
                onUpdateApkInfo = { _pendingApkInfo.value = it },
                onProgress = { _pendingApkInfo.value = _pendingApkInfo.value?.copy(vtResult = it) }
            )
        }
    }

    private fun launchHashLookupOnly(context: Context, originalUri: Uri) {
        viewModelScope.launch {
            _pendingApkInfo.value = _pendingApkInfo.value?.copy(vtResult = VtResult(status = VtStatus.SCANNING))
            val result = InstallVirusTotalHelper.launchHashLookupOnly(context, originalUri, virusTotalService)
            _pendingApkInfo.value = _pendingApkInfo.value?.copy(sha256 = result.first, vtResult = result.second)
        }
    }

    fun cancelSession(id: UUID) { viewModelScope.launch { activeController().cancel(id, viewModelScope) } }
    fun dismissSession(id: UUID) { viewModelScope.launch { activeController().dismiss(id) } }
    fun retrySession(id: UUID) { viewModelScope.launch { activeController().retry(id, viewModelScope, application) } }

    fun retryDialogInstall() {
        val target = _dialogTarget.value ?: return
        _dialogStage.value = DialogStage.Installing
        viewModelScope.launch {
            activeController().retry(target.sessionId, appScope, application) { newId ->
                _dialogTarget.value = target.copy(sessionId = newId)
            }
        }
    }

    fun unblockPackage(packageName: String) {
        if (packageName.isBlank()) return
        viewModelScope.launch {
            InstallActionDelegate.unblockPackage(application, packageName)
            _pendingApkInfo.value = _pendingApkInfo.value?.copy(isBlocked = false)
        }
    }

    suspend fun uninstallConflictingApp(packageName: String): Boolean {
        val removed = InstallSessionManager.uninstallConflictingApp(
            application, packageName, _selectedProfileId.value, defaultController,
            shizukuController, rootController, dhizukuController, backendFactory, packageUninstaller,
        )
        if (removed) onConflictingAppUninstalled()
        return removed
    }

    fun onConflictingAppUninstalled() {
        val info = _pendingApkInfo.value ?: return
        if (SignatureCheck.isInstalled(application, info.packageName)) return
        _pendingApkInfo.value = info.copy(installedVersionName = null, installedVersionCode = null, signatureMismatch = false)
    }
}
