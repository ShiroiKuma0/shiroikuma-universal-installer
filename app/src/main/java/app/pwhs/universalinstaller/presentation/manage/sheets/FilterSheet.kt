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


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    sortBy: UninstallSortBy,
    direction: SortDirection,
    groupBy: GroupBy,
    appFilter: Set<AppFilter>,
    usageGranted: Boolean,
    onSortChange: (UninstallSortBy) -> Unit,
    onGroupByChange: (GroupBy) -> Unit,
    onRequestUsageAccess: () -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resource = LocalResources.current

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    // Disable Reset when nothing differs from defaults — keeps the affordance honest and
    // matches what a tap would actually change. Defaults mirror `ManageUiState`'s initial
    // values so this stays in sync if those move.
    val isDefaultState = sortBy == UninstallSortBy.Name &&
        direction == SortDirection.Asc &&
        groupBy == GroupBy.None &&
        appFilter == setOf(AppFilter.User)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.uninstall_filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onResetFilters,
                    enabled = !isDefaultState,
                ) {
                    Text(stringResource(R.string.uninstall_filter_reset))
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.uninstall_filter_sort_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortChip(UninstallSortBy.Name, sortBy, direction, stringResource(R.string.uninstall_sort_name)) {
                    onSortChange(UninstallSortBy.Name)
                }
                SortChip(UninstallSortBy.Size, sortBy, direction, stringResource(R.string.uninstall_sort_size)) {
                    onSortChange(UninstallSortBy.Size)
                }
                SortChip(UninstallSortBy.InstalledAt, sortBy, direction, stringResource(R.string.uninstall_sort_installed)) {
                    onSortChange(UninstallSortBy.InstalledAt)
                }
                SortChip(UninstallSortBy.LastUpdated, sortBy, direction, stringResource(R.string.uninstall_sort_last_updated)) {
                    onSortChange(UninstallSortBy.LastUpdated)
                }
                SortChip(UninstallSortBy.LastUsed, sortBy, direction, stringResource(R.string.uninstall_sort_last_used)) {
                    if (!usageGranted) {
                        android.widget.Toast.makeText(
                            context,
                            resource.getString(R.string.uninstall_sort_usage_toast),
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                        onRequestUsageAccess()
                    } else {
                        onSortChange(UninstallSortBy.LastUsed)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGroupByChange(
                            if (groupBy == GroupBy.Installer) GroupBy.None else GroupBy.Installer,
                        )
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.manage_group_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = groupBy == GroupBy.Installer,
                    onCheckedChange = {
                        onGroupByChange(if (it) GroupBy.Installer else GroupBy.None)
                    },
                )
            }
        }
    }
}

/**
 * Click wrapper that doesn't require long-click semantics — the row just toggles, no need
 * for `combinedClickable`. Pulled out so we avoid accidentally using combinedClickable in
 * places where a plain clickable is clearer.
 */
