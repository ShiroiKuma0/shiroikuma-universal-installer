package app.pwhs.tv.presentation.manage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import app.pwhs.core.domain.InstalledApp
import app.pwhs.tv.R
import app.pwhs.tv.formatSize
import app.pwhs.tv.rememberAppIcon
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ManageScreen(
    modifier: Modifier = Modifier,
    viewModel: ManageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    var focusedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    val context = LocalContext.current

    // Default focus: land on the first app once, when the list first appears on this visit,
    // so the D-pad has a home instead of stranding focus on the rail. (The list itself is loaded
    // once by the ViewModel's init and kept in its retained back-stack entry, so revisiting this
    // tab restores instantly without a reload.)
    val firstRowFocus = remember { FocusRequester() }
    var didRequestFocus by remember { mutableStateOf(false) }

    val uninstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // A non-root uninstall just finished via the system dialog. Refresh silently (no skeleton
        // flash) so the list drops the removed app without re-showing the loading state.
        viewModel.refresh()
    }

    LaunchedEffect(uiState.filteredApps, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        val current = focusedApp
        val fresh = uiState.filteredApps.firstOrNull { it.packageName == current?.packageName }
        when {
            // Selection still visible: adopt the refreshed instance so the detail pane (and the
            // Enable/Disable label) tracks state a privileged op just changed.
            fresh != null -> if (fresh != current) focusedApp = fresh
            // Selection dropped out (uninstalled / disabled under the User filter / filtered):
            // re-home the pane AND move D-pad focus back to the list so it isn't stranded on a
            // control being animated away.
            else -> {
                focusedApp = uiState.filteredApps.firstOrNull()
                if (uiState.filteredApps.isNotEmpty()) runCatching { firstRowFocus.requestFocus() }
            }
        }
        if (!didRequestFocus && uiState.filteredApps.isNotEmpty()) {
            didRequestFocus = true
            runCatching { firstRowFocus.requestFocus() }
        }
    }

    // Destructive privileged actions confirm first (no system dialog on the silent root path).
    confirm?.let { pending ->
        ConfirmDialog(
            action = pending,
            onConfirm = {
                when (pending) {
                    is ConfirmAction.Uninstall -> viewModel.uninstallSilent(pending.app)
                    is ConfirmAction.ClearData -> viewModel.clearData(pending.app)
                }
                confirm = null
            },
            onCancel = { confirm = null },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: Apps List (with Sidebar Background)
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface) // Darker sidebar background
                .padding(horizontal = 32.dp, vertical = 40.dp)
        ) {
            Text(
                text = stringResource(R.string.tv_manage_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(Modifier.height(24.dp))

            // Search and Filter Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f).height(60.dp),
                    textStyle = MaterialTheme.typography.titleSmall,
                    placeholder = { Text(stringResource(R.string.tv_manage_search_placeholder), style = MaterialTheme.typography.titleSmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        // Filled + primary-tinted on focus so the field reads as active at 3m,
                        // not just a thin border-color swap.
                        focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFilterChip(
                    selected = uiState.filter == AppFilter.User,
                    label = stringResource(R.string.tv_manage_filter_user),
                    onClick = { viewModel.setFilter(AppFilter.User) }
                )
                SmallFilterChip(
                    selected = uiState.filter == AppFilter.System,
                    label = stringResource(R.string.tv_manage_filter_system),
                    onClick = { viewModel.setFilter(AppFilter.System) }
                )
                SmallFilterChip(
                    selected = uiState.filter == AppFilter.Disabled,
                    label = stringResource(R.string.tv_manage_filter_disabled),
                    onClick = { viewModel.setFilter(AppFilter.Disabled) }
                )
            }

            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFilterChip(
                    selected = uiState.sortBy == SortBy.Name,
                    label = stringResource(R.string.tv_manage_sort_name),
                    onClick = { viewModel.setSortBy(SortBy.Name) }
                )
                SmallFilterChip(
                    selected = uiState.sortBy == SortBy.Size,
                    label = stringResource(R.string.tv_manage_sort_size),
                    onClick = { viewModel.setSortBy(SortBy.Size) }
                )
                SmallFilterChip(
                    selected = uiState.sortBy == SortBy.Date,
                    label = stringResource(R.string.tv_manage_sort_date),
                    onClick = { viewModel.setSortBy(SortBy.Date) }
                )
            }

            Spacer(Modifier.height(24.dp))

            LazyColumn(
                // focusGroup + focusRestorer is what Compose for TV provides instead of
                // Leanback's BrowseFrameLayout focus interception: leaving and re-entering the
                // list restores the previous row directly rather than re-searching the tree.
                modifier = Modifier
                    .fillMaxSize()
                    .focusRestorer()
                    .focusGroup(),
                contentPadding = PaddingValues(bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (uiState.isLoading) {
                    items(8) { LoadingRow() }
                } else if (uiState.filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.tv_manage_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    val firstPackage = uiState.filteredApps.firstOrNull()?.packageName
                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                        // isSelected is passed as a lambda, not a Boolean. Reading focusedApp
                        // here would make every D-pad move recompose the whole list — the item
                        // block runs in the scope that reads the state. Deferring the read to
                        // AppListRow limits it to the two rows whose selection actually flips.
                        AppListRow(
                            app = app,
                            isSelected = { focusedApp?.packageName == app.packageName },
                            focusRequester = if (app.packageName == firstPackage) firstRowFocus else null,
                            onFocus = { focusedApp = app }
                        )
                    }
                }
            }
        }

        // Right Column: Details & Actions (Main Content Background)
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 48.dp, vertical = 48.dp)
        ) {
            AnimatedContent(
                targetState = focusedApp,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "detailTransition"
            ) { app ->
                if (app != null && !uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Text(
                            text = stringResource(R.string.tv_manage_version_prefix, app.versionName),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        Spacer(Modifier.height(24.dp))

                        // Metadata first — non-focusable, so it must sit ABOVE the actions,
                        // otherwise D-pad can't scroll past the last action to reveal it.
                        InfoBlock(label = stringResource(R.string.tv_manage_storage_used), value = formatSize(context, app.sizeBytes))
                        if (app.isSystemApp) InfoBlock(label = stringResource(R.string.tv_manage_type), value = stringResource(R.string.tv_manage_system_app))
                        if (!app.enabled) InfoBlock(label = stringResource(R.string.tv_manage_status), value = stringResource(R.string.tv_manage_status_disabled))

                        Spacer(Modifier.height(32.dp))

                        // Actions — kept last so every focusable row is reachable by D-pad (the
                        // non-focusable metadata above never traps focus). Privileged rows appear
                        // only on rooted boxes; each op's result surfaces in the bottom status pill.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionItem(label = stringResource(R.string.tv_manage_action_open)) {
                                runCatching {
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                                        context.startActivity(intent)
                                    }
                                }
                            }

                            if (uiState.rootAvailable) {
                                ActionItem(label = stringResource(R.string.tv_manage_action_force_stop)) {
                                    viewModel.forceStop(app)
                                }
                                ActionItem(
                                    label = stringResource(
                                        if (app.enabled) R.string.tv_manage_action_disable
                                        else R.string.tv_manage_action_enable
                                    )
                                ) {
                                    viewModel.setEnabled(app, !app.enabled)
                                }
                                ActionItem(
                                    label = stringResource(R.string.tv_manage_action_clear_data),
                                    destructive = true,
                                ) { confirm = ConfirmAction.ClearData(app) }
                            }

                            ActionItem(
                                label = stringResource(R.string.tv_manage_action_uninstall),
                                destructive = true,
                            ) {
                                // Rooted: confirm then uninstall silently; otherwise the system
                                // ACTION_DELETE dialog handles its own confirmation.
                                if (uiState.rootAvailable) {
                                    confirm = ConfirmAction.Uninstall(app)
                                } else {
                                    uninstallLauncher.launch(
                                        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                                    )
                                }
                            }

                            ActionItem(label = stringResource(R.string.tv_manage_action_extract)) {
                                viewModel.extractApp(app.packageName, app.appName)
                            }

                            ActionItem(label = stringResource(R.string.tv_manage_action_settings)) {
                                runCatching {
                                    context.startActivity(
                                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${app.packageName}")
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(64.dp))
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when {
                            uiState.isLoading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ShimmerBox(modifier = Modifier.size(120.dp), shape = RoundedCornerShape(20.dp))
                                Spacer(Modifier.height(24.dp))
                                ShimmerBox(modifier = Modifier.width(200.dp).height(32.dp), shape = RoundedCornerShape(8.dp))
                                Spacer(Modifier.height(8.dp))
                                ShimmerBox(modifier = Modifier.width(140.dp).height(24.dp), shape = RoundedCornerShape(8.dp))
                            }
                            uiState.searchQuery.isNotBlank() -> DetailEmptyState(
                                icon = Icons.Rounded.SearchOff,
                                title = stringResource(R.string.tv_manage_no_matches, uiState.searchQuery),
                                actionLabel = stringResource(R.string.tv_manage_clear_search),
                                onAction = { viewModel.setSearchQuery("") },
                            )
                            else -> DetailEmptyState(
                                icon = Icons.Rounded.Apps,
                                title = stringResource(R.string.tv_manage_empty),
                                actionLabel = null,
                                onAction = {},
                            )
                        }
                    }
                }
            }
        }
    }

        // Screen-level status pill: privileged-action results + extract progress/outcome.
        // Rendered as an overlay so it (and its focusable Dismiss/Retry) is always reachable,
        // never a non-focusable dead zone below the scrolling action list.
        ManageStatusOverlay(
            actionResult = actionResult,
            extractState = uiState.extractState,
            onDismissAction = { viewModel.clearActionResult() },
            onDismissExtract = { viewModel.dismissExtractResult() },
            // Pressing Dismiss removes the focused pill button, so hand focus back to the list
            // rather than stranding it. (The auto-dismiss timers use the plain callbacks above,
            // which must NOT steal focus from wherever the user has moved.)
            onErrorDismiss = {
                viewModel.clearActionResult()
                viewModel.dismissExtractResult()
                runCatching { firstRowFocus.requestFocus() }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppListRow(
    app: InstalledApp,
    isSelected: () -> Boolean,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit
) {
    val icon = rememberAppIcon(app.packageName, sizePx = 120)
    val shape = RoundedCornerShape(12.dp)

    Surface(
        onClick = { /* Detail pane updates on focus */ },
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f), // Minimal scale to avoid overlap
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = if (isSelected()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Text(text = app.appName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected()) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionItem(
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    // Solid, color-shifting focus (plus a larger 1.05 scale) so it's obvious which row — and
    // especially which destructive row — is focused from the couch. A faint rest fill keeps the
    // rows legible as a group even before focus lands.
    val focusContainer = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val focusContent = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val restContent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            focusedContainerColor = focusContainer,
            contentColor = restContent,
            focusedContentColor = focusContent
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SmallFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val shape = CircleShape
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        // Theme-aware fills (Color.Gray/Color.White were invisible / low-contrast on the
        // light-mode white sidebar) with a clear primary focus highlight.
        colors = ButtonDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.height(48.dp).clip(shape)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp))
        Spacer(Modifier.width(16.dp))
        ShimmerBox(modifier = Modifier.width(140.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-alpha",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f)),
    )
}

/** A destructive Manage action awaiting confirmation (silent root path has no system dialog). */
private sealed interface ConfirmAction {
    val app: InstalledApp
    data class Uninstall(override val app: InstalledApp) : ConfirmAction
    data class ClearData(override val app: InstalledApp) : ConfirmAction
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfirmDialog(
    action: ConfirmAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val title = when (action) {
        is ConfirmAction.Uninstall -> stringResource(R.string.tv_manage_confirm_uninstall_title, action.app.appName)
        is ConfirmAction.ClearData -> stringResource(R.string.tv_manage_confirm_clear_title, action.app.appName)
    }
    // Default focus on Cancel so a stray center-press never fires the destructive action.
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.widthIn(min = 420.dp, max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(Modifier.padding(32.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.tv_manage_confirm_irreversible),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val btnShape = RoundedCornerShape(14.dp)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).clip(btnShape),
                        shape = ButtonDefaults.shape(btnShape),
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(R.string.tv_manage_confirm_yes), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).clip(btnShape).focusRequester(cancelFocus),
                        shape = ButtonDefaults.shape(btnShape),
                        colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) { Text(stringResource(R.string.tv_manage_confirm_cancel), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ManageStatusOverlay(
    actionResult: ActionResult?,
    extractState: ExtractState,
    onDismissAction: () -> Unit,
    onDismissExtract: () -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Transient successes fade themselves; errors linger with a focusable Dismiss.
    LaunchedEffect(actionResult) {
        if (actionResult != null && !actionResult.isError) {
            delay(2500)
            onDismissAction()
        }
    }
    LaunchedEffect(extractState) {
        when (extractState) {
            is ExtractState.Done -> { delay(3000); onDismissExtract() }
            is ExtractState.Error -> { delay(5000); onDismissExtract() }
            else -> {}
        }
    }

    val visible = actionResult != null || extractState !is ExtractState.Idle
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        // Color and text come from the SAME chosen source (actionResult wins over extract) so a
        // success message can never render in the error/red style, or vice-versa.
        val isError: Boolean
        val message: String
        if (actionResult != null) {
            isError = actionResult.isError
            message = actionResult.message
        } else {
            when (val e = extractState) {
                is ExtractState.Running -> {
                    isError = false
                    val pct = if (e.totalBytes > 0) (e.bytesCopied * 100 / e.totalBytes).toInt() else 0
                    message = stringResource(R.string.tv_manage_extracting, pct)
                }
                is ExtractState.Done -> { isError = false; message = stringResource(R.string.tv_manage_extracted_success) }
                is ExtractState.Error -> { isError = true; message = stringResource(R.string.tv_manage_extract_error_prefix, e.message) }
                else -> { isError = false; message = "" }
            }
        }
        val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        Surface(
            modifier = Modifier.widthIn(min = 360.dp, max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = container, contentColor = content),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isError) {
                    Spacer(Modifier.width(20.dp))
                    val btnShape = CircleShape
                    Button(
                        onClick = onErrorDismiss,
                        shape = ButtonDefaults.shape(btnShape),
                        modifier = Modifier.clip(btnShape),
                        colors = ButtonDefaults.colors(containerColor = content.copy(alpha = 0.15f), contentColor = content),
                    ) { Text(stringResource(R.string.tv_receive_dismiss)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(420.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            val shape = CircleShape
            Button(onClick = onAction, shape = ButtonDefaults.shape(shape), modifier = Modifier.clip(shape)) {
                Text(actionLabel)
            }
        }
    }
}
