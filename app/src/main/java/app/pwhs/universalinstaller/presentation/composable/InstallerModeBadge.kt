package app.pwhs.universalinstaller.presentation.composable

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.install.controller.RootState
import app.pwhs.universalinstaller.presentation.setting.InstallMode
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import app.pwhs.universalinstaller.presentation.setting.ShizukuState
import app.pwhs.universalinstaller.util.DhizukuCompat
import app.pwhs.universalinstaller.util.DhizukuState
import app.pwhs.core.data.local.dataStore
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

/**
 * Pill showing the active install backend. Tapping it opens a picker so the user can switch
 * engine (PackageInstaller / Shizuku / Dhizuku / Root) right from the Install and Manage screens —
 * the switch reuses [SettingViewModel.setInstallMode], which runs the same Shizuku-permission
 * / root-request ladder as the Settings screen.
 */
@Composable
fun InstallerModeBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingViewModel: SettingViewModel = koinViewModel()
    val settingState by settingViewModel.uiState.collectAsState()

    val modeFlow = remember(context) {
        context.dataStore.data.map { prefs ->
            when {
                prefs[PreferencesKeys.USE_ROOT] == true -> Mode.Root
                prefs[PreferencesKeys.USE_SHIZUKU] == true -> Mode.Shizuku
                prefs[PreferencesKeys.USE_DHIZUKU] == true -> Mode.Dhizuku
                else -> Mode.Default
            }
        }
    }
    val mode by modeFlow.collectAsState(initial = Mode.Default)
    var showPicker by remember { mutableStateOf(false) }

    // Surface the hint events the switcher emits (e.g. "install Shizuku", "permission denied")
    // so the user isn't left wondering why the engine didn't change.
    LaunchedEffect(Unit) {
        settingViewModel.events.collect { stringRes ->
            Toast.makeText(context, context.getString(stringRes), Toast.LENGTH_LONG).show()
        }
    }

    val label = when (mode) {
        Mode.Root -> stringResource(R.string.installer_mode_root)
        Mode.Shizuku -> stringResource(R.string.installer_mode_shizuku)
        Mode.Dhizuku -> stringResource(R.string.installer_mode_dhizuku)
        Mode.Default -> stringResource(R.string.installer_mode_package_installer)
    }
    val icon = when (mode) {
        Mode.Root -> Icons.Rounded.Key
        Mode.Shizuku -> Icons.Rounded.AdminPanelSettings
        Mode.Dhizuku -> Icons.Rounded.Shield
        Mode.Default -> Icons.Rounded.Android
    }
    val privileged = mode == Mode.Root || mode == Mode.Shizuku || mode == Mode.Dhizuku
    val container = if (privileged)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (privileged)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { showPicker = true }
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.installer_mode_using, label),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(start = 6.dp),
        )
    }

    if (showPicker) {
        val dhizukuState by settingViewModel.dhizukuState.collectAsState()
        val current = InstallMode.from(
            useShizuku = mode == Mode.Shizuku,
            useRoot = mode == Mode.Root,
            useDhizuku = mode == Mode.Dhizuku,
        )
        // Root stays tappable whenever this build ships su support — tapping it when not yet
        // ready fires the root request (su prompt). It's only greyed (dimmed) to signal it
        // isn't the active/ready engine. On builds with no root support it's fully disabled.
        // Shizuku is disabled only when it genuinely can't run here (not installed /
        // unsupported); NOT_RUNNING / NO_PERMISSION stay tappable since picking them kicks
        // off the start/permission flow, same as Settings.
        // Only DIM Root when we positively know it's unusable (NOT_ROOTED / UNAVAILABLE).
        // UNKNOWN must NOT dim: a fresh SettingViewModel probes su as UNKNOWN (libsu confirms
        // READY only after a shell attempt), so a granted device shows UNKNOWN here and was
        // being greyed even though root works.
        val rootReady = settingState.rootState == RootState.READY || current == InstallMode.ROOT
        val rootDimmed = current != InstallMode.ROOT && (
            settingState.rootState == RootState.NOT_ROOTED ||
            settingState.rootState == RootState.UNAVAILABLE
        )
        val shizukuSelectable = settingState.shizukuState != ShizukuState.UNSUPPORTED &&
            settingState.shizukuState != ShizukuState.NOT_INSTALLED

        val dhizukuSupported = DhizukuCompat.isSupported
        val dhizukuReady = dhizukuState == DhizukuState.READY || current == InstallMode.DHIZUKU
        val dhizukuSelectable = dhizukuSupported &&
            dhizukuState != DhizukuState.UNSUPPORTED &&
            dhizukuState != DhizukuState.NOT_INSTALLED &&
            dhizukuState != DhizukuState.PROFILE_OWNER_UNSUPPORTED
        val dhizukuDimmed = current != InstallMode.DHIZUKU && !dhizukuReady

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.installer_engine_dialog_title)) },
            text = {
                Column {
                    EngineOption(
                        title = stringResource(R.string.installer_mode_package_installer),
                        subtitle = stringResource(R.string.installer_engine_default_desc),
                        selected = current == InstallMode.DEFAULT,
                        enabled = true,
                        onClick = {
                            settingViewModel.setInstallMode(InstallMode.DEFAULT)
                            showPicker = false
                        },
                    )
                    EngineOption(
                        title = stringResource(R.string.installer_mode_shizuku),
                        subtitle = when (settingState.shizukuState) {
                            ShizukuState.NOT_INSTALLED -> stringResource(R.string.setting_shizuku_not_installed)
                            ShizukuState.UNSUPPORTED -> stringResource(R.string.setting_shizuku_unsupported)
                            else -> stringResource(R.string.installer_engine_shizuku_desc)
                        },
                        selected = current == InstallMode.SHIZUKU,
                        enabled = shizukuSelectable,
                        onClick = {
                            settingViewModel.setInstallMode(InstallMode.SHIZUKU)
                            showPicker = false
                        },
                    )
                    if (dhizukuSupported && dhizukuState != DhizukuState.NOT_INSTALLED) {
                        EngineOption(
                            title = stringResource(R.string.installer_mode_dhizuku),
                            subtitle = when (dhizukuState) {
                                DhizukuState.UNSUPPORTED -> stringResource(R.string.setting_dhizuku_unsupported)
                                DhizukuState.NOT_INSTALLED -> stringResource(R.string.setting_dhizuku_not_installed)
                                DhizukuState.NOT_RUNNING -> stringResource(R.string.setting_dhizuku_not_running)
                                DhizukuState.PROFILE_OWNER_UNSUPPORTED -> stringResource(R.string.setting_dhizuku_profile_owner_unsupported)
                                DhizukuState.NOT_AUTHORIZED -> stringResource(R.string.setting_dhizuku_no_permission)
                                else -> stringResource(R.string.installer_engine_dhizuku_desc)
                            },
                            selected = current == InstallMode.DHIZUKU,
                            enabled = dhizukuSelectable,
                            dimmed = dhizukuDimmed,
                            onClick = {
                                settingViewModel.setInstallMode(InstallMode.DHIZUKU)
                                showPicker = false
                            },
                        )
                    }
                    EngineOption(
                        title = stringResource(R.string.installer_mode_root),
                        subtitle = when {
                            !settingState.rootSupported ->
                                stringResource(R.string.installer_engine_root_unsupported)
                            rootReady -> stringResource(R.string.installer_engine_root_desc)
                            else -> stringResource(R.string.installer_engine_root_request)
                        },
                        selected = current == InstallMode.ROOT,
                        // Clickable as long as the build supports root, even when su isn't
                        // ready — the tap triggers the root request.
                        enabled = settingState.rootSupported,
                        dimmed = rootDimmed,
                        onClick = {
                            settingViewModel.setInstallMode(InstallMode.ROOT)
                            showPicker = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EngineOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    // Greyed-out look without blocking the click — used for engines that aren't ready yet
    // but can still be tapped to start their permission/request flow (e.g. Root).
    dimmed: Boolean = false,
) {
    Row(
        modifier = Modifier
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            val titleColor = if (enabled && !dimmed) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class Mode { Default, Shizuku, Dhizuku, Root }
