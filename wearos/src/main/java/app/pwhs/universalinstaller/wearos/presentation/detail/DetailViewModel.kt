package app.pwhs.universalinstaller.wearos.presentation.detail

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.universalinstaller.wearos.data.WearApkInfo
import app.pwhs.universalinstaller.wearos.data.WearApkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface InstallState {
    data object Idle : InstallState
    data object Installing : InstallState
    data object Success : InstallState
    data class Failed(val message: String) : InstallState
}

class DetailViewModel(
    private val apkId: String,
    private val repository: WearApkRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val _apkInfo = MutableStateFlow<WearApkInfo?>(null)
    val apkInfo: StateFlow<WearApkInfo?> = _apkInfo.asStateFlow()

    private val _installState = MutableStateFlow<InstallState>(InstallState.Idle)
    val installState: StateFlow<InstallState> = _installState.asStateFlow()

    init {
        _apkInfo.value = repository.getById(apkId)
    }

    fun install() {
        val info = _apkInfo.value ?: return
        val apkFile = File(info.cachedFilePath)
        if (!apkFile.exists()) {
            _installState.value = InstallState.Failed("APK file not found")
            return
        }

        viewModelScope.launch {
            _installState.value = InstallState.Installing
            runCatching {
                withContext(Dispatchers.IO) {
                    commitInstallSession(apkFile)
                }
            }.onSuccess {
                _installState.value = InstallState.Success
                repository.deleteById(apkId)
            }.onFailure { e ->
                _installState.value = InstallState.Failed(e.message ?: "Installation failed")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteById(apkId)
        }
    }

    fun resetInstallState() {
        _installState.value = InstallState.Idle
    }

    /**
     * Writes APK bytes into a PackageInstaller session and commits it.
     * Android handles showing any necessary confirmation UI on the watch.
     * Must be called on a background thread.
     */
    private fun commitInstallSession(apkFile: File) {
        val context = getApplication<Application>()
        val installer = context.packageManager.packageInstaller

        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).also {
            it.setAppPackageName(_apkInfo.value?.packageName)
        }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                apkFile.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }

            // Self-targeting intent so system can report result back
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setPackage(context.packageName)
            }
            val pi = PendingIntent.getActivity(
                context, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            session.commit(pi.intentSender)
        }
    }
}
