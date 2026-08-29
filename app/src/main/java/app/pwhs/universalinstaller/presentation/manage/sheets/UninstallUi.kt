@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package app.pwhs.universalinstaller.presentation.manage



import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Stop
import app.pwhs.universalinstaller.util.AndroidAutoCompat
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Store
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.presentation.composable.UniversalSearchBar
import app.pwhs.universalinstaller.presentation.composable.EmptyStateView
import app.pwhs.universalinstaller.presentation.composable.ShimmerBox
import app.pwhs.universalinstaller.presentation.composable.InstallerModeBadge
import app.pwhs.universalinstaller.presentation.install.controller.SystemAppMethod
import app.pwhs.universalinstaller.presentation.manage.logs.UninstallLogsActivity
import app.pwhs.universalinstaller.presentation.manage.permissions.AppPermissionsActivity
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.util.AppIconData
import app.pwhs.universalinstaller.util.BiometricGate
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import app.pwhs.universalinstaller.util.extension.getDisplayName
import org.koin.androidx.compose.koinViewModel


@Composable
internal fun UninstallUi(
    modifier: Modifier = Modifier,
    uiState: ManageUiState = ManageUiState(),
    onSearchQueryChanged: (String) -> Unit = {},
    onToggleAppFilter: (AppFilter) -> Unit = {},
    onOpenAppPrivileged: (String, String) -> Unit = { _, _ -> },
    onUninstall: (String) -> Unit = {},
    onBlockPackage: (String) -> Unit = {},
    blockedPackages: Set<String> = emptySet(),
    onExtract: (String, String) -> Unit = { _, _ -> },
    onShare: (String, String) -> Unit = { _, _ -> },
    onReinstall: (String, String) -> Unit = { _, _ -> },
    onCheckVirusTotal: (InstalledApp) -> Unit = {},
    onAddToServer: (String, String) -> Unit = { _, _ -> },
    onForceStop: (String, String) -> Unit = { _, _ -> },
    onSetEnabled: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onClearData: (String, String) -> Unit = { _, _ -> },
    queryStorage: suspend (String) -> StorageBreakdown? = { null },
    queryUsage: suspend (String) -> List<UsageBucket> = { emptyList() },
    onDismissExtractResult: () -> Unit = {},
    onDismissPrivilegedResult: () -> Unit = {},
    onRefreshPrivileged: () -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onToggleSelectAll: () -> Unit = {},
    onUninstallSelected: () -> Unit = {},
    onForceStopSelected: () -> Unit = {},
    onDisableSelected: () -> Unit = {},
    onClearDataSelected: () -> Unit = {},
    onExtractSelected: () -> Unit = {},
    onDismissBatchExtractResult: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenBackups: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSortChange: (UninstallSortBy) -> Unit = {},
    onGroupByChange: (GroupBy) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onRequestUsageAccess: () -> Unit = {},
    onRefreshUsageAccess: () -> Unit = {},
    onConfirmSystemApp: (SystemAppMethod?) -> Unit = {},
    onDismissSystemApp: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resource = LocalResources.current
    var actionTarget by remember { mutableStateOf<InstalledApp?>(null) }
    var confirmUninstallTarget by remember { mutableStateOf<InstalledApp?>(null) }

    BackHandler(enabled = uiState.isSelectionMode) {
        onClearSelection()
    }
    var confirmClearDataTarget by remember { mutableStateOf<InstalledApp?>(null) }
    val extractInProgress = uiState.extractState is ExtractState.Running
    // Biometric gate state — flag tracked per-attempt rather than per-target so toggling
    // the Settings switch applies on the next uninstall without re-composing.
    val uninstallGateEnabled by remember(context) {
        context.dataStore.data.map {
            it[app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
                .BIOMETRIC_LOCK_UNINSTALL] ?: false
        }
    }.collectAsState(initial = false)
    val gatedUninstall: (String) -> Unit = { pkg ->
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (activity != null) {
            val name = uiState.apps.firstOrNull { it.packageName == pkg }?.appName ?: pkg
            BiometricGate.authenticate(
                activity = activity,
                enabled = uninstallGateEnabled,
                title = resource.getString(R.string.biometric_uninstall_title),
                subtitle = resource.getString(R.string.biometric_uninstall_sub, name),
                onSuccess = { onUninstall(pkg) },
            )
        } else {
            onUninstall(pkg)
        }
    }

    // Action sheet — opens on card tap (when not in selection mode). Adding new actions
    // later is just appending another ActionRow; lifting the dialogs to this level avoided
    // duplicating them inside every card composition.
    actionTarget?.let { target ->
        // Re-probe before each open in case Shizuku permission was just revoked or root
        // shell expired — the cached `privilegedReady` flag may be stale.
        LaunchedEffect(target.packageName) { onRefreshPrivileged() }
        AppActionSheet(
            app = target,
            extractInProgress = extractInProgress,
            privilegedReady = uiState.privilegedReady,
            onOpenApp = {
                actionTarget = null
                if (uiState.privilegedReady) {
                    onOpenAppPrivileged(target.packageName, target.appName)
                } else {
                    launchInstalledApp(context, target.packageName)
                }
            },
            onOpenAppInfo = {
                actionTarget = null
                openAppInfoSettings(context, target.packageName)
            },
            onShare = {
                actionTarget = null
                onShare(target.packageName, target.appName)
            },
            onReinstall = {
                actionTarget = null
                onReinstall(target.packageName, target.appName)
            },
            onCheckVirusTotal = {
                actionTarget = null
                onCheckVirusTotal(target)
            },
            onExtract = {
                actionTarget = null
                onExtract(target.packageName, target.appName)
            },
            onAddToServer = {
                actionTarget = null
                onAddToServer(target.packageName, target.appName)
            },
            onForceStop = {
                actionTarget = null
                onForceStop(target.packageName, target.appName)
            },
            onSetEnabled = { enabled ->
                actionTarget = null
                onSetEnabled(target.packageName, target.appName, enabled)
            },
            onClearData = {
                actionTarget = null
                confirmClearDataTarget = target
            },
            queryStorage = queryStorage,
            queryUsage = queryUsage,
            onUninstall = {
                actionTarget = null
                // System apps bypass the generic confirm — the ViewModel surfaces the
                // root-aware method dialog directly. User apps still get the explicit
                // "Are you sure" guard before destructive action. Biometric gate (when
                // enabled) wraps the system-app path; the user-app path goes through the
                // confirm dialog → its onConfirm calls gatedUninstall.
                if (target.isSystemApp) gatedUninstall(target.packageName)
                else confirmUninstallTarget = target
            },
            onBlockPackage = {
                actionTarget = null
                onBlockPackage(target.packageName)
            },
            isBlocked = target.packageName in blockedPackages,
            onDismiss = { actionTarget = null },
        )
    }

    confirmUninstallTarget?.let { target ->
        UninstallConfirmDialog(
            app = target,
            onConfirm = {
                confirmUninstallTarget = null
                gatedUninstall(target.packageName)
            },
            onDismiss = { confirmUninstallTarget = null },
        )
    }

    confirmClearDataTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmClearDataTarget = null },
            icon = {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(stringResource(R.string.manage_clear_data_confirm_title, target.appName))
            },
            text = { Text(stringResource(R.string.manage_clear_data_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val t = target
                    confirmClearDataTarget = null
                    onClearData(t.packageName, t.appName)
                }) {
                    Text(
                        stringResource(R.string.manage_action_clear_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDataTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (uiState.extractState is ExtractState.Running) {
        val runningState = uiState.extractState
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss, it's running */ },
            title = { Text(stringResource(R.string.extract_progress_title, runningState.appName)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (runningState.totalBytes > 0) {
                        val progress = runningState.bytesCopied.toFloat() / runningState.totalBytes.toFloat()
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${android.text.format.Formatter.formatShortFileSize(context, runningState.bytesCopied)} / ${android.text.format.Formatter.formatShortFileSize(context, runningState.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {}
        )
    }

    (uiState.batchExtractState as? BatchExtractState.Running)?.let { batch ->
        AlertDialog(
            onDismissRequest = { /* running — not dismissable */ },
            title = {
                Text(stringResource(R.string.manage_batch_extract_title, batch.completed + 1, batch.total))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    Text(
                        text = batch.currentName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (batch.totalBytes > 0) {
                        batch.bytesCopied.toFloat() / batch.totalBytes.toFloat()
                    } else 0f
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
        )
    }

    LaunchedEffect(uiState.batchExtractState) {
        val s = uiState.batchExtractState as? BatchExtractState.Done ?: return@LaunchedEffect
        val msg = if (s.failed == 0) {
            resource.getString(R.string.manage_batch_extract_done, s.success)
        } else {
            resource.getString(R.string.manage_batch_extract_partial, s.success, s.failed)
        }
        val res = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = resource.getString(R.string.extract_done_action_open),
            withDismissAction = true,
        )
        if (res == SnackbarResult.ActionPerformed) onOpenBackups()
        onDismissBatchExtractResult()
    }

    // Privileged-action snackbar (Force stop / Disable / Enable). Lives alongside the
    // extract snackbar — both share the same SnackbarHostState; the system Material
    // queue handles back-to-back messages.
    LaunchedEffect(uiState.privilegedActionResult) {
        val result = uiState.privilegedActionResult ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = result.message, withDismissAction = true)
        onDismissPrivilegedResult()
    }

    // Drive the post-extract UX from the ExtractState.mode field — Backup shows snackbar
    // with "Open folder", Share fires the system chooser as soon as the cache file lands.
    LaunchedEffect(uiState.extractState) {
        when (val s = uiState.extractState) {
            is ExtractState.Done -> {
                val fileName = if (s.uri.scheme == "file") {
                    java.io.File(s.uri.path!!).name
                } else {
                    context.contentResolver.getDisplayName(s.uri)
                }
                when (s.mode) {
                    ExtractMode.Backup -> {
                        val res = snackbarHostState.showSnackbar(
                            message = resource.getString(R.string.extract_done, fileName),
                            actionLabel = resource.getString(R.string.extract_done_action_open),
                            withDismissAction = true,
                        )
                        if (res == SnackbarResult.ActionPerformed) onOpenBackups()
                    }
                    ExtractMode.Share -> {
                        val launched = launchShareIntent(context, s.uri, s.appName)
                        if (!launched) {
                            snackbarHostState.showSnackbar(
                                message = resource.getString(
                                    R.string.manage_action_share_failed,
                                    "no app accepts the share",
                                ),
                                withDismissAction = true,
                            )
                        }
                    }
                    ExtractMode.Server -> {
                        snackbarHostState.showSnackbar(
                            message = "Added $fileName to server",
                            withDismissAction = true,
                        )
                    }
                    ExtractMode.Reinstall -> {
                        val installUri = if (s.uri.scheme == "file") {
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${app.pwhs.universalinstaller.BuildConfig.APPLICATION_ID}.fileprovider",
                                java.io.File(s.uri.path!!),
                            )
                        } else {
                            s.uri
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(installUri, "application/vnd.android.package-archive")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setPackage(app.pwhs.universalinstaller.BuildConfig.APPLICATION_ID)
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            snackbarHostState.showSnackbar(
                                message = "Couldn't launch Universal Installer",
                                withDismissAction = true,
                            )
                        }
                    }
                }
                onDismissExtractResult()
            }            is ExtractState.Error -> {
                val msg = when (s.mode) {
                    ExtractMode.Share ->
                        resource.getString(R.string.manage_action_share_failed, s.message)
                    ExtractMode.Backup ->
                        resource.getString(R.string.extract_failed, s.message)
                    ExtractMode.Server ->
                        "Failed to add to server: ${s.message}"
                    ExtractMode.Reinstall ->
                        "Failed to extract for reinstall: ${s.message}"
                }
                snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
                onDismissExtractResult()
            }
            else -> Unit
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showBatchConfirm by remember { mutableStateOf(false) }
    // Selection-mode overflow menu (privileged batch actions) + its destructive confirm.
    var showBatchMenu by remember { mutableStateOf(false) }
    var showBatchClearDataConfirm by remember { mutableStateOf(false) }
    // Search bar visibility — toggled by the top-bar search button. Saved across config
    // changes so a rotation doesn't snap the user out of search. We auto-open it when the
    // VM still holds a query (e.g. process re-creation while searching).
    var searchActive by rememberSaveable { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            // requestFocus throws if the node isn't laid out yet — AnimatedVisibility's
            // entrance handles that timing for us, but the very first frame is still racing.
            runCatching { searchFocusRequester.requestFocus() }
        }
    }
    // Lifted so the filter FAB's long-press can drive the list (scroll to top).
    val listState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    if (showFilterSheet) {
        FilterSheet(
            sortBy = uiState.sortBy,
            direction = uiState.sortDirection,
            groupBy = uiState.groupBy,
            appFilter = uiState.appFilter,
            usageGranted = uiState.usageAccessGranted,
            onSortChange = onSortChange,
            onGroupByChange = onGroupByChange,
            onRequestUsageAccess = onRequestUsageAccess,
            onResetFilters = onResetFilters,
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showBatchConfirm = false
                    onUninstallSelected()
                }) {
                    Text(stringResource(R.string.uninstall), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.uninstall_confirm_batch_title, uiState.selectedPackages.size)) },
            text = {
                Text(stringResource(R.string.uninstall_confirm_batch_text, uiState.selectedPackages.size))
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        )
    }

    if (showBatchClearDataConfirm) {
        val count = uiState.selectedPackages.size
        AlertDialog(
            onDismissRequest = { showBatchClearDataConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showBatchClearDataConfirm = false
                    onClearDataSelected()
                }) {
                    Text(
                        stringResource(R.string.manage_batch_action_clear_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchClearDataConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.manage_batch_clear_data_confirm_title, count)) },
            text = { Text(stringResource(R.string.manage_batch_clear_data_confirm_text)) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
    }

    uiState.systemAppPrompt?.let { prompt ->
        SystemAppDialog(
            prompt = prompt,
            onConfirm = onConfirmSystemApp,
            onDismiss = onDismissSystemApp,
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                // Selection mode top bar
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.uninstall_n_selected, uiState.selectedPackages.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.uninstall_cancel_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleSelectAll) {
                            Icon(
                                Icons.Rounded.SelectAll,
                                contentDescription = if (uiState.isAllSelected) stringResource(R.string.uninstall_deselect_all) else stringResource(R.string.uninstall_select_all),
                                tint = if (uiState.isAllSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        IconButton(onClick = onExtractSelected) {
                            Icon(
                                Icons.Rounded.Inventory2,
                                contentDescription = stringResource(R.string.manage_batch_action_extract),
                            )
                        }
                        IconButton(onClick = {
                            // Skip the generic confirm dialog when any system app is in the
                            // selection — the system-app dialog below covers confirmation and
                            // method choice in one place, so the user doesn't see two dialogs.
                            val hasSystem = uiState.apps
                                .filter { it.packageName in uiState.selectedPackages }
                                .any { it.isSystemApp }
                            if (hasSystem) onUninstallSelected() else showBatchConfirm = true
                        }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.uninstall_selected_action),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        // Privileged batch ops live in an overflow menu — only meaningful
                        // when Root/Shizuku is ready, so we hide the whole affordance
                        // otherwise rather than offer rows that always fail.
                        if (uiState.privilegedReady) {
                            IconButton(onClick = { showBatchMenu = true }) {
                                Icon(
                                    Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.more_actions_cd),
                                )
                            }
                            DropdownMenu(
                                expanded = showBatchMenu,
                                onDismissRequest = { showBatchMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.manage_batch_action_force_stop)) },
                                    leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null) },
                                    onClick = {
                                        showBatchMenu = false
                                        onForceStopSelected()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.manage_batch_action_disable)) },
                                    leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                                    onClick = {
                                        showBatchMenu = false
                                        onDisableSelected()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.manage_batch_action_clear_data)) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showBatchMenu = false
                                        showBatchClearDataConfirm = true
                                    },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            } else {
                LargeTopAppBar(
                    expandedHeight = 140.dp,
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.screen_title_uninstall),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            InstallerModeBadge()
                            Spacer(modifier = Modifier.height(12.dp))

                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            // Toggle: closing also clears whatever was typed so the next
                            // open starts from an empty field, matching the user's mental
                            // model of "X = exit search".
                            if (searchActive) {
                                onSearchQueryChanged("")
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
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                            )
                        }
                        IconButton(onClick = onOpenBackups) {
                            Icon(
                                imageVector = Icons.Rounded.Inventory2,
                                contentDescription = stringResource(R.string.extract_action_backups),
                            )
                        }
                        IconButton(onClick = onOpenLogs) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = stringResource(R.string.uninstall_logs_cd),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode && !uiState.isLoading) {
                // Tap → filter sheet; long-press → scroll to top. M3's `FloatingActionButton`
                // and clickable `Surface` only expose `onClick`, so we compose a FAB-shaped
                // Surface and attach `combinedClickable` ourselves. Avoids a second FAB that
                // clashed visually with the red `DeleteOutline` on each card.
                val haptic = LocalHapticFeedback.current
                Surface(
                    shape = FloatingActionButtonDefaults.shape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(FloatingActionButtonDefaults.shape)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { showFilterSheet = true },
                            onLongClick = {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                                coroutineScope.launch { listState.scrollToItem(0) }
                            },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = stringResource(R.string.uninstall_filter_cd),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Stats banner — recomputed from filteredApps so it follows the active chips.
            // Hidden in selection mode where the top bar already shows "N selected".
            if (!uiState.isSelectionMode) {
                StatsBanner(
                    apps = uiState.filteredApps,
                    sortBy = uiState.sortBy,
                    direction = uiState.sortDirection,
                )
            }

            // Search bar — only mounted when the user has tapped the top-bar search icon,
            // freeing up vertical space for the list when search isn't active.
            UniversalSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                active = !uiState.isSelectionMode && searchActive,
                onActiveChange = { active -> searchActive = active },
                placeholder = stringResource(R.string.uninstall_search_hint),
                focusRequester = searchFocusRequester,
            )

            if (!uiState.isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppFilter.entries.forEach { filter ->
                        val selected = filter in uiState.appFilter
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleAppFilter(filter) },
                            label = { Text(stringResource(appFilterLabel(filter))) },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
            }

            // Re-check usage access when user returns from the Settings screen.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) onRefreshUsageAccess()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            when {
                uiState.isLoading -> {
                    ManageSkeleton()
                }

                uiState.filteredApps.isEmpty() -> {
                    // When the empty list is the result of a search/filter (not a genuinely
                    // empty device), offer a one-tap way out. resetFilters() deliberately
                    // leaves the query alone, so the action clears BOTH or it'd be a no-op
                    // for the common search-miss case.
                    val filtersActive = uiState.searchQuery.isNotBlank() ||
                        uiState.appFilter != setOf(AppFilter.User) ||
                        uiState.sortBy != UninstallSortBy.Name ||
                        uiState.groupBy != GroupBy.None
                    EmptyStateView(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.uninstall_no_apps_found),
                        subtitle = if (uiState.searchQuery.isNotBlank())
                            stringResource(R.string.uninstall_no_match, uiState.searchQuery)
                        else stringResource(R.string.uninstall_no_user_apps),
                        actionLabel = if (filtersActive) stringResource(R.string.uninstall_clear_filters) else null,
                        onAction = if (filtersActive) {
                            {
                                onSearchQueryChanged("")
                                onResetFilters()
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp)
                    )
                }

                else -> {
                    // Any change in sort or filter jumps back to the top. `scrollToItem` is
                    // O(1); `animateScrollToItem` steps through every item and lags hard on
                    // 300+ apps — user already sees the chip flip, the jump doesn't need
                    // animation.
                    LaunchedEffect(
                        uiState.sortBy,
                        uiState.sortDirection,
                        uiState.searchQuery,
                        uiState.appFilter,
                    ) {
                        if (listState.firstVisibleItemIndex != 0 ||
                            listState.firstVisibleItemScrollOffset != 0
                        ) {
                            listState.scrollToItem(0)
                        }
                    }
                    val sideloadLabel = stringResource(R.string.manage_group_other)
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRefresh,
                    ) {
                        LazyColumn(
                            state = listState,
                            // Extra bottom space so the FAB doesn't overlap the last card's
                            // Uninstall button — 56dp FAB + 16dp inset + breathing room.
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                        if (uiState.groupBy == GroupBy.Installer) {
                            // Group-by-installer view. We compute the bucket label per app
                            // (known store → display name; everything else → sideload), then
                            // sort groups by name so the order is stable across loads.
                            val grouped = uiState.filteredApps.groupBy { app ->
                                resolveInstallerInfo(app.installerPackage, app.packageName)
                                    ?.displayName ?: sideloadLabel
                            }.toSortedMap()
                            grouped.forEach { (groupName, apps) ->
                                stickyHeader(key = "h:$groupName") {
                                    GroupHeader(
                                        title = groupName,
                                        count = apps.size,
                                    )
                                }
                                items(
                                    items = apps,
                                    key = { it.packageName },
                                ) { app ->
                                    AppCard(
                                        app = app,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = app.packageName in uiState.selectedPackages,
                                        isBlocked = app.packageName in blockedPackages,
                                        onShowActions = { actionTarget = app },
                                        onLongClick = { onToggleSelection(app.packageName) },
                                        onToggleSelect = { onToggleSelection(app.packageName) },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                AppCard(
                                    app = app,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = app.packageName in uiState.selectedPackages,
                                    isBlocked = app.packageName in blockedPackages,
                                    onShowActions = { actionTarget = app },
                                    onLongClick = { onToggleSelection(app.packageName) },
                                    onToggleSelect = { onToggleSelection(app.packageName) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    }
                }
            }
        }
    }
}
