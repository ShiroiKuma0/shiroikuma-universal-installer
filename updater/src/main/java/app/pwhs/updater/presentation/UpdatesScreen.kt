package app.pwhs.updater.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.core.R
import app.pwhs.core.ui.component.EmptyStateView
import app.pwhs.core.ui.theme.Spacing
import app.pwhs.updater.domain.model.TrackedApp
import app.pwhs.updater.presentation.component.TrackedAppCard
import app.pwhs.updater.presentation.dialog.AddTrackedAppDialog
import app.pwhs.updater.presentation.dialog.AppPickerDialog
import app.pwhs.updater.presentation.dialog.EditCategoryDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedAppToTrack by remember { mutableStateOf<InstalledAppItem?>(null) }
    var appToEditCategory by remember { mutableStateOf<TrackedApp?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                val jsonString = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (jsonString.isNotBlank()) {
                    viewModel.importTrackedAppsFromJson(jsonString) { count ->
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.updates_import_success, count))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.updates_title))
                        if (uiState.updateCount > 0) {
                            Text(
                                text = stringResource(R.string.updates_available_count, uiState.updateCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.updateCount > 0) {
                        Button(
                            onClick = { viewModel.updateAll(context) },
                            enabled = !uiState.isUpdatingAll,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(end = Spacing.S),
                        ) {
                            if (uiState.isUpdatingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(Spacing.XS))
                                Text("Update All (${uiState.updateCount})")
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.checkAllUpdates() },
                        enabled = !uiState.isChecking,
                    ) {
                        if (uiState.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.updates_check_all),
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.updates_menu_import)) },
                                leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch("*/*")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.updates_menu_export)) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val jsonString = viewModel.exportTrackedAppsJson()
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_TEXT, jsonString)
                                        putExtra(Intent.EXTRA_TITLE, "universal_installer_apps.json")
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export Tracked Apps"))
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedAppToTrack = null
                    viewModel.showAddDialog(true)
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.updates_track_app)) },
                shape = MaterialTheme.shapes.large,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                if (uiState.trackedApps.isNotEmpty()) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text(stringResource(R.string.updates_search_placeholder)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.L, vertical = Spacing.S),
                    )

                    // Category Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.L, vertical = Spacing.XS),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S),
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedCategory == null,
                                onClick = { viewModel.onCategorySelected(null) },
                                label = { Text("All") },
                            )
                        }

                        if (uiState.updateCount > 0) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedCategory == UpdatesUiState.CATEGORY_UPDATES,
                                    onClick = {
                                        if (uiState.selectedCategory == UpdatesUiState.CATEGORY_UPDATES) {
                                            viewModel.onCategorySelected(null)
                                        } else {
                                            viewModel.onCategorySelected(UpdatesUiState.CATEGORY_UPDATES)
                                        }
                                    },
                                    label = { Text("Updates (${uiState.updateCount})") },
                                )
                            }
                        }

                        items(uiState.categories) { category ->
                            FilterChip(
                                selected = uiState.selectedCategory == category,
                                onClick = {
                                    if (uiState.selectedCategory == category) {
                                        viewModel.onCategorySelected(null)
                                    } else {
                                        viewModel.onCategorySelected(category)
                                    }
                                },
                                label = { Text(category) },
                            )
                        }
                    }
                }

                if (uiState.trackedApps.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.XXL),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyStateView(
                            icon = Icons.Rounded.CloudDownload,
                            title = stringResource(R.string.updates_no_apps_title),
                            subtitle = stringResource(R.string.updates_no_apps_subtitle),
                            actionLabel = stringResource(R.string.updates_track_first_app),
                            onAction = { viewModel.showAddDialog(true) },
                        )
                    }
                } else if (uiState.filteredApps.isEmpty()) {
                    // Search Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.XXL),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyStateView(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.updates_no_results_title),
                            subtitle = stringResource(R.string.updates_no_results_subtitle, uiState.searchQuery),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.L),
                        verticalArrangement = Arrangement.spacedBy(Spacing.M),
                    ) {
                        items(
                            items = uiState.filteredApps,
                            key = { it.packageName },
                        ) { app ->
                            TrackedAppCard(
                                app = app,
                                isDownloading = uiState.downloadingPackage == app.packageName,
                                downloadProgress = uiState.downloadProgress,
                                onUpdateClick = {
                                    viewModel.downloadAndInstall(context, app)
                                },
                                onCheckClick = {
                                    viewModel.checkSingleUpdate(app.packageName)
                                },
                                onDeleteClick = {
                                    viewModel.removeTrackedApp(app.packageName)
                                },
                                onEditCategoryClick = {
                                    appToEditCategory = app
                                },
                            )
                        }
                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddTrackedAppDialog(
            isAdding = uiState.isAdding,
            errorMessage = uiState.error,
            selectedAppName = selectedAppToTrack?.appName,
            onSelectFromInstalled = {
                viewModel.showAppPickerDialog(true)
            },
            onDismiss = {
                viewModel.showAddDialog(false)
                selectedAppToTrack = null
            },
            onConfirm = { url, prereleases, category ->
                viewModel.addTrackedAppFromUrl(
                    context = context,
                    url = url,
                    includePrereleases = prereleases,
                    targetPackageName = selectedAppToTrack?.packageName,
                    category = category,
                )
            },
        )
    }

    if (uiState.showAppPickerDialog) {
        AppPickerDialog(
            installedApps = uiState.installedApps,
            isLoading = uiState.isLoadingInstalledApps,
            onLoadApps = { viewModel.loadInstalledApps(context) },
            onAppSelected = { app ->
                selectedAppToTrack = app
                viewModel.showAppPickerDialog(false)
            },
            onDismiss = { viewModel.showAppPickerDialog(false) },
        )
    }

    if (appToEditCategory != null) {
        val currentApp = appToEditCategory!!
        EditCategoryDialog(
            appName = currentApp.appName,
            currentCategory = currentApp.category,
            existingCategories = uiState.categories,
            onDismiss = { appToEditCategory = null },
            onConfirm = { newCategory ->
                viewModel.updateAppCategory(currentApp.packageName, newCategory)
                appToEditCategory = null
            },
        )
    }
}
