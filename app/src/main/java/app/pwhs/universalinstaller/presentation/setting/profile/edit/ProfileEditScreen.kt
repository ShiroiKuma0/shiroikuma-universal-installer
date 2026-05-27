package app.pwhs.universalinstaller.presentation.setting.profile.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.InstallerProfile
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@Composable
fun ProfileEditScreen(
    profileId: String?,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var name by remember { mutableStateOf("") }
    var installerPkg by remember { mutableStateOf("") }
    var backend by remember { mutableStateOf("Default") }
    
    // Flags
    var replaceExisting by remember { mutableStateOf<Boolean?>(null) }
    var allowTest by remember { mutableStateOf<Boolean?>(null) }
    var requestDowngrade by remember { mutableStateOf<Boolean?>(null) }
    var grantAllPermissions by remember { mutableStateOf<Boolean?>(null) }
    var bypassLowTargetSdk by remember { mutableStateOf<Boolean?>(null) }
    var allUsers by remember { mutableStateOf<Boolean?>(null) }

    val originalProfile = remember(profileId, uiState.installerProfiles) {
        uiState.installerProfiles.find { it.id == profileId }
    }

    LaunchedEffect(originalProfile) {
        originalProfile?.let {
            name = it.name
            installerPkg = it.installerPackageName ?: ""
            backend = it.preferredBackend ?: "Default"
            replaceExisting = it.replaceExisting
            allowTest = it.allowTest
            requestDowngrade = it.requestDowngrade
            grantAllPermissions = it.grantAllPermissions
            bypassLowTargetSdk = it.bypassLowTargetSdk
            allUsers = it.allUsers
        }
    }

    ProfileEditUi(
        isNew = profileId == null,
        name = name,
        onNameChange = { name = it },
        installerPkg = installerPkg,
        onInstallerPkgChange = { installerPkg = it },
        backend = backend,
        onBackendChange = { backend = it },
        replaceExisting = replaceExisting,
        onReplaceExistingChange = { replaceExisting = it },
        allowTest = allowTest,
        onAllowTestChange = { allowTest = it },
        requestDowngrade = requestDowngrade,
        onRequestDowngradeChange = { requestDowngrade = it },
        grantAllPermissions = grantAllPermissions,
        onGrantAllPermissionsChange = { grantAllPermissions = it },
        bypassLowTargetSdk = bypassLowTargetSdk,
        onBypassLowTargetSdkChange = { bypassLowTargetSdk = it },
        allUsers = allUsers,
        onAllUsersChange = { allUsers = it },
        onBack = { (context as? android.app.Activity)?.finish() },
        onSave = {
            val profile = (originalProfile ?: InstallerProfile(id = UUID.randomUUID().toString(), name = "")).copy(
                name = name,
                installerPackageName = installerPkg.ifBlank { null },
                preferredBackend = if (backend == "Default") null else backend,
                replaceExisting = replaceExisting,
                allowTest = allowTest,
                requestDowngrade = requestDowngrade,
                grantAllPermissions = grantAllPermissions,
                bypassLowTargetSdk = bypassLowTargetSdk,
                allUsers = allUsers,
            )
            viewModel.saveProfile(profile)
            (context as? android.app.Activity)?.finish()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditUi(
    isNew: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    installerPkg: String,
    onInstallerPkgChange: (String) -> Unit,
    backend: String,
    onBackendChange: (String) -> Unit,
    replaceExisting: Boolean?,
    onReplaceExistingChange: (Boolean?) -> Unit,
    allowTest: Boolean?,
    onAllowTestChange: (Boolean?) -> Unit,
    requestDowngrade: Boolean?,
    onRequestDowngradeChange: (Boolean?) -> Unit,
    grantAllPermissions: Boolean?,
    onGrantAllPermissionsChange: (Boolean?) -> Unit,
    bypassLowTargetSdk: Boolean?,
    onBypassLowTargetSdkChange: (Boolean?) -> Unit,
    allUsers: Boolean?,
    onAllUsersChange: (Boolean?) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                expandedHeight = 120.dp,
                title = {
                    Text(
                        text = if (isNew) stringResource(R.string.profile_create_title) else stringResource(R.string.profile_edit_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSave,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = navBarPadding)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.profile_save))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = navBarPadding + 88.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.profile_name_label)) },
                placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = installerPkg,
                onValueChange = onInstallerPkgChange,
                label = { Text(stringResource(R.string.setting_shizuku_installer_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.setting_shizuku_backend),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                listOf("Default", "Shizuku", "Root").forEach { b ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackendChange(b) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = backend == b, onClick = { onBackendChange(b) })
                        Text(b, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.dialog_menu_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                ProfileFlagRow(stringResource(R.string.setting_shizuku_replace), replaceExisting, onReplaceExistingChange)
                ProfileFlagRow(stringResource(R.string.setting_shizuku_allow_test), allowTest, onAllowTestChange)
                ProfileFlagRow(stringResource(R.string.setting_shizuku_downgrade), requestDowngrade, onRequestDowngradeChange)
                ProfileFlagRow(stringResource(R.string.setting_shizuku_grant_permissions), grantAllPermissions, onGrantAllPermissionsChange)
                ProfileFlagRow(stringResource(R.string.setting_shizuku_bypass_sdk), bypassLowTargetSdk, onBypassLowTargetSdkChange)
                ProfileFlagRow(stringResource(R.string.setting_shizuku_all_users), allUsers, onAllUsersChange)
            }
        }
    }
}

@Composable
private fun ProfileFlagRow(
    label: String,
    checked: Boolean?,
    onCheckedChange: (Boolean?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCheckedChange(
                    when (checked) {
                        true -> false
                        false -> null
                        null -> true
                    }
                )
            }
            .padding(vertical = 8.dp)
    ) {
        val state = when (checked) {
            true -> androidx.compose.ui.state.ToggleableState.On
            false -> androidx.compose.ui.state.ToggleableState.Off
            null -> androidx.compose.ui.state.ToggleableState.Indeterminate
        }
        androidx.compose.material3.TriStateCheckbox(
            state = state,
            onClick = null
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
        )
    }
}
