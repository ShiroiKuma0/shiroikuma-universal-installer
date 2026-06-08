package app.pwhs.tv

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
 * "Install" destination: a QR a phone scans to open the upload page and push an APK. When a
 * file arrives we move focus to its Install button (so the remote lands on the action) and
 * install via the core [ApkInstaller].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ReceiveScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by TvReceiverState.status.collectAsState()
    val received by TvReceiverState.received.collectAsState(initial = null)

    var pending by remember { mutableStateOf<ReceivedApk?>(null) }
    var installing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val installFocus = remember { FocusRequester() }

    LaunchedEffect(received) {
        received?.let { pending = it; resultMessage = null }
    }
    // Move the remote's focus straight onto Install when a file shows up.
    LaunchedEffect(pending) {
        if (pending != null) runCatching { installFocus.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        Text("Install from phone", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.Top) {
            when (val s = status) {
                is ReceiverStatus.Running -> {
                    QrCode(data = s.url, modifier = Modifier.size(240.dp))
                    Spacer(Modifier.width(36.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("1.  Connect phone to the same Wi-Fi", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("2.  Scan the QR (or open the address below)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("3.  Pick an APK — it installs here", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "http://${s.ip}:${s.port}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = ".apk and bundles (.apks/.xapk/.apkm) supported",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ReceiverStatus.Stopped ->
                    Text("Starting receiver…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(28.dp))

        val p = pending
        if (p != null) {
            Text(
                text = "Received: ${p.fileName}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = formatSize(p.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            resultMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (installing) return@Button
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            openUnknownSources(context); return@Button
                        }
                        installing = true
                        resultMessage = "Installing…"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { ApkInstaller(context).install(File(p.path)) }
                            installing = false
                            when (result) {
                                is ApkInstaller.Result.Success -> { resultMessage = "Installed ✓"; pending = null }
                                is ApkInstaller.Result.Failure -> resultMessage = "Failed: ${result.message}"
                            }
                        }
                    },
                    modifier = Modifier.focusRequester(installFocus),
                ) { Text(if (installing) "Installing…" else "Install") }
                Button(onClick = { pending = null; resultMessage = null }) { Text("Dismiss") }
            }
        }
    }
}

private fun openUnknownSources(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
