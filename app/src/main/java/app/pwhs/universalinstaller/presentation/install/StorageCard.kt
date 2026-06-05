package app.pwhs.universalinstaller.presentation.install

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.core.util.StorageUtil
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.ui.theme.LocalDialogProgressStyle
import app.pwhs.universalinstaller.ui.theme.dialogTextStyle

/**
 * Compact card showing internal-storage usage. Stats come from `/data` (where APKs install),
 * not emulated external — that's what actually matters for install success.
 */
@Composable
internal fun StorageCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Reads once per composition scope — fresh enough for a home-screen glance without
    // polling. A recomposition (e.g. pick a file) naturally refreshes it.
    val stats = remember { StorageUtil.getStorageStats() }
    val progress = stats.progress

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.install_storage_title),
                    style = dialogTextStyle("storage_label", MaterialTheme.typography.titleSmall, MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.install_storage_value,
                        Formatter.formatShortFileSize(context, stats.freeBytes),
                        Formatter.formatShortFileSize(context, stats.totalBytes),
                    ),
                    style = dialogTextStyle("storage_value", MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Spacer(Modifier.height(8.dp))
            // Honour the Main surface's progress override (colour + thickness); else colour by fill level.
            val progressStyle = LocalDialogProgressStyle.current
            val progressColor = progressStyle.color?.let { Color(it) } ?: when {
                progress >= 0.9f -> MaterialTheme.colorScheme.error
                progress >= 0.75f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (progressStyle.thickness != null) it.height(progressStyle.thickness.dp) else it },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
