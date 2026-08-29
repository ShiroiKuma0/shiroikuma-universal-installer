package app.pwhs.universalinstaller.presentation.install.dialog.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.domain.model.SplitEntry
import app.pwhs.universalinstaller.domain.model.SplitType
import app.pwhs.universalinstaller.presentation.install.displayLanguage

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SplitChipPicker(
    entries: List<SplitEntry>,
    selectedBytes: Long,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                val isBase = entry.type == SplitType.Base
                FilterChip(
                    selected = entry.selected,
                    onClick = { if (!isBase) onToggle(index) },
                    enabled = !isBase,
                    label = {
                        Text(
                            text = splitChipLabel(entry),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    leadingIcon = if (isBase) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        // Base sticks to selected styling even though it's disabled — visually
                        // it's "locked on", not greyed out into ambiguity.
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (selectedBytes > 0) {
                "Selected: ${Formatter.formatFileSize(context, selectedBytes)}"
            } else {
                "Selected: —"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Friendly chip label per split type. Strips the "config." prefix that Android
 * Bundles add to ABI/locale/density splits, and runs locales through
 * displayLanguage so "config.en" renders as "English".
 */
@Composable
internal fun splitChipLabel(entry: SplitEntry): String {
    val raw = entry.name.removePrefix("config.")
    return when (entry.type) {
        SplitType.Base -> "Base"
        SplitType.Locale -> displayLanguage(raw)
        SplitType.Libs -> raw.replace('_', '-')
        SplitType.ScreenDensity -> raw
        SplitType.Feature -> raw
        SplitType.Other -> entry.name
    }
}
