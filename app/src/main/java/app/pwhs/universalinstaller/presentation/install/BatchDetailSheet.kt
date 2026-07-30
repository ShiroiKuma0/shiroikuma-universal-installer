package app.pwhs.universalinstaller.presentation.install

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.pwhs.universalinstaller.domain.model.InstallerProfile
import app.pwhs.universalinstaller.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BatchDetailSheet(
    state: BatchInstallState.Ready,
    detailUri: Uri,
    onDismiss: () -> Unit,
    onSave: (Uri, List<Uri>) -> Unit,
    profiles: List<InstallerProfile> = emptyList(),
    appProfileMapping: Map<String, String> = emptyMap(),
    allUsers: Boolean = false,
    selectedUserId: Int? = null,
    onProfileSelected: (InstallerProfile?) -> Unit = {},
    onMappingChanged: (String, String?) -> Unit = { _, _ -> },
    onToggleAllUsers: (Boolean) -> Unit = {},
    onSelectUserId: (Int?) -> Unit = {},
) {
    val entry = state.entries.find { it.uri == detailUri } ?: return
    
    // We maintain a local set of active split URIs for this sheet.
    var currentSplits by remember(entry.splitUris) { mutableStateOf(entry.splitUris.toSet()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        ApkInfoContent(
            apkInfo = entry.apkInfo.copy(
                splitEntries = entry.apkInfo.splitEntries.map { split ->
                    split.copy(selected = split.uri in currentSplits)
                }
            ),
            onInstall = { onSave(detailUri, currentSplits.toList()) },
            onCancel = onDismiss,
            confirmText = "Done",
            cancelText = stringResource(R.string.cancel),
            onToggleSplit = { splitIndex ->
                val splitUri = entry.apkInfo.splitEntries[splitIndex].uri
                currentSplits = if (splitUri in currentSplits) {
                    currentSplits - splitUri
                } else {
                    currentSplits + splitUri
                }
            },
            profiles = profiles,
            appProfileMapping = appProfileMapping,
            allUsers = allUsers,
            selectedUserId = selectedUserId,
            onProfileSelected = onProfileSelected,
            onMappingChanged = onMappingChanged,
            onToggleAllUsers = onToggleAllUsers,
            onSelectUserId = onSelectUserId,
            startCompact = false,
        )
    }
}
