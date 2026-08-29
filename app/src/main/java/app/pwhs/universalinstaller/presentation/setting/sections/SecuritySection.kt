package app.pwhs.universalinstaller.presentation.setting.sections

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Fingerprint
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
import app.pwhs.universalinstaller.presentation.setting.components.SwitchPreference
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery

internal fun LazyListScope.SecuritySection(
    q: String,
    securityLabels: List<String>,
    context: Context,
    blacklist: List<String>,
    biometricLockInstall: Boolean,
    biometricLockUninstall: Boolean,
    biometricEnrolmentAvailable: Boolean,
    onBiometricLockInstallChanged: (Boolean) -> Unit,
    onBiometricLockUninstallChanged: (Boolean) -> Unit
) {
    if (matchesQuery(q, securityLabels)) item {
        SettingsSection(title = stringResource(R.string.setting_section_security), icon = Icons.Rounded.Fingerprint) {
            SearchableItem(q, stringResource(R.string.setting_blacklist_title), "blacklist block packages") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.setting_blacklist_title)) },
                    supportingContent = {
                        Text(
                            if (blacklist.isEmpty()) stringResource(R.string.setting_blacklist_empty)
                            else stringResource(R.string.setting_blacklist_count, blacklist.size)
                        )
                    },
                    leadingContent = { Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.blacklist.BlacklistActivity::class.java))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            if (q.isBlank()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            SearchableItem(q, stringResource(R.string.setting_lock_install_title), "biometric security install") {
                SwitchPreference(
                    title = stringResource(R.string.setting_lock_install_title),
                    subtitle = stringResource(R.string.setting_lock_install_subtitle),
                    checked = biometricLockInstall,
                    onCheckedChange = onBiometricLockInstallChanged,
                    enabled = biometricEnrolmentAvailable
                )
            }
            SearchableItem(q, stringResource(R.string.setting_lock_uninstall_title), "biometric security uninstall") {
                SwitchPreference(
                    title = stringResource(R.string.setting_lock_uninstall_title),
                    subtitle = stringResource(R.string.setting_lock_uninstall_subtitle),
                    checked = biometricLockUninstall,
                    onCheckedChange = onBiometricLockUninstallChanged,
                    enabled = biometricEnrolmentAvailable
                )
            }
            if (!biometricEnrolmentAvailable) {
                Text(
                    text = stringResource(R.string.setting_biometric_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
