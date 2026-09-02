package app.pwhs.tv.presentation.receive

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.core.data.ApkMetadataReader
import app.pwhs.core.data.DownloadsApkScanner
import app.pwhs.core.data.local.SharedPrefsKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.domain.ApkFile
import app.pwhs.core.install.ApkInstaller
import app.pwhs.core.install.RootInstaller
import app.pwhs.core.receiver.ReceivedApk
import app.pwhs.core.receiver.ReceivingProgress
import app.pwhs.core.receiver.ReceiverStatus
import app.pwhs.core.receiver.TvReceiverState
import app.pwhs.core.util.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val metadataReader = ApkMetadataReader(context)
    private val installer = ApkInstaller(context)
    private val rootInstaller = RootInstaller(context)

    val status: StateFlow<ReceiverStatus> = TvReceiverState.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReceiverStatus.Stopped)

    val receivingProgress: StateFlow<ReceivingProgress?> = TvReceiverState.receivingProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _pendingApk = MutableStateFlow<ReceivedApk?>(null)
    val pendingApk: StateFlow<ReceivedApk?> = _pendingApk.asStateFlow()

    private val _downloads = MutableStateFlow<List<ApkFile>>(emptyList())
    val downloads: StateFlow<List<ApkFile>> = _downloads.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _installingLabel = MutableStateFlow<String?>(null)
    val installingLabel: StateFlow<String?> = _installingLabel.asStateFlow()

    /** 0f..1f while the session is being written, null when the total size is unknown/idle. */
    private val _installProgress = MutableStateFlow<Float?>(null)
    val installProgress: StateFlow<Float?> = _installProgress.asStateFlow()

    private val _installResult = MutableStateFlow<InstallOutcome?>(null)
    val installResult: StateFlow<InstallOutcome?> = _installResult.asStateFlow()

    /** Kept so the result overlay can offer a one-tap Retry without re-picking the APK. */
    private var lastInstall: InstallRequest? = null

    sealed interface InstallOutcome {
        data class Success(val label: String, val silent: Boolean = false) : InstallOutcome
        data class Failure(val label: String, val message: String) : InstallOutcome
    }

    private data class InstallRequest(val uri: Uri, val isBundle: Boolean, val label: String, val sizeBytes: Long)

    init {
        viewModelScope.launch {
            TvReceiverState.received.collectLatest { received ->
                _installResult.value = null
                // Extract metadata immediately for received APK
                val metadata = metadataReader.readMetadata(Uri.fromFile(File(received.path)), received.fileName.isBundleName())
                _pendingApk.value = received.copy(metadata = metadata)
            }
        }
    }

    /** True once a scan has been kicked off, so tab revisits don't redundantly re-scan. */
    private var hasScannedOnce = false

    /** Scan only if we never have — used on screen entry; the Rescan button calls [scanLocalApks]. */
    fun scanLocalApksIfNeeded() {
        if (hasScannedOnce) return
        scanLocalApks()
    }

    fun scanLocalApks() {
        if (_isScanning.value) return
        hasScannedOnce = true
        viewModelScope.launch {
            _isScanning.value = true
            val files = withContext(Dispatchers.IO) { DownloadsApkScanner.scan(context) }
            _downloads.value = files
            
            // Optionally load metadata for local files lazily or all at once if small
            // For now, let's load them all to show icons in the list
            val enriched = files.map { file ->
                val meta = metadataReader.readMetadata(Uri.parse(file.uri), file.isBundle)
                file.copy(metadata = meta)
            }
            _downloads.value = enriched
            _isScanning.value = false
        }
    }

    fun install(uri: Uri, isBundle: Boolean, label: String, sizeBytes: Long) {
        if (_installingLabel.value != null) return
        lastInstall = InstallRequest(uri, isBundle, label, sizeBytes)
        viewModelScope.launch {
            _installResult.value = null
            _installingLabel.value = label
            _installProgress.value = if (sizeBytes > 0) 0f else null
            // Prefer the silent root path on rooted boxes (skips the D-pad-hostile system dialog);
            // fall back to the PackageInstaller session when root is off or unavailable.
            val silentPref = runCatching {
                context.dataStore.data.first()[SharedPrefsKeys.ROOT_SILENT_INSTALL]
            }.getOrNull() ?: true
            val useRoot = silentPref && RootShell.isAvailable()
            val result = withContext(Dispatchers.IO) {
                if (useRoot) {
                    rootInstaller.install(uri, isBundle) { f -> _installProgress.value = f }
                } else {
                    installer.install(uri, isBundle, totalBytes = sizeBytes) { written, total ->
                        if (total > 0) _installProgress.value = (written.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }
            _installingLabel.value = null
            _installProgress.value = null
            _installResult.value = when (result) {
                is ApkInstaller.Result.Success -> {
                    _pendingApk.value = null // received APK is installed — clear the hero so the QR returns
                    InstallOutcome.Success(label, silent = useRoot)
                }
                is ApkInstaller.Result.Failure -> InstallOutcome.Failure(label, result.message)
            }
        }
    }

    fun retryInstall() {
        lastInstall?.let { install(it.uri, it.isBundle, it.label, it.sizeBytes) }
    }

    fun clearInstallResult() {
        _installResult.value = null
    }

    fun dismissPending() {
        _pendingApk.value = null
        _installResult.value = null
    }

    private fun String.isBundleName(): Boolean =
        substringAfterLast('.', "").lowercase() in setOf("apks", "xapk", "apkm", "apk+")
}
