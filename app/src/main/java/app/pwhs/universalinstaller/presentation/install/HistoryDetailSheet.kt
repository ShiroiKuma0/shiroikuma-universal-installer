package app.pwhs.universalinstaller.presentation.install

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import timber.log.Timber
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.data.local.InstallHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryDetailSheet(
    entry: InstallHistoryEntity,
    onDismiss: () -> Unit,
    onReinstall: ((File) -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    val iconBitmap = remember(entry.iconPath) {
        entry.iconPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val sourceFile = remember(entry.filePath) {
        entry.filePath?.let {
            val f = File(it)
            if (f.exists()) f else null
        }
    }

    val launchIntent = remember(entry.packageName, entry.success) {
        if (entry.success && entry.packageName.isNotBlank()) {
            context.packageManager.getLaunchIntentForPackage(entry.packageName)
        } else {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Icon + Name + Package
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = entry.appName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (entry.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (entry.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (entry.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.appName.ifBlank { entry.fileName },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.packageName.ifBlank { entry.fileName },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Tags & Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Status Chip
                val statusText = if (entry.success) stringResource(R.string.history_detail_status_success) else stringResource(R.string.history_detail_status_failed)
                val statusBg = if (entry.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val statusColor = if (entry.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                DetailChip(text = statusText, backgroundColor = statusBg, contentColor = statusColor)

                // Operation Chip
                val op = entry.operationType
                if (!op.isNullOrBlank()) {
                    val (opText, opColor) = when (op) {
                        "UPDATE" -> {
                            val vText = if (!entry.oldVersionName.isNullOrBlank() && entry.versionName.isNotBlank()) {
                                "${entry.oldVersionName} → ${entry.versionName}"
                            } else {
                                stringResource(R.string.history_detail_op_update)
                            }
                            vText to MaterialTheme.colorScheme.tertiaryContainer
                        }
                        "DOWNGRADE" -> {
                            val vText = if (!entry.oldVersionName.isNullOrBlank() && entry.versionName.isNotBlank()) {
                                "${entry.oldVersionName} → ${entry.versionName}"
                            } else {
                                stringResource(R.string.history_detail_op_downgrade)
                            }
                            vText to MaterialTheme.colorScheme.errorContainer
                        }
                        else -> stringResource(R.string.history_detail_op_new) to MaterialTheme.colorScheme.secondaryContainer
                    }
                    DetailChip(
                        text = opText,
                        backgroundColor = opColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Mode Chip
                if (!entry.installerMode.isNullOrBlank()) {
                    DetailChip(
                        text = entry.installerMode,
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Error Diagnostics Box (if failed)
            if (!entry.success && !entry.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.history_detail_error_title),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Error",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(entry.errorMessage))
                                        Toast.makeText(context, R.string.history_detail_copied, Toast.LENGTH_SHORT).show()
                                    },
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            text = entry.errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Information Grid
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (entry.versionName.isNotBlank()) {
                        InfoRow(label = "Version", value = entry.versionName)
                    }
                    InfoRow(label = "Installed Time", value = dateFormat.format(Date(entry.installedAt)))
                    InfoRow(label = "File Name", value = entry.fileName)
                    if (entry.fileSizeBytes > 0) {
                        InfoRow(label = "File Size", value = Formatter.formatFileSize(context, entry.fileSizeBytes))
                    }
                    if (!entry.filePath.isNullOrBlank()) {
                        InfoRow(label = "File Path", value = entry.filePath)
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (launchIntent != null) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(launchIntent)
                                onDismiss()
                            }.onFailure { e ->
                                Timber.e(e, "Failed to launch app %s", entry.packageName)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_cannot_open_app),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.history_detail_launch))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (entry.packageName.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", entry.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.history_detail_app_info), maxLines = 1)
                        }
                    }

                    if (sourceFile != null && onReinstall != null) {
                        FilledTonalButton(
                            onClick = {
                                onReinstall(sourceFile)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.history_detail_reinstall), maxLines = 1)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val text = buildString {
                            appendLine("App: ${entry.appName}")
                            appendLine("Package: ${entry.packageName}")
                            if (entry.versionName.isNotBlank()) appendLine("Version: ${entry.versionName}")
                            if (!entry.installerMode.isNullOrBlank()) appendLine("Installer Mode: ${entry.installerMode}")
                            appendLine("Status: ${if (entry.success) "Success" else "Failed"}")
                            if (!entry.errorMessage.isNullOrBlank()) appendLine("Error: ${entry.errorMessage}")
                            appendLine("Time: ${dateFormat.format(Date(entry.installedAt))}")
                        }
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, R.string.history_detail_copied, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.history_detail_copy))
                }
            }
        }
    }
}

@Composable
private fun DetailChip(
    text: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = if (label == "File Path") FontFamily.Monospace else FontFamily.Default,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
