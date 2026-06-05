package app.pwhs.universalinstaller.presentation.install.dialog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.install.PermissionEntry
import app.pwhs.universalinstaller.ui.theme.dialogTextStyle

@Composable
internal fun PermissionRowList(
    entries: List<PermissionEntry>,
    modifier: Modifier = Modifier,
) {
    var showAll by remember(entries) { mutableStateOf(false) }
    val collapsedCount = 5
    val needsToggle = entries.size > collapsedCount
    val visible = if (showAll || !needsToggle) entries else entries.take(collapsedCount)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        visible.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (entry.isDangerous) {
                            Modifier.background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            )
                        } else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (entry.isDangerous) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (entry.isDangerous) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.label,
                        style = dialogTextStyle(
                            "permission",
                            MaterialTheme.typography.bodySmall.copy(fontWeight = if (entry.isDangerous) FontWeight.SemiBold else FontWeight.Normal),
                            if (entry.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.prefix.isNotEmpty()) {
                        Text(
                            text = entry.prefix,
                            style = dialogTextStyle("permission", MaterialTheme.typography.labelSmall, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (needsToggle) {
            androidx.compose.material3.TextButton(
                onClick = { showAll = !showAll },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (showAll) {
                        stringResource(R.string.dialog_menu_show_less)
                    } else {
                        stringResource(R.string.dialog_menu_show_more, entries.size - collapsedCount)
                    },
                    style = dialogTextStyle("permission", MaterialTheme.typography.labelMedium),
                )
            }
        }
    }
}
