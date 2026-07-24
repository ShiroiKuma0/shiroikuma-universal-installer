package app.pwhs.universalinstaller.presentation.setting.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.ui.theme.InstallerBadgeDefaults

/** Outcome shown in the yellow-bordered info dialog on top of the Export/Import panel. */
private sealed interface EimResult {
    data class ExportOk(val name: String) : EimResult
    data class ImportOk(val summary: String) : EimResult
    data class Failed(val message: String) : EimResult
}

/**
 * The Export/Import panel (Kōjiki-style): a tappable export-directory box with a "last export"
 * status line, category checkboxes covering everything settable in the app, and an
 * Arcanechat-style pill button row (Cancel left; Import + Export right).
 *
 * Close chain: a successful export (OK) or import ("Later"/dismiss) closes the info dialog, this
 * panel AND the UI settings page via [onCloseAll]; "Restart now" restarts the whole app. Failures
 * only dismiss the info dialog — the panel stays open.
 */
@Composable
fun ExportImportDialog(
    viewModel: InstallerUiViewModel,
    onDismiss: () -> Unit,
    onCloseAll: () -> Unit,
) {
    val context = LocalContext.current
    val dirUri by viewModel.exportDir.collectAsState()
    val lastExport by viewModel.lastExport.collectAsState()
    val checked = remember {
        mutableStateMapOf<ConfigCategory, Boolean>().apply {
            ConfigCategory.entries.forEach { this[it] = true }
        }
    }
    var result by remember { mutableStateOf<EimResult?>(null) }
    val noneSelectedMsg = stringResource(R.string.eim_none_selected)

    fun selected(): Set<ConfigCategory> =
        ConfigCategory.entries.filter { checked[it] == true }.toSet()
    fun Result<String>.toExportResult(): EimResult = fold(
        { EimResult.ExportOk(it) },
        { EimResult.Failed(context.getString(R.string.eim_export_failed, it.message ?: "")) },
    )

    val dirPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.setExportDir(uri)
    }
    // Save-As fallback when no export directory is set.
    val exportFallback = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportConfigTo(uri, selected()) { r -> result = r.toExportResult() }
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importConfig(uri, selected()) { r ->
            result = r.fold(
                { EimResult.ImportOk(it) },
                { EimResult.Failed(context.getString(R.string.eim_import_failed, it.message ?: "")) },
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Black,
            border = BorderStroke(2.dp, InstallerBadgeDefaults.Border),
        ) {
            // 白い熊: unspecified texts in the panel render in the accent (kxkb yellow).
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
              CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                Text(
                    text = stringResource(R.string.eim_section),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))

                // Export-directory box: caption + current folder (tap to pick, reopens at the
                // current location), with the "last export" status line beneath.
                val dirShape = RoundedCornerShape(10.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(dirShape)
                        .border(1.5.dp, InstallerBadgeDefaults.Border, dirShape)
                        .clickable { dirPicker.launch(dirUri.takeIf { it.isNotEmpty() }?.let(Uri::parse)) }
                        .padding(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.eim_dir_caption),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                    val dirName = remember(dirUri) {
                        dirUri.takeIf { it.isNotEmpty() }?.let { u ->
                            runCatching { DocumentFile.fromTreeUri(context, Uri.parse(u))?.name }.getOrNull()
                                ?: Uri.parse(u).lastPathSegment
                        }
                    }
                    Text(
                        text = dirName ?: stringResource(R.string.eim_dir_unset),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dirName == null) KxkbWarnRed
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                val (statusText, warn) = when (val le = lastExport) {
                    LastExport.NoDir -> stringResource(R.string.eim_warn_nodir) to true
                    LastExport.None -> stringResource(R.string.eim_warn_none) to true
                    is LastExport.Found -> stringResource(R.string.eim_last, le.formatted) to false
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (warn) KxkbWarnRed
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(10.dp))

                val allChecked = ConfigCategory.entries.all { checked[it] == true }
                CheckRow(
                    label = stringResource(R.string.eim_select_all),
                    checkedState = allChecked,
                    bold = true,
                ) { v -> ConfigCategory.entries.forEach { checked[it] = v } }
                ConfigCategory.entries.forEach { cat ->
                    CheckRow(
                        label = stringResource(cat.labelRes),
                        checkedState = checked[cat] == true,
                        indent = 16,
                    ) { checked[cat] = it }
                }
                Spacer(Modifier.height(16.dp))

                // Arcanechat-style action line: Cancel alone on the left, actions on the right.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillButton(stringResource(R.string.cancel), onClick = onDismiss)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(stringResource(R.string.eim_import)) {
                            if (selected().isEmpty()) {
                                result = EimResult.Failed(noneSelectedMsg)
                            } else {
                                importPicker.launch(
                                    arrayOf("application/json", "application/octet-stream", "*/*")
                                )
                            }
                        }
                        PillButton(stringResource(R.string.eim_export)) {
                            when {
                                selected().isEmpty() -> result = EimResult.Failed(noneSelectedMsg)
                                dirUri.isNotEmpty() ->
                                    viewModel.exportConfigToDir(selected()) { r -> result = r.toExportResult() }
                                else -> exportFallback.launch(viewModel.exportFileName())
                            }
                        }
                    }
                }
              }
            }
        }
    }

    result?.let { r ->
        EimResultDialog(
            result = r,
            onDismissFailure = { result = null },
            onCloseAll = onCloseAll,
        )
    }
}

