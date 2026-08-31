package app.pwhs.updater.presentation.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.core.R
import app.pwhs.core.ui.theme.Spacing

@Composable
fun AddTrackedAppDialog(
    isAdding: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (url: String, includePrereleases: Boolean) -> Unit,
) {
    var urlText by remember { mutableStateOf("") }
    var includePrereleases by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.AddLink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(text = stringResource(R.string.updates_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.updates_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.L))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text(stringResource(R.string.updates_dialog_url_label)) },
                    placeholder = { Text("https://github.com/owner/repo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    isError = !errorMessage.isNullOrBlank(),
                    supportingText = {
                        if (!errorMessage.isNullOrBlank()) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(Spacing.M))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.updates_dialog_prerelease),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = includePrereleases,
                        onCheckedChange = { includePrereleases = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(urlText.trim(), includePrereleases) },
                enabled = urlText.isNotBlank() && !isAdding,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.updates_dialog_add_btn))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAdding) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
