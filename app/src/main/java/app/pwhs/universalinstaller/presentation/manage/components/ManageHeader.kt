package app.pwhs.universalinstaller.presentation.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.InstallerModeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageHeader(
    isSelectionMode: Boolean,
    selectedCount: Int,
    isAllSelected: Boolean,
    searchActive: Boolean,
    privilegedReady: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onExtractSelected: () -> Unit,
    onRequestBatchUninstall: () -> Unit,
    onForceStopSelected: () -> Unit,
    onDisableSelected: () -> Unit,
    onRequestBatchClearData: () -> Unit,
    onToggleSearch: () -> Unit,
    onRefresh: () -> Unit,
    onOpenBackups: () -> Unit,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBatchMenu by remember { mutableStateOf(false) }

    if (isSelectionMode) {
        TopAppBar(
            modifier = modifier,
            title = {
                Text(stringResource(R.string.uninstall_n_selected, selectedCount))
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
                        contentDescription = if (isAllSelected) stringResource(R.string.uninstall_deselect_all) else stringResource(R.string.uninstall_select_all),
                        tint = if (isAllSelected)
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
                IconButton(onClick = onRequestBatchUninstall) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.uninstall_selected_action),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                if (privilegedReady) {
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
                                onRequestBatchClearData()
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
            modifier = modifier,
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
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
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
}
