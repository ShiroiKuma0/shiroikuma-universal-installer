package app.pwhs.tv

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pwhs.core.install.ApkInstaller
import app.pwhs.core.receiver.ReceivedApk
import app.pwhs.core.receiver.ReceiverStatus
import app.pwhs.core.receiver.TvReceiverState
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * "Install" destination: shows a QR a phone scans to open the upload page and push an APK.
 * When a file arrives it asks to confirm, then installs via the core [ApkInstaller].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ReceiveScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by TvReceiverState.status.collectAsState()
    val received by TvReceiverState.received.collectAsState(initial = null)

    // Pending confirm: the most recent arrival the user hasn't acted on.
    var pending by remember { mutableStateOf<ReceivedApk?>(null) }
    var installing by remember { mutableStateOf(false) }
    LaunchedEffect(received) { received?.let { pending = it } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        Text("Install from phone", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        when (val s = status) {
            is ReceiverStatus.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                QrCode(data = s.url, modifier = Modifier.size(220.dp))
                Spacer(Modifier.width(32.dp))
                Column {
                    Text("Scan with your phone", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Open the page and pick an APK to send here.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${s.ip}:${s.port}", style = MaterialTheme.typography.bodySmall)
                }
            }
            ReceiverStatus.Stopped ->
                Text("Starting receiver…", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))

        val p = pending
        if (p != null) {
            // Plain container (not a clickable Card) so D-pad focus goes straight to the
            // Install/Dismiss buttons instead of being trapped by a focusable card.
            Column(Modifier.fillMaxWidth()) {
                Text("Received: ${p.fileName}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = {
                            if (installing) return@Button
                            if (!canInstall(context)) {
                                openUnknownSources(context)
                                return@Button
                            }
                            installing = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    ApkInstaller(context).install(File(p.path))
                                }
                                installing = false
                                val msg = when (result) {
                                    is ApkInstaller.Result.Success -> "Installed ${p.fileName}"
                                    is ApkInstaller.Result.Failure -> "Failed: ${result.message}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (result is ApkInstaller.Result.Success) pending = null
                            }
                        }) { Text(if (installing) "Installing…" else "Install") }
                    Button(onClick = { pending = null }) { Text("Dismiss") }
                }
            }
        }
    }
}

private fun canInstall(context: android.content.Context): Boolean =
    context.packageManager.canRequestPackageInstalls()

private fun openUnknownSources(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
