package app.pwhs.universalinstaller.presentation.setting.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Badge
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
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery

internal fun LazyListScope.ProfilesSection(
    q: String,
    profileLabels: List<String>,
    onProfilesClick: () -> Unit,
) {
    if (matchesQuery(q, profileLabels)) item {
        SettingsSection(
            title = stringResource(R.string.setting_section_profiles),
            icon = Icons.Rounded.Badge
        ) {
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.setting_profiles_title), style = MaterialTheme.typography.bodyLarge)
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.setting_profiles_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(onClick = onProfilesClick),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}
