package app.pwhs.universalinstaller.presentation.install.dialog

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.ui.theme.surfaceBorder

data class StorageWarningInfo(
    val freeBytes: Long,
    val requiredBytes: Long,
) {
    companion object {
        fun create(requiredBytes: Long = 0L): StorageWarningInfo {
            val stats = app.pwhs.core.util.StorageUtil.getStorageStats()
            val needed = if (requiredBytes > 0L) {
                (requiredBytes * 2).coerceAtLeast(app.pwhs.core.util.StorageUtil.MIN_STORAGE_HEADROOM_BYTES)
            } else {
                app.pwhs.core.util.StorageUtil.MIN_STORAGE_HEADROOM_BYTES
            }
            return StorageWarningInfo(stats.freeBytes, needed)
        }
    }
}

@Composable
fun InsufficientStorageDialog(
    warningInfo: StorageWarningInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val formattedRequired = Formatter.formatShortFileSize(context, warningInfo.requiredBytes)
    val formattedFree = Formatter.formatShortFileSize(context, warningInfo.freeBytes)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.surfaceBorder(),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.insufficient_storage_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.insufficient_storage_dialog_message,
                        formattedRequired,
                        formattedFree,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openStorageSettings(context)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.insufficient_storage_dialog_clean_up))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

internal fun openStorageSettings(context: Context) {
    val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val success = runCatching { context.startActivity(intent); true }.getOrDefault(false)
    if (!success) {
        val fallback = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}
