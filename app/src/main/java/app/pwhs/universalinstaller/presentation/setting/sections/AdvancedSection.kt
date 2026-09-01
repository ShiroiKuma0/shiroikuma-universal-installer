package app.pwhs.universalinstaller.presentation.setting.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.presentation.setting.SecurityLevel
import app.pwhs.universalinstaller.presentation.setting.components.SecurityLevelSelector
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery

internal fun LazyListScope.AdvancedSection(
    q: String,
    advancedLabels: List<String>,
    virusTotalApiKey: String,
    onVirusTotalKeyChanged: (String) -> Unit,
    securityLevel: SecurityLevel,
    onSecurityLevelChanged: (SecurityLevel) -> Unit,
) {
    if (matchesQuery(q, advancedLabels)) item {
        SettingsSection(title = stringResource(R.string.setting_section_advanced), icon = Icons.Rounded.Terminal) {
            OutlinedTextField(
                value = virusTotalApiKey,
                onValueChange = onVirusTotalKeyChanged,
                label = { Text(stringResource(R.string.setting_vt_api_key_title)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Rounded.Key, null, tint = MaterialTheme.colorScheme.primary) },
                placeholder = { Text(stringResource(R.string.setting_vt_api_key_placeholder)) },
                singleLine = true,
            )
            SecurityLevelSelector(
                current = securityLevel,
                onChange = onSecurityLevelChanged,
            )
        }
    }
}
