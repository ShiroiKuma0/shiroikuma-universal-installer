@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package app.pwhs.universalinstaller.presentation.setting.sections

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.presentation.setting.SyncOptions
import app.pwhs.universalinstaller.presentation.setting.components.SwitchPreference
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery

internal fun LazyListScope.SyncSection(
    q: String,
    syncLabels: List<String>,
    context: Context,
    syncOptions: SyncOptions,
    onSyncServerPortChanged: (String) -> Unit,
    onSyncRequirePinChanged: (Boolean) -> Unit,
    onSyncPinCodeChanged: (String) -> Unit
) {
    if (matchesQuery(q, syncLabels)) item {
        SettingsSection(title = stringResource(R.string.setting_section_sync_short), icon = Icons.Rounded.WifiTethering) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_sync_control_panel)) },
                leadingContent = { Icon(Icons.Rounded.WifiTethering, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable {
                    context.startActivity(android.content.Intent(context, app.pwhs.universalinstaller.presentation.sync.SyncActivity::class.java))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = syncOptions.serverPort,
                    onValueChange = onSyncServerPortChanged,
                    label = { Text(stringResource(R.string.sync_port)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true
                )
                SwitchPreference(
                    title = stringResource(R.string.sync_require_pin),
                    checked = syncOptions.requirePin,
                    onCheckedChange = onSyncRequirePinChanged
                )
                if (syncOptions.requirePin) {
                    OutlinedTextField(
                        value = syncOptions.pinCode,
                        onValueChange = onSyncPinCodeChanged,
                        label = { Text(stringResource(R.string.sync_pin_code)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        singleLine = true
                    )
                }
            }
        }
    }
}