/** The finished-info dialog: yellow border; success acknowledgement closes the whole chain. */
@Composable
private fun EimResultDialog(
    result: EimResult,
    onDismissFailure: () -> Unit,
    onCloseAll: () -> Unit,
) {
    val context = LocalContext.current
    val borderModifier = Modifier.border(
        BorderStroke(2.dp, InstallerBadgeDefaults.Border),
        AlertDialogDefaults.shape,
    )
    // 白い熊 black/yellow: black container, accent title + body, pill buttons.
    @Composable
    fun eimAlert(
        title: String,
        body: String,
        onDismissRequest: () -> Unit,
        confirmLabel: String,
        onConfirm: () -> Unit,
        dismissLabel: String? = null,
        onDismissButton: (() -> Unit)? = null,
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = borderModifier,
            containerColor = Color.Black,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = { PillButton(confirmLabel, onClick = onConfirm) },
            dismissButton = if (dismissLabel != null && onDismissButton != null) {
                { PillButton(dismissLabel, onClick = onDismissButton) }
            } else null,
        )
    }
    when (result) {
        is EimResult.ExportOk -> eimAlert(
            title = stringResource(R.string.eim_export),
            body = stringResource(R.string.eim_export_ok, result.name),
            onDismissRequest = onCloseAll,
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = onCloseAll,
        )
        is EimResult.ImportOk -> eimAlert(
            title = stringResource(R.string.eim_import_ok_title),
            body = stringResource(R.string.eim_import_ok_body, result.summary),
            onDismissRequest = onCloseAll,
            confirmLabel = stringResource(R.string.eim_restart_now),
            onConfirm = { restartApp(context) },
            dismissLabel = stringResource(R.string.eim_later),
            onDismissButton = onCloseAll,
        )
        is EimResult.Failed -> eimAlert(
            title = stringResource(R.string.eim_section),
            body = result.message,
            onDismissRequest = onDismissFailure,
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = onDismissFailure,
        )
    }
}

@Composable
private fun CheckRow(
    label: String,
    checkedState: Boolean,
    bold: Boolean = false,
    indent: Int = 0,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checkedState) }
            .padding(start = indent.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checkedState, onCheckedChange = onChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else null,
        )
    }
}

/** Full app restart (Kōjiki-style): relaunch the task at the launcher activity, then exit. */
private fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(Intent.makeRestartActivityTask(launch.component))
    Runtime.getRuntime().exit(0)
}
