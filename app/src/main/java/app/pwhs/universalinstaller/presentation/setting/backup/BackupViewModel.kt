package app.pwhs.universalinstaller.presentation.setting.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.universalinstaller.domain.backup.BackupExportOptions
import app.pwhs.universalinstaller.domain.backup.BackupInspectResult
import app.pwhs.universalinstaller.domain.backup.BackupRestoreManager
import app.pwhs.universalinstaller.domain.backup.BackupRestoreOptions
import app.pwhs.universalinstaller.domain.backup.BackupSummaryInfo
import app.pwhs.universalinstaller.domain.backup.EncryptedBackupEnvelope
import app.pwhs.universalinstaller.domain.backup.TrackedAppsBackupDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    // Export state
    val includeSettings: Boolean = true,
    val includeTokens: Boolean = true,
    val includeTrackedApps: Boolean = true,
    val includeUninstallLogs: Boolean = true,
    val isEncrypted: Boolean = false,
    val exportPassword: String = "",
    val confirmExportPassword: String = "",
    val isExporting: Boolean = false,
    val exportPasswordError: String? = null,
    val pendingExportPayload: String? = null,

    // Restore state
    val isRestoring: Boolean = false,
    val pendingEnvelope: EncryptedBackupEnvelope? = null,
    val decryptPassword: String = "",
    val decryptError: String? = null,
    val summaryInfo: BackupSummaryInfo? = null,
    val restoreSettings: Boolean = true,
    val restoreTokens: Boolean = true,
    val restoreTrackedApps: Boolean = true,
    val restoreUninstallLogs: Boolean = true,

    // Sheet visibility
    val showExportSheet: Boolean = false,
    val showRestoreSheet: Boolean = false,

    // Feedback
    val successMessage: String? = null,
    val errorMessage: String? = null,

    // Capabilities
    val isTrackedAppsAvailable: Boolean = false,
)

