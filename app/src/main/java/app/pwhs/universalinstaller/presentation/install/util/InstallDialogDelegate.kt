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

    fun setTarget(target: DialogTarget?) {
        _dialogTarget.value = target
    }

    fun clearTarget() {
        _dialogTarget.value = null
    }

    fun startLoading() {
        _dialogStage.value = DialogStage.Loading
    }

    fun showPrepare() {
        _dialogStage.value = DialogStage.Prepare
    }

    fun showMenu() {
        _dialogStage.value = DialogStage.Menu
    }

    fun backToPrepare() {
        _dialogStage.value = DialogStage.Prepare
    }

    fun startInstalling() {
        _dialogStage.value = DialogStage.Installing
    }

    fun installSuccess() {
        _dialogStage.value = DialogStage.Success
    }

    fun installFailed(error: String) {
        _dialogStage.value = DialogStage.Failed(error)
    }

    fun readFailed(reason: String) {
        _dialogStage.value = DialogStage.ReadFailed(reason)
    }

    fun parseFailed(reason: String) {
        _dialogStage.value = DialogStage.ParseFailed(reason)
    }

    fun permissionRequired() {
        _dialogStage.value = DialogStage.PermissionRequired
    }

    fun close() {
        _dialogStage.value = DialogStage.None
    }
}
