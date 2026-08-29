package app.pwhs.universalinstaller.presentation.manage

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.presentation.manage.BatchExtractState
import app.pwhs.universalinstaller.presentation.manage.ExtractState
import app.pwhs.universalinstaller.presentation.install.controller.SystemAppMethod

@Composable
internal fun ManageDialogHost(
    confirmUninstallTarget: InstalledApp?,
    onConfirmUninstall: (String) -> Unit,
    onDismissUninstall: () -> Unit,
    confirmClearDataTarget: InstalledApp?,
    onConfirmClearData: (String, String) -> Unit,
    onDismissClearData: () -> Unit,
    extractState: ExtractState,
    batchExtractState: BatchExtractState,
    showBatchConfirm: Boolean,
    onConfirmBatchUninstall: () -> Unit,
    onDismissBatchConfirm: () -> Unit,
    showBatchClearDataConfirm: Boolean,
    onConfirmBatchClearData: () -> Unit,
    onDismissBatchClearDataConfirm: () -> Unit,
    systemAppPrompt: SystemAppPrompt?,
    onConfirmSystemApp: (SystemAppMethod?) -> Unit,
    onDismissSystemApp: () -> Unit,
    selectedPackagesCount: Int,
) {
    val context = LocalContext.current

    confirmUninstallTarget?.let { target ->
        UninstallConfirmDialog(
            app = target,
            onConfirm = { onConfirmUninstall(target.packageName) },
            onDismiss = onDismissUninstall,
        )
    }

    confirmClearDataTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissClearData,
            icon = {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(stringResource(R.string.manage_clear_data_confirm_title, target.appName))
            },
            text = { Text(stringResource(R.string.manage_clear_data_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmClearData(target.packageName, target.appName)
                }) {
                    Text(
                        stringResource(R.string.manage_action_clear_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearData) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (extractState is ExtractState.Running) {
        val runningState = extractState
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss, it's running */ },
            title = { Text(stringResource(R.string.extract_progress_title, runningState.appName)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (runningState.totalBytes > 0) {
                        val progress = runningState.bytesCopied.toFloat() / runningState.totalBytes.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${Formatter.formatShortFileSize(context, runningState.bytesCopied)} / ${Formatter.formatShortFileSize(context, runningState.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {}
        )
    }

    (batchExtractState as? BatchExtractState.Running)?.let { batch ->
        AlertDialog(
            onDismissRequest = { /* running — not dismissable */ },
            title = {
                Text(stringResource(R.string.manage_batch_extract_title, batch.completed + 1, batch.total))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    Text(
                        text = batch.currentName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (batch.totalBytes > 0) {
                        batch.bytesCopied.toFloat() / batch.totalBytes.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
        )
    }

    if (showBatchConfirm) {
        AlertDialog(
            onDismissRequest = onDismissBatchConfirm,
            confirmButton = {
                TextButton(onClick = onConfirmBatchUninstall) {
                    Text(stringResource(R.string.uninstall), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBatchConfirm) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.uninstall_confirm_batch_title, selectedPackagesCount)) },
            text = {
                Text(stringResource(R.string.uninstall_confirm_batch_text, selectedPackagesCount))
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        )
    }

    if (showBatchClearDataConfirm) {
        AlertDialog(
            onDismissRequest = onDismissBatchClearDataConfirm,
            confirmButton = {
                TextButton(onClick = onConfirmBatchClearData) {
                    Text(
                        stringResource(R.string.manage_batch_action_clear_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBatchClearDataConfirm) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.manage_batch_clear_data_confirm_title, selectedPackagesCount)) },
            text = { Text(stringResource(R.string.manage_batch_clear_data_confirm_text)) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
    }

    systemAppPrompt?.let { prompt ->
        SystemAppDialog(
            prompt = prompt,
            onConfirm = onConfirmSystemApp,
            onDismiss = onDismissSystemApp,
        )
    }
}
