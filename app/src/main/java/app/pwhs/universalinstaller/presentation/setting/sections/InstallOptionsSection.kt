package app.pwhs.universalinstaller.presentation.setting.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.presentation.setting.SettingUiState
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import app.pwhs.universalinstaller.presentation.setting.components.InstallSourceItem
import app.pwhs.universalinstaller.presentation.setting.components.OptionGroupHeader
import app.pwhs.universalinstaller.presentation.setting.components.OptionSwitch
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery
import androidx.datastore.preferences.core.Preferences
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.asCommon

internal fun LazyListScope.InstallOptionsSection(
    q: String,
    privilegedLabels: List<String>,
    uiState: SettingUiState,
    useDhizuku: Boolean,
    onPrivilegedOptionChanged: (SettingViewModel.PrivilegedOption, Boolean) -> Unit,
    onInstallerPackageChanged: (String) -> Unit,
    onShizukuOptionChanged: (Preferences.Key<Boolean>, Boolean) -> Unit
) {
    val privileged = uiState.useShizuku ||
        (uiState.rootSupported && uiState.useRoot) ||
        useDhizuku
    if (matchesQuery(q, privilegedLabels)) {
        item {
            SettingsSection(
                title = stringResource(R.string.setting_section_install_options),
                icon = Icons.Rounded.AdminPanelSettings,
            ) {
                // Values are kept in sync across backends by setPrivilegedOption, so
                // reading either store gives the same answer. Root's is used when Root
                // is active purely so a pre-existing divergence shows the live one.
                val opts = if (uiState.useRoot && uiState.rootSupported) {
                    uiState.rootOptions.asCommon()
                } else {
                    uiState.shizukuOptions.asCommon()
                }
                if (useDhizuku) {
                    Text(
                        text = stringResource(R.string.setting_install_options_dhizuku_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (privileged) {
                    OptionGroupHeader(stringResource(R.string.setting_shizuku_options_install_group))
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_replace),
                        subtitle = stringResource(R.string.setting_shizuku_replace_sub),
                        checked = opts.replaceExisting,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.ReplaceExisting, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_downgrade),
                        subtitle = stringResource(R.string.setting_shizuku_downgrade_sub),
                        checked = opts.requestDowngrade,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.RequestDowngrade, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_grant_permissions),
                        subtitle = stringResource(R.string.setting_shizuku_grant_permissions_sub),
                        checked = opts.grantAllPermissions,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.GrantAllPermissions, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_allow_test),
                        subtitle = stringResource(R.string.setting_shizuku_allow_test_sub),
                        checked = opts.allowTest,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.AllowTest, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_bypass_sdk),
                        subtitle = stringResource(R.string.setting_shizuku_bypass_sdk_sub),
                        checked = opts.bypassLowTargetSdk,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.BypassLowTargetSdk, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_all_users),
                        subtitle = stringResource(R.string.setting_shizuku_all_users_sub),
                        checked = opts.allUsers,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.AllUsers, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_allow_restricted_permissions),
                        subtitle = stringResource(R.string.setting_shizuku_allow_restricted_permissions_sub),
                        checked = opts.allowRestrictedPermissions,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.AllowRestrictedPermissions, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_dont_kill_app),
                        subtitle = stringResource(R.string.setting_shizuku_dont_kill_app_sub),
                        checked = opts.dontKillApp,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.DontKillApp, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_disable_verification),
                        subtitle = stringResource(R.string.setting_shizuku_disable_verification_sub),
                        checked = opts.disableVerification,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.DisableVerification, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_enable_rollback),
                        subtitle = stringResource(R.string.setting_shizuku_enable_rollback_sub),
                        checked = opts.enableRollback,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.EnableRollback, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_request_update_ownership),
                        subtitle = stringResource(R.string.setting_shizuku_request_update_ownership_sub),
                        checked = opts.requestUpdateOwnership,
                        onCheckedChange = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.RequestUpdateOwnership, it) },
                    )
                }
                InstallSourceItem(
                    title = stringResource(R.string.setting_shizuku_set_source),
                    subtitle = stringResource(R.string.setting_shizuku_set_source_sub),
                    enabled = opts.setInstallSource,
                    installerPackageName = opts.installerPackageName,
                    onToggle = { onPrivilegedOptionChanged(SettingViewModel.PrivilegedOption.SetInstallSource, it) },
                    onInstallerChange = onInstallerPackageChanged,
                )

                // Uninstall flags are genuinely Shizuku-only — ManageViewModel reads
                // the Shizuku keys and gates them on USE_SHIZUKU.
                if (uiState.useShizuku) {
                    OptionGroupHeader(stringResource(R.string.setting_shizuku_options_uninstall_group))
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_uninstall_keep_data),
                        subtitle = stringResource(R.string.setting_shizuku_uninstall_keep_data_sub),
                        checked = uiState.shizukuOptions.uninstallKeepData,
                        onCheckedChange = { onShizukuOptionChanged(PreferencesKeys.SHIZUKU_UNINSTALL_KEEP_DATA, it) },
                    )
                    OptionSwitch(
                        title = stringResource(R.string.setting_shizuku_uninstall_all_users),
                        subtitle = stringResource(R.string.setting_shizuku_uninstall_all_users_sub),
                        checked = uiState.shizukuOptions.uninstallAllUsers,
                        onCheckedChange = { onShizukuOptionChanged(PreferencesKeys.SHIZUKU_UNINSTALL_ALL_USERS, it) },
                    )
                }
            }
        }
    }
}
