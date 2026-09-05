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
import app.pwhs.universalinstaller.ui.theme.surfaceBorder
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
internal fun SystemAppDialog(
    prompt: SystemAppPrompt,
    onConfirm: (SystemAppMethod?) -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        is SystemAppPrompt.Single -> SystemAppMethodDialog(
            title = stringResource(R.string.uninstall_system_dialog_title_single),
            warning = stringResource(R.string.uninstall_system_warning_single, prompt.appName),
            allowSkip = false,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is SystemAppPrompt.Batch -> SystemAppMethodDialog(
            title = stringResource(R.string.uninstall_system_dialog_title_batch),
            warning = stringResource(
                R.string.uninstall_system_warning_batch,
                prompt.systemApps.size + prompt.userApps.size,
                prompt.userApps.size,
                prompt.systemApps.size,
            ),
            systemAppsPreview = prompt.systemApps,
            allowSkip = prompt.userApps.isNotEmpty(),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is SystemAppPrompt.PrivilegedRequired -> SystemAppPrivilegedRequiredDialog(
            prompt = prompt,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Single shared implementation for the Single and Batch variants. `allowSkip=true` exposes
 * the third radio option ("Skip system apps") which makes sense only when the user also
 * has regular apps in the selection that can still be uninstalled normally.
 */
@Composable
internal fun SystemAppMethodDialog(
    title: String,
    warning: String,
    allowSkip: Boolean,
    onConfirm: (SystemAppMethod?) -> Unit,
    onDismiss: () -> Unit,
    systemAppsPreview: List<Pair<String, String>> = emptyList(),
) {
    // Sealed local type to let the radio group include "Skip" alongside real methods
    // without polluting the shared enum.
    var selection by remember { mutableStateOf<Choice>(Choice.Method(SystemAppMethod.UninstallForUser0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.surfaceBorder(),
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (systemAppsPreview.isNotEmpty()) {
                    val shown = systemAppsPreview.take(4).joinToString(", ") { it.second }
                    val more = (systemAppsPreview.size - 4).coerceAtLeast(0)
                    Text(
                        text = if (more > 0) "$shown, +$more" else shown,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.uninstall_system_method_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                SystemMethodRadio(
                    title = stringResource(R.string.uninstall_system_method_per_user_title),
                    subtitle = stringResource(R.string.uninstall_system_method_per_user_sub),
                    selected = selection == Choice.Method(SystemAppMethod.UninstallForUser0),
                    onClick = { selection = Choice.Method(SystemAppMethod.UninstallForUser0) },
                )
                SystemMethodRadio(
                    title = stringResource(R.string.uninstall_system_method_disable_title),
                    subtitle = stringResource(R.string.uninstall_system_method_disable_sub),
                    selected = selection == Choice.Method(SystemAppMethod.Disable),
                    onClick = { selection = Choice.Method(SystemAppMethod.Disable) },
                )
                if (allowSkip) {
                    SystemMethodRadio(
                        title = stringResource(R.string.uninstall_system_method_skip_title),
                        subtitle = stringResource(R.string.uninstall_system_method_skip_sub),
                        selected = selection == Choice.Skip,
                        onClick = { selection = Choice.Skip },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(when (val c = selection) {
                    is Choice.Method -> c.method
                    Choice.Skip -> null
                })
            }) {
                Text(
                    text = stringResource(R.string.uninstall_system_continue),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private sealed interface Choice {
    data class Method(val method: SystemAppMethod) : Choice
    data object Skip : Choice
}

@Composable
internal fun SystemMethodRadio(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SystemAppPrivilegedRequiredDialog(
    prompt: SystemAppPrompt.PrivilegedRequired,
    onConfirm: (SystemAppMethod?) -> Unit,
    onDismiss: () -> Unit,
) {
    val hasRegular = prompt.userAppsAvailable.isNotEmpty()
    val body = when {
        prompt.systemApps.size == 1 && !hasRegular ->
            stringResource(R.string.uninstall_system_privileged_required_body_single, prompt.systemApps.first().second)
        hasRegular ->
            stringResource(
                R.string.uninstall_system_privileged_required_body_batch,
                prompt.systemApps.size,
                prompt.userAppsAvailable.size,
            )
        else ->
            stringResource(R.string.uninstall_system_privileged_required_body_batch_only_system, prompt.systemApps.size)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.surfaceBorder(),
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.uninstall_system_privileged_required_title)) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            if (hasRegular) {
                TextButton(onClick = { onConfirm(null) }) {
                    Text(stringResource(R.string.uninstall_system_proceed_user_only))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = if (hasRegular) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        } else null,
    )
}
