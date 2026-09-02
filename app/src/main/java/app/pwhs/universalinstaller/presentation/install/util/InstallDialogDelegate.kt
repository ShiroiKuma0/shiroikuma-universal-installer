package app.pwhs.universalinstaller.presentation.install.util

import app.pwhs.universalinstaller.presentation.install.DialogStage
import app.pwhs.universalinstaller.presentation.install.DialogTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InstallDialogDelegate {

    private val _dialogStage = MutableStateFlow<DialogStage>(DialogStage.None)
    val dialogStage: StateFlow<DialogStage> = _dialogStage.asStateFlow()

    private val _dialogTarget = MutableStateFlow<DialogTarget?>(null)
    val dialogTarget: StateFlow<DialogTarget?> = _dialogTarget.asStateFlow()

    private val _downloadProgress = MutableStateFlow<app.pwhs.core.network.DownloadProgress?>(null)
    val downloadProgress: StateFlow<app.pwhs.core.network.DownloadProgress?> = _downloadProgress.asStateFlow()

    fun updateDownloadProgress(progress: app.pwhs.core.network.DownloadProgress?) {
        _downloadProgress.value = progress
    }

    fun setTarget(target: DialogTarget?) {
        _dialogTarget.value = target
    }

    fun clearTarget() {
        _dialogTarget.value = null
        _downloadProgress.value = null
    }

    fun startLoading() {
        _dialogStage.value = DialogStage.Loading
    }

    fun showPrepare() {
        _dialogStage.value = DialogStage.Prepare
        _downloadProgress.value = null
    }

    fun showMenu() {
        _dialogStage.value = DialogStage.Menu
    }

    fun backToPrepare() {
        _dialogStage.value = DialogStage.Prepare
    }

    fun startInstalling() {
        _dialogStage.value = DialogStage.Installing
        _downloadProgress.value = null
    }

    fun installSuccess() {
        _dialogStage.value = DialogStage.Success
    }

    fun installFailed(error: String) {
        _dialogStage.value = DialogStage.Failed(error)
    }

    fun readFailed(reason: String) {
        _dialogStage.value = DialogStage.ReadFailed(reason)
        _downloadProgress.value = null
    }

    fun parseFailed(reason: String) {
        _dialogStage.value = DialogStage.ParseFailed(reason)
        _downloadProgress.value = null
    }

    fun permissionRequired() {
        _dialogStage.value = DialogStage.PermissionRequired
    }

    fun close() {
        _dialogStage.value = DialogStage.None
        _downloadProgress.value = null
    }

    fun cancelDownload(context: android.content.Context) {
        BackgroundPackageDownloader.cancel(context)
        _downloadProgress.value = null
        close()
    }

    fun startNetworkDownload(
        context: android.content.Context,
        uri: android.net.Uri,
        onFileDownloaded: (java.io.File, String) -> Unit,
    ) {
        startLoading()
        BackgroundPackageDownloader.download(
            context = context,
            uri = uri,
            onProgress = { progress -> updateDownloadProgress(progress) },
            onSuccess = { file, fileName ->
                updateDownloadProgress(null)
                onFileDownloaded(file, fileName)
            },
            onError = { error -> readFailed(error) },
        )
    }
}
