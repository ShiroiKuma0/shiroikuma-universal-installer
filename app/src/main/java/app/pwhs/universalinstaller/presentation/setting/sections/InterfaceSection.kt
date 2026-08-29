package app.pwhs.universalinstaller.presentation.setting.sections

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.presentation.setting.components.SearchableItem
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery

internal fun LazyListScope.InterfaceSection(
    q: String,
    interfaceLabels: List<String>,
    context: Context,
    onLanguageClick: () -> Unit
) {
    if (matchesQuery(q, interfaceLabels)) item {
        SettingsSection(title = stringResource(R.string.setting_section_interface), icon = Icons.Rounded.Palette) {
            SearchableItem(q, stringResource(R.string.theme_screen_title), "interface theme") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme_screen_title)) },
                    leadingContent = { Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.theme.ThemeActivity::class.java))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            if (q.isBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
            SearchableItem(q, stringResource(R.string.setting_language_title), stringResource(R.string.setting_language_subtitle)) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.setting_language_title), style = MaterialTheme.typography.bodyLarge)
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.setting_language_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    modifier = Modifier.clickable(onClick = onLanguageClick),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