class BackupViewModel(
    private val backupRestoreManager: BackupRestoreManager,
    private val trackedAppsDataSource: TrackedAppsBackupDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BackupUiState(
            isTrackedAppsAvailable = trackedAppsDataSource.isAvailable(),
            includeTrackedApps = trackedAppsDataSource.isAvailable(),
        )
    )
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun showExportSheet(show: Boolean) {
        _uiState.update {
            it.copy(
                showExportSheet = show,
                exportPasswordError = null,
                errorMessage = null,
                pendingExportPayload = null,
            )
        }
    }

    fun showRestoreSheet(show: Boolean) {
        _uiState.update {
            it.copy(
                showRestoreSheet = show,
                decryptPassword = "",
                decryptError = null,
                pendingEnvelope = null,
                summaryInfo = null,
                errorMessage = null,
            )
        }
    }

    fun setIncludeSettings(value: Boolean) = _uiState.update { it.copy(includeSettings = value) }
    fun setIncludeTokens(value: Boolean) = _uiState.update { it.copy(includeTokens = value) }
    fun setIncludeTrackedApps(value: Boolean) = _uiState.update { it.copy(includeTrackedApps = value) }
    fun setIncludeUninstallLogs(value: Boolean) = _uiState.update { it.copy(includeUninstallLogs = value) }
    fun setEncrypted(value: Boolean) = _uiState.update { it.copy(isEncrypted = value, exportPasswordError = null) }
    fun setExportPassword(value: String) = _uiState.update { it.copy(exportPassword = value, exportPasswordError = null) }
    fun setConfirmExportPassword(value: String) = _uiState.update { it.copy(confirmExportPassword = value, exportPasswordError = null) }

    fun setRestoreSettings(value: Boolean) = _uiState.update { it.copy(restoreSettings = value) }
    fun setRestoreTokens(value: Boolean) = _uiState.update { it.copy(restoreTokens = value) }
    fun setRestoreTrackedApps(value: Boolean) = _uiState.update { it.copy(restoreTrackedApps = value) }
    fun setRestoreUninstallLogs(value: Boolean) = _uiState.update { it.copy(restoreUninstallLogs = value) }
    fun setDecryptPassword(value: String) = _uiState.update { it.copy(decryptPassword = value, decryptError = null) }

    fun startExport() {
        val state = _uiState.value
        if (state.isEncrypted) {
            if (state.exportPassword.isBlank()) {
                _uiState.update { it.copy(exportPasswordError = "Password cannot be empty") }
                return
            }
            if (state.exportPassword != state.confirmExportPassword) {
                _uiState.update { it.copy(exportPasswordError = "Passwords do not match") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportPasswordError = null) }
            try {
                val options = BackupExportOptions(
                    includeSettings = state.includeSettings,
                    includeTokens = state.includeTokens,
                    includeTrackedApps = state.includeTrackedApps,
                    includeUninstallLogs = state.includeUninstallLogs,
                )
                val password = if (state.isEncrypted) state.exportPassword else null
                val content = backupRestoreManager.createBackup(options, password)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        pendingExportPayload = content,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun onExportCompleted() {
        _uiState.update {
            it.copy(
                showExportSheet = false,
                pendingExportPayload = null,
                successMessage = "Backup exported successfully!",
            )
        }
    }

    fun onExportCancelled() {
        _uiState.update { it.copy(pendingExportPayload = null) }
    }

    fun onExportFailed(error: String) {
        _uiState.update {
            it.copy(
                pendingExportPayload = null,
                errorMessage = "Failed to save backup: $error",
            )
        }
    }

    fun onFileSelectedForRestore(content: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showRestoreSheet = true,
                    isRestoring = false,
                    decryptError = null,
                    decryptPassword = "",
                    pendingEnvelope = null,
                    summaryInfo = null,
                )
            }
            when (val result = backupRestoreManager.inspectBackupFile(content)) {
                is BackupInspectResult.Encrypted -> {
                    _uiState.update { it.copy(pendingEnvelope = result.envelope) }
                }
                is BackupInspectResult.Ready -> {
                    _uiState.update {
                        it.copy(
                            summaryInfo = result.summary,
                            restoreSettings = result.summary.hasSettings,
                            restoreTokens = result.summary.hasSourceTokens,
                            restoreTrackedApps = result.summary.trackedAppsCount > 0,
                            restoreUninstallLogs = result.summary.uninstallLogsCount > 0,
                        )
                    }
                }
                is BackupInspectResult.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message,
                            showRestoreSheet = false,
                        )
                    }
                }
            }
        }
    }

    fun decryptPendingEnvelope() {
        val envelope = _uiState.value.pendingEnvelope ?: return
        val password = _uiState.value.decryptPassword

        if (password.isBlank()) {
            _uiState.update { it.copy(decryptError = "Please enter password") }
            return
        }

        viewModelScope.launch {
            when (val result = backupRestoreManager.decryptAndInspect(envelope, password)) {
                is BackupInspectResult.Ready -> {
                    _uiState.update {
                        it.copy(
                            pendingEnvelope = null,
                            summaryInfo = result.summary,
                            decryptError = null,
                            restoreSettings = result.summary.hasSettings,
                            restoreTokens = result.summary.hasSourceTokens,
                            restoreTrackedApps = result.summary.trackedAppsCount > 0,
                            restoreUninstallLogs = result.summary.uninstallLogsCount > 0,
                        )
                    }
                }
                is BackupInspectResult.Error -> {
                    _uiState.update { it.copy(decryptError = result.message) }
                }
                is BackupInspectResult.Encrypted -> {
                    _uiState.update { it.copy(decryptError = "Unexpected nested encryption") }
                }
            }
        }
    }

    fun confirmRestore() {
        val summary = _uiState.value.summaryInfo ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            try {
                val options = BackupRestoreOptions(
                    restoreSettings = state.restoreSettings,
                    restoreTokens = state.restoreTokens,
                    restoreTrackedApps = state.restoreTrackedApps,
                    restoreUninstallLogs = state.restoreUninstallLogs,
                )
                val result = backupRestoreManager.restoreBackup(summary.backup, options)
                val parts = mutableListOf<String>()
                if (result.settingsRestored) parts.add("Settings")
                if (result.tokensRestored) parts.add("Tokens")
                if (result.trackedAppsCount > 0) parts.add("${result.trackedAppsCount} apps")
                if (result.uninstallLogsCount > 0) parts.add("${result.uninstallLogsCount} logs")

                val message = if (parts.isEmpty()) {
                    "Nothing was selected to restore"
                } else {
                    "Restored: ${parts.joinToString(", ")}"
                }

                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        showRestoreSheet = false,
                        summaryInfo = null,
                        pendingEnvelope = null,
                        successMessage = message,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = "Restore failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
