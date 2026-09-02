package app.pwhs.universalinstaller.presentation.install

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R

@Composable
fun WatchSendDialog(
    state: WatchSendState,
    onDismiss: () -> Unit,
) {
    when (state) {
        WatchSendState.Idle -> Unit

        WatchSendState.CheckingWatch -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.watch_send_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            text = stringResource(R.string.watch_send_connecting),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        is WatchSendState.Sending -> {
            val progressPercent = (state.progress * 100).toInt()
            AlertDialog(
                onDismissRequest = {}, // do not dismiss during active transmission
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.watch_send_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.watch_send_sending, progressPercent),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {},
            )
        }

        WatchSendState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.watch_send_success_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.watch_send_success_msg),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }

        WatchSendState.NoWatch -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.watch_send_no_watch_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.watch_send_no_watch_msg),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }

        is WatchSendState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.watch_send_error_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }
    }
}
