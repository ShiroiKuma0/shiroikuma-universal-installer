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
internal fun AppActionSheet(
    app: InstalledApp,
    extractInProgress: Boolean,
    privilegedReady: Boolean,
    onOpenApp: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onShare: () -> Unit,
    onReinstall: () -> Unit,
    onCheckVirusTotal: () -> Unit,
    onAddToServer: () -> Unit,
    onExtract: () -> Unit,
    onForceStop: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onClearData: () -> Unit,
    queryStorage: suspend (String) -> StorageBreakdown?,
    queryUsage: suspend (String) -> List<UsageBucket>,
    onUninstall: () -> Unit,
    onBlockPackage: () -> Unit,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Cheap PM IPC; stable across recompositions so memoize per package.
    val launchable = remember(app.packageName) {
        context.packageManager.getLaunchIntentForPackage(app.packageName) != null
    }
    var storage by remember(app.packageName) { mutableStateOf<StorageBreakdown?>(null) }
    var usage by remember(app.packageName) { mutableStateOf<List<UsageBucket>>(emptyList()) }
    LaunchedEffect(app.packageName) {
        storage = queryStorage(app.packageName)
        usage = queryUsage(app.packageName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // Sheet content can grow taller than the viewport once usage chart + storage
        // chips + privileged group + intent rows all render. ModalBottomSheet's body
        // slot doesn't scroll on its own, so wrap in our own scrolling column.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
        // Header: app icon + name + package name. Same shell as the AppCard so users see
        // the in-context selection echoed back to them.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AppIconData(app.packageName))
                    .build(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium),
                error = {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    )
                },
                success = { SubcomposeAsyncImageContent() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    // Long package names overflowed the sheet header — scroll them instead
                    // of truncating so the full id is readable (#79).
                    modifier = Modifier.basicMarquee(),
                )
                if (app.versionName.isNotBlank()) {
                    Text(
                        text = "v${app.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (app.isSystemApp) {
                    Text(
                        text = stringResource(R.string.uninstall_system_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (!app.enabled) {
                    if (app.isSystemApp) Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.manage_disabled_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // Usage chart — renders only when there's at least one bucket with foreground time.
        // Hidden silently when Usage Access isn't granted (queryUsage returns []).
        val totalUsageMs = remember(usage) { usage.sumOf { it.foregroundMillis } }
        if (totalUsageMs > 0L) {
            UsageChart(
                buckets = usage,
                totalMillis = totalUsageMs,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }

        // Storage breakdown — only renders when StorageStatsManager returns data (Usage
        // Access granted + API 26+). Three lightweight chips so the user grasps APK vs
        // Data vs Cache at a glance without a deep dialog.
        storage?.let { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StorageChip(
                    label = stringResource(R.string.manage_storage_app),
                    value = android.text.format.Formatter.formatShortFileSize(context, s.appBytes),
                    weight = 1f,
                )
                StorageChip(
                    label = stringResource(R.string.manage_storage_data),
                    value = android.text.format.Formatter.formatShortFileSize(context, s.dataBytes),
                    weight = 1f,
                )
                StorageChip(
                    label = stringResource(R.string.manage_storage_cache),
                    value = android.text.format.Formatter.formatShortFileSize(context, s.cacheBytes),
                    weight = 1f,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // "Open in store" only renders when we have a known installer source. Sideload
        // installs and unknown sources don't get this row — there's nowhere meaningful
        // to send the user.
        val storeInfo = remember(app.installerPackage, app.packageName) {
            resolveInstallerInfo(app.installerPackage, app.packageName)
        }
        val isAaApp = app.isAndroidAutoSupported
        val isAaInstalled = remember { AndroidAutoCompat.isAndroidAutoInstalled(context) }

        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            if (launchable) {
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.Launch,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.manage_action_open_app),
                    subtitle = "",
                    onClick = onOpenApp,
                )
            }
            storeInfo?.let { info ->
                if (info.intent != null) {
                    ActionRow(
                        icon = Icons.Rounded.Store,
                        iconTint = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.manage_action_open_in_store, info.displayName),
                        subtitle = "",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    info.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                            onDismiss()
                        },
                    )
                } else {
                    ActionRow(
                        icon = Icons.Rounded.Android,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = stringResource(R.string.manage_action_installer_source, info.displayName),
                        subtitle = "",
                        onClick = { },
                    )
                }
            }
            ActionRow(
                icon = Icons.Rounded.Info,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.manage_action_app_info),
                subtitle = "",
                onClick = onOpenAppInfo,
            )
            ActionRow(
                icon = Icons.Rounded.Security,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.manage_action_permissions),
                subtitle = "",
                onClick = {
                    val intent = Intent(context, AppPermissionsActivity::class.java)
                        .putExtra(AppPermissionsActivity.EXTRA_PACKAGE_NAME, app.packageName)
                    runCatching { context.startActivity(intent) }
                    onDismiss()
                },
            )
            ActionRow(
                icon = Icons.Rounded.Share,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.manage_action_share),
                subtitle = "",
                enabled = !extractInProgress,
                onClick = onShare,
            )

            if (isAaApp && app.installerPackage != "com.android.vending" && isAaInstalled) {
                ActionRow(
                    icon = Icons.Rounded.DirectionsCar,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.aa_open_settings),
                    subtitle = "",
                    onClick = {
                        val aaIntent = AndroidAutoCompat.getSettingsIntent(context)
                        if (aaIntent != null) {
                            runCatching { context.startActivity(aaIntent) }
                        }
                        onDismiss()
                    },
                )
            }
            ActionRow(
                icon = Icons.Rounded.Refresh,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.manage_action_reinstall),
                subtitle = "",
                enabled = !extractInProgress,
                onClick = onReinstall,
            )
            ActionRow(
                icon = Icons.Rounded.Search,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.manage_action_check_vt),
                subtitle = "",
                onClick = {
                    onCheckVirusTotal()
                    onDismiss()
                },
            )
            ActionRow(
                icon = Icons.Rounded.CloudUpload,
                iconTint = MaterialTheme.colorScheme.primary,
                label = "Add to Server",
                subtitle = "",
                enabled = !extractInProgress,
                onClick = onAddToServer,
            )
            ActionRow(
                icon = if (app.hasSplits) Icons.Rounded.FolderZip else Icons.Rounded.Inventory2,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.extract_action),
                subtitle = "",
                enabled = !extractInProgress,
                onClick = onExtract,
            )
        }

        if (privilegedReady) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.manage_section_advanced),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                ActionRow(
                    icon = Icons.Rounded.Block,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.manage_action_force_stop),
                    subtitle = "",
                    onClick = onForceStop,
                )
                if (app.enabled) {
                    ActionRow(
                        icon = Icons.Rounded.PowerSettingsNew,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        label = stringResource(R.string.manage_action_disable),
                        subtitle = "",
                        onClick = { onSetEnabled(false) },
                    )
                } else {
                    ActionRow(
                        icon = Icons.Rounded.PlayCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.manage_action_enable),
                        subtitle = "",
                        onClick = { onSetEnabled(true) },
                    )
                }
                ActionRow(
                    icon = Icons.Rounded.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    label = stringResource(R.string.manage_action_clear_data),
                    subtitle = "",
                    onClick = onClearData,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            ActionRow(
                icon = Icons.Rounded.DeleteOutline,
                iconTint = MaterialTheme.colorScheme.error,
                label = stringResource(R.string.uninstall),
                subtitle = "",
                onClick = onUninstall,
            )
            ActionRow(
                icon = Icons.Rounded.Block,
                iconTint = if (isBlocked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                label = stringResource(
                    if (isBlocked) R.string.manage_action_unblock else R.string.manage_action_block
                ),
                subtitle = "",
                onClick = onBlockPackage,
            )
        }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.25f)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

@Composable
internal fun UninstallConfirmDialog(
    app: InstalledApp,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.uninstall),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.uninstall_confirm_single_title, app.appName)) },
        text = {
            Text(stringResource(R.string.uninstall_confirm_single_text, app.appName, app.packageName))
        },
        icon = {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AppIconData(app.packageName))
                    .build(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium),
                error = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                success = { SubcomposeAsyncImageContent() },
            )
        },
    )
}

// ── Intent helpers ──────────────────────────────────────────────────────────

