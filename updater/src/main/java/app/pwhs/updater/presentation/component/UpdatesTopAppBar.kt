package app.pwhs.updater.presentation.component

import android.content.Context
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import app.pwhs.core.R
import app.pwhs.updater.presentation.AppSortOption
import app.pwhs.updater.presentation.UpdatesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesTopAppBar(
    uiState: UpdatesUiState,
    context: Context,
    searchActive: Boolean = false,
    onToggleSearch: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBackClick: (() -> Unit)?,
    onUpdateAll: () -> Unit,
    onSortOptionChanged: (AppSortOption) -> Unit,
    onCheckAllUpdates: () -> Unit,
    onTokensClick: () -> Unit = {},
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.updates_title),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.basicMarquee(),
                )
                if (uiState.updateCount > 0) {
                    Text(
                        text = stringResource(R.string.updates_available_count, uiState.updateCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.basicMarquee(),
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
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (searchActive) "Close search" else "Search",
                )
            }

            if (uiState.updateCount > 0) {
                IconButton(
                    onClick = onUpdateAll,
                    enabled = !uiState.isUpdatingAll,
                ) {
                    BadgedBox(
                        badge = {
                            Badge { Text(uiState.updateCount.toString()) }
                        },
                    ) {
                        if (uiState.isUpdatingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "Update All",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Updates first") },
                        leadingIcon = {
                            if (uiState.sortOption == AppSortOption.UPDATES_FIRST) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            showSortMenu = false
                            onSortOptionChanged(AppSortOption.UPDATES_FIRST)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Name (A to Z)") },
                        leadingIcon = {
                            if (uiState.sortOption == AppSortOption.NAME_ASC) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            showSortMenu = false
                            onSortOptionChanged(AppSortOption.NAME_ASC)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Last checked") },
                        leadingIcon = {
                            if (uiState.sortOption == AppSortOption.LAST_CHECKED) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            showSortMenu = false
                            onSortOptionChanged(AppSortOption.LAST_CHECKED)
                        },
                    )
                }
            }

            IconButton(
                onClick = onCheckAllUpdates,
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
                        text = { Text("Source API Tokens") },
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onTokensClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.updates_menu_import)) },
                        leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onImportClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.updates_menu_export)) },
                        leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onExportClick()
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
