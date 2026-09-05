package app.pwhs.universalinstaller.presentation.setting.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSheetsHost(
    viewModel: BackupViewModel,
    onPickerReady: (() -> Unit) -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                }.onSuccess { content ->
                    if (content != null) {
                        withContext(Dispatchers.Main) {
                            viewModel.onFileSelectedForRestore(content)
                        }
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = uiState.pendingExportPayload
        if (uri != null && payload != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(payload.toByteArray(Charsets.UTF_8))
                        output.flush()
                    }
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        viewModel.onExportCompleted()
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        viewModel.onExportFailed(e.message ?: "Failed to write backup")
                    }
                }
            }
        } else {
            viewModel.onExportCancelled()
        }
    }

    LaunchedEffect(uiState.pendingExportPayload) {
        val payload = uiState.pendingExportPayload
        if (payload != null) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "universal_installer_backup_$timestamp.json"
            createDocumentLauncher.launch(filename)
        }
    }

    LaunchedEffect(Unit) {
        onPickerReady {
            openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    if (uiState.showExportSheet) {
        ExportBackupBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.showExportSheet(false) },
            onToggleSettings = { viewModel.setIncludeSettings(it) },
            onToggleTokens = { viewModel.setIncludeTokens(it) },
            onToggleTrackedApps = { viewModel.setIncludeTrackedApps(it) },
            onToggleUninstallLogs = { viewModel.setIncludeUninstallLogs(it) },
            onToggleEncrypted = { viewModel.setEncrypted(it) },
            onExportPasswordChanged = { viewModel.setExportPassword(it) },
            onConfirmExportPasswordChanged = { viewModel.setConfirmExportPassword(it) },
            onStartExport = { viewModel.startExport() },
        )
    }

    if (uiState.showRestoreSheet) {
        RestoreBackupBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.showRestoreSheet(false) },
            onDecryptPasswordChanged = { viewModel.setDecryptPassword(it) },
            onSubmitPassword = { viewModel.decryptPendingEnvelope() },
            onToggleRestoreSettings = { viewModel.setRestoreSettings(it) },
            onToggleRestoreTokens = { viewModel.setRestoreTokens(it) },
            onToggleRestoreTrackedApps = { viewModel.setRestoreTrackedApps(it) },
            onToggleRestoreUninstallLogs = { viewModel.setRestoreUninstallLogs(it) },
            onConfirmRestore = { viewModel.confirmRestore() },
        )
    }
}
