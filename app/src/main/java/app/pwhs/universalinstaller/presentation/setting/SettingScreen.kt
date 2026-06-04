package app.pwhs.universalinstaller.presentation.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.DirectionsCar
import app.pwhs.universalinstaller.util.AndroidAutoCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.telemetry.Telemetry
import app.pwhs.universalinstaller.domain.model.ExternalOpenMode
import app.pwhs.universalinstaller.domain.model.InstallUiStyle
import app.pwhs.universalinstaller.presentation.composable.EmptyStateView
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.presentation.composable.UniversalSearchBar
import app.pwhs.universalinstaller.presentation.install.controller.RootState
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.pwhs.universalinstaller.util.AppIconData
import app.pwhs.universalinstaller.util.DhizukuState
import app.pwhs.universalinstaller.presentation.setting.profile.PackageNamePickerDialog
import androidx.datastore.preferences.core.Preferences
import org.koin.androidx.compose.koinViewModel
import app.pwhs.universalinstaller.presentation.setting.sections.InstallOptionsSection
import app.pwhs.universalinstaller.presentation.setting.sections.InstallSection
import app.pwhs.universalinstaller.presentation.setting.sections.InterfaceSection
import app.pwhs.universalinstaller.presentation.setting.sections.PrivacySection
import app.pwhs.universalinstaller.presentation.setting.sections.SecuritySection
import app.pwhs.universalinstaller.presentation.setting.sections.SyncSection
import app.pwhs.universalinstaller.presentation.setting.components.*
@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dhizukuState by viewModel.dhizukuState.collectAsState()
    val useDhizuku by viewModel.useDhizuku.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    val securityLevel by viewModel.securityLevel.collectAsState()
    val externalOpenMode by viewModel.externalOpenMode.collectAsState()
    val installUiStyle by viewModel.installUiStyle.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Dhizuku can be granted or revoked in its own app while we are backgrounded.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDhizukuState()
        onPauseOrDispose {}
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { stringRes ->
            android.widget.Toast.makeText(context, stringRes, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    SettingUi(
        modifier = modifier,
        uiState = uiState,
        onInstallModeChanged = viewModel::setInstallMode,
        onVirusTotalKeyChanged = viewModel::setVirusTotalApiKey,
        securityLevel = securityLevel,
        externalOpenMode = externalOpenMode,
        onExternalOpenModeChanged = viewModel::setExternalOpenMode,
        installUiStyle = installUiStyle,
        onInstallUiStyleChanged = viewModel::setInstallUiStyle,
        onSecurityLevelChanged = viewModel::setSecurityLevel,
        onShizukuOptionChanged = viewModel::setShizukuOption,
        dhizukuState = dhizukuState,
        useDhizuku = useDhizuku,
        onUseDhizukuChanged = viewModel::setUseDhizuku,
        onPrivilegedOptionChanged = viewModel::setPrivilegedOption,
        onInstallerPackageChanged = viewModel::setInstallerPackageName,
        blacklist = blacklist,
        onReplayTutorial = {
            // Reuse MainActivity's onboarding route rather than clearing ONBOARDING_COMPLETED:
            // clearing it would also re-show the tour on the next cold start, which nobody asked
            // for. This shows it once, on demand.
            context.startActivity(
                android.content.Intent(context, app.pwhs.universalinstaller.MainActivity::class.java)
                    .putExtra(
                        app.pwhs.universalinstaller.presentation.splash.SplashActivity.EXTRA_SHOW_ONBOARDING,
                        true,
                    )
            )
        },
        onShizukuInstallerChanged = viewModel::setShizukuInstallerPackageName,
        onDeleteApkChanged = viewModel::setDeleteApkAfterInstall,
        onAutoOpenAfterInstallChanged = viewModel::setAutoOpenAfterInstall,
        onLanguageClick = {
            context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.language.LanguageActivity::class.java))
        },
        onRootRetry = viewModel::retryRoot,
        onRootOptionChanged = viewModel::setRootOption,
        onRootInstallerChanged = viewModel::setRootInstallerPackageName,
        onSyncRequirePinChanged = viewModel::setSyncRequirePin,
        onSyncPinCodeChanged = viewModel::setSyncPinCode,
        onSyncServerPortChanged = viewModel::setSyncServerPort,
        onBiometricLockInstallChanged = viewModel::setBiometricLockInstall,
        onBiometricLockUninstallChanged = viewModel::setBiometricLockUninstall,
        onAutoConfirmExternalInstallChanged = viewModel::setAutoConfirmExternalInstall,
        onShowDownloadTabChanged = viewModel::setShowDownloadTab,
        onDefaultInstallerChanged = viewModel::toggleDefaultInstaller,
        onProfilesClick = {
            context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.profile.ProfileActivity::class.java))
        },
        analyticsEnabled = analyticsEnabled,
        onAnalyticsEnabledChanged = viewModel::setAnalyticsEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingUi(
    modifier: Modifier = Modifier,
    uiState: SettingUiState = SettingUiState(),
    onInstallModeChanged: (InstallMode) -> Unit = {},
    onVirusTotalKeyChanged: (String) -> Unit = {},
    securityLevel: SecurityLevel = SecurityLevel.Normal,
    externalOpenMode: ExternalOpenMode = ExternalOpenMode.Dialog,
    onExternalOpenModeChanged: (ExternalOpenMode) -> Unit = {},
    installUiStyle: InstallUiStyle = InstallUiStyle.Dialog,
    onInstallUiStyleChanged: (InstallUiStyle) -> Unit = {},
    onSecurityLevelChanged: (SecurityLevel) -> Unit = {},
    onShizukuOptionChanged: (Preferences.Key<Boolean>, Boolean) -> Unit = { _, _ -> },
    onReplayTutorial: () -> Unit = {},
    // Not in SettingUiState: that is built by an index-based combine() and extending it means
    // renumbering every cast in the block.
    dhizukuState: DhizukuState = DhizukuState.NOT_INSTALLED,
    useDhizuku: Boolean = false,
    onUseDhizukuChanged: (Boolean) -> Unit = {},
    onPrivilegedOptionChanged: (SettingViewModel.PrivilegedOption, Boolean) -> Unit = { _, _ -> },
    onInstallerPackageChanged: (String) -> Unit = {},
    blacklist: List<String> = emptyList(),
    onShizukuInstallerChanged: (String) -> Unit = {},
    onDeleteApkChanged: (Boolean) -> Unit = {},
    onAutoOpenAfterInstallChanged: (Boolean) -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onRootRetry: () -> Unit = {},
    onRootOptionChanged: (Preferences.Key<Boolean>, Boolean) -> Unit = { _, _ -> },
    onRootInstallerChanged: (String) -> Unit = {},
    onSyncRequirePinChanged: (Boolean) -> Unit = {},
    onSyncPinCodeChanged: (String) -> Unit = {},
    onSyncServerPortChanged: (String) -> Unit = {},
    onBiometricLockInstallChanged: (Boolean) -> Unit = {},
    onBiometricLockUninstallChanged: (Boolean) -> Unit = {},
    onAutoConfirmExternalInstallChanged: (Boolean) -> Unit = {},
    onShowDownloadTabChanged: (Boolean) -> Unit = {},
    onDefaultInstallerChanged: (Boolean) -> Unit = {},
    onProfilesClick: () -> Unit = {},
    analyticsEnabled: Boolean = true,
    onAnalyticsEnabledChanged: (Boolean) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Settings search. Filters per-item (discrete rows hide individually) and per-section
    // (a section disappears entirely when nothing under it matches). Survives rotation.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val q = searchQuery.trim()

    var searchActive by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            // Small delay to ensure the search bar is mounted before requesting focus.
            kotlinx.coroutines.delay(100)
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                expandedHeight = 120.dp,
                title = {
                    Text(
                        text = stringResource(R.string.setting_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (searchActive) {
                            searchQuery = ""
                            searchActive = false
                        } else {
                            searchActive = true
                        }
                    }) {
                        Icon(
                            imageVector = if (searchActive) Icons.Rounded.Close
                            else Icons.Rounded.Search,
                            contentDescription = stringResource(
                                if (searchActive) R.string.uninstall_search_close_cd
                                else R.string.uninstall_search_open_cd,
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            UniversalSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = stringResource(R.string.setting_search_hint),
                focusRequester = searchFocusRequester,
            )

            // Resolve searchable labels here (composable scope) so the LazyListScope `if`
            // gates below — which aren't composable — can decide section visibility without
            // calling stringResource. `matchesQuery` returns true on a blank query, so the
            // full list renders when search is empty.
            val installLabels = listOf(stringResource(R.string.setting_use_dhizuku_title), "dhizuku", 
                stringResource(R.string.setting_install_mode_title), "shizuku", "root", "default",
                stringResource(R.string.setting_delete_apk_title),
                stringResource(R.string.setting_auto_open_title),
                stringResource(R.string.setting_auto_confirm_title),
                stringResource(R.string.setting_show_download_tab_title),
                stringResource(R.string.setting_default_installer_title),
            )
            val privilegedLabels = listOf(
                stringResource(R.string.setting_section_install_options),
                "shizuku", "root", "dhizuku", "downgrade", "replace",
            )
            val profileLabels = listOf(
                stringResource(R.string.setting_profiles_title),
                stringResource(R.string.setting_profiles_subtitle), "profile",
            )
            val interfaceLabels = listOf(
                "interface",
                stringResource(R.string.shiroikuma_ui_title),
                stringResource(R.string.theme_screen_title),
                stringResource(R.string.setting_language_title),
            )
            val securityLabels = listOf("security", "lock", "biometric", "fingerprint", "installations", "uninstalls",
                stringResource(R.string.setting_blacklist_title), "blacklist", "block",
            )
            val syncLabels = listOf("sync", "port", "pin")
            val advancedLabels = listOf("advanced", "virustotal", "api key")
            val privacyLabels = listOf(
                stringResource(R.string.setting_analytics_title),
                "privacy", "analytics", "crash", "data", "telemetry",
            )
            val aboutLabels = listOf(stringResource(R.string.help_title), "help", "tutorial",
                "about", "diagnostics",
                stringResource(R.string.setting_section_about),
            )

            // Whether any (currently-applicable) section survives the filter — drives the
            // "no results" state. Shizuku/Root only count when they'd be shown at all.
            val anyVisible = matchesQuery(q, installLabels) ||
                    matchesQuery(q, privilegedLabels) ||
                    matchesQuery(q, profileLabels) ||
                    matchesQuery(q, interfaceLabels) ||
                    matchesQuery(q, securityLabels) ||
                    matchesQuery(q, syncLabels) ||
                    matchesQuery(q, advancedLabels) ||
                    (Telemetry.isCollecting && matchesQuery(q, privacyLabels)) ||
                    matchesQuery(q, aboutLabels)

            androidx.compose.animation.Crossfade(
                targetState = uiState.isLoading,
                label = "SettingsLoading",
                modifier = Modifier.fillMaxSize()
            ) { isLoading ->
                if (isLoading) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = navBarPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Installation Section ─────────────────────
                InstallSection(
                    q = q,
                    installLabels = installLabels,
                    uiState = uiState,
                    dhizukuState = dhizukuState,
                    useDhizuku = useDhizuku,
                    context = context,
                    onInstallModeChanged = onInstallModeChanged,
                    onUseDhizukuChanged = onUseDhizukuChanged,
                    onRootRetry = onRootRetry,
                    onDeleteApkChanged = onDeleteApkChanged,
                    onAutoOpenAfterInstallChanged = onAutoOpenAfterInstallChanged,
                    onDefaultInstallerChanged = onDefaultInstallerChanged
                )

                // ── Install options ──────────────────────────
                // Install options section: shows full privileged flags when Shizuku/Root/Dhizuku is enabled,
                // and shows Install Source configuration for all modes.
                InstallOptionsSection(
                    q = q,
                    privilegedLabels = privilegedLabels,
                    uiState = uiState,
                    useDhizuku = useDhizuku,
                    onPrivilegedOptionChanged = onPrivilegedOptionChanged,
                    onInstallerPackageChanged = onInstallerPackageChanged,
                    onShizukuOptionChanged = onShizukuOptionChanged
                )

                // ── Profiles Section ─────────────────────────
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

                // ── Interface Section ────────────────────────
                InterfaceSection(
                    q = q,
                    interfaceLabels = interfaceLabels,
                    context = context,
                    onLanguageClick = onLanguageClick
                )

                // ── Security Section ─────────────────────────
                SecuritySection(
                    q = q,
                    securityLabels = securityLabels,
                    context = context,
                    blacklist = blacklist,
                    biometricLockInstall = uiState.biometricLockInstall,
                    biometricLockUninstall = uiState.biometricLockUninstall,
                    biometricEnrolmentAvailable = uiState.biometricEnrolmentAvailable,
                    onBiometricLockInstallChanged = onBiometricLockInstallChanged,
                    onBiometricLockUninstallChanged = onBiometricLockUninstallChanged
                )

                // ── Privacy Section ──────────────────────────
                // Absent from the open-source build: Telemetry has no sink there, so the
                // switch would promise control over something that never happens.
                PrivacySection(
                    q = q,
                    privacyLabels = privacyLabels,
                    analyticsEnabled = analyticsEnabled,
                    onAnalyticsEnabledChanged = onAnalyticsEnabledChanged
                )

                // ── Sync Section ─────────────────────────────
                SyncSection(
                    q = q,
                    syncLabels = syncLabels,
                    context = context,
                    syncOptions = uiState.syncOptions,
                    onSyncServerPortChanged = onSyncServerPortChanged,
                    onSyncRequirePinChanged = onSyncRequirePinChanged,
                    onSyncPinCodeChanged = onSyncPinCodeChanged
                )

                // ── Advanced Options ─────────────────────────
                if (matchesQuery(q, advancedLabels)) item {
                    SettingsSection(title = stringResource(R.string.setting_section_advanced), icon = Icons.Rounded.Terminal) {
                        OutlinedTextField(
                            value = uiState.virusTotalApiKey,
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

                // ── About Section ────────────────────────────
                if (matchesQuery(q, aboutLabels)) item {
                    SettingsSection(title = stringResource(R.string.setting_section_about), icon = Icons.Rounded.Info) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.help_title)) },
                            leadingContent = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.help.HelpActivity::class.java))
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        // The onboarding tour is the other half of #102, and existing installs
                        // have ONBOARDING_COMPLETED set, so it is unreachable without this.
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.help_replay_tutorial)) },
                            supportingContent = { Text(stringResource(R.string.help_replay_tutorial_sub)) },
                            leadingContent = { Icon(Icons.Rounded.Replay, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { onReplayTutorial() },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.setting_section_about)) },
                            supportingContent = { Text("v${uiState.appVersion}") },
                            leadingContent = { Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.about.AboutActivity::class.java))
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.setting_diagnostics_title)) },
                            leadingContent = { Icon(Icons.Rounded.BugReport, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.diagnostics.DiagnosticsActivity::class.java))
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        val isAaInstalled = remember { AndroidAutoCompat.isAndroidAutoInstalled(context) }
                        if (isAaInstalled) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.aa_open_settings)) },
                                supportingContent = { Text(stringResource(R.string.aa_open_settings_sub)) },
                                leadingContent = { Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    val aaIntent = AndroidAutoCompat.getSettingsIntent(context)
                                    if (aaIntent != null) {
                                        runCatching { context.startActivity(aaIntent) }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }

                if (q.isNotBlank() && !anyVisible) item {
                    EmptyStateView(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.setting_search_no_results),
                        subtitle = stringResource(R.string.setting_search_no_results_sub, q),
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(top = 48.dp, start = 32.dp, end = 32.dp),
                    )
                }
            } // end of LazyColumn
                } // end of else
            } // end of Crossfade
        } // end of Column
    } // end of Scaffold
} // end of SettingUi

/** True when [query] is blank (everything passes) or any [haystacks] entry contains it. */
