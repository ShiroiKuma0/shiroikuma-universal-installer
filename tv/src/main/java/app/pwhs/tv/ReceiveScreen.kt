package app.pwhs.tv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.pwhs.core.data.DownloadsApkScanner
import app.pwhs.core.domain.ApkFile
import app.pwhs.core.install.ApkInstaller
import app.pwhs.core.receiver.ReceivedApk
import app.pwhs.core.receiver.ReceiverStatus
import app.pwhs.core.receiver.TvReceiverState
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * "Install" destination with two sources: push from a phone (QR + LAN upload) and APKs
 * already on this TV (MediaStore scan). Both install via the core [ApkInstaller].
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

    val readPerm = if (Build.VERSION.SDK_INT <= 32) Manifest.permission.READ_EXTERNAL_STORAGE else null
    var hasStorage by remember {
        mutableStateOf(
            readPerm == null ||
                ContextCompat.checkSelfPermission(context, readPerm) == PackageManager.PERMISSION_GRANTED
        )
    }
    var downloads by remember { mutableStateOf<List<ApkFile>>(emptyList()) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasStorage = it }

    LaunchedEffect(received) { received?.let { pending = it; resultMessage = null } }
    LaunchedEffect(pending) { if (pending != null) runCatching { installFocus.requestFocus() } }
    LaunchedEffect(hasStorage) {
        if (hasStorage) downloads = withContext(Dispatchers.IO) { DownloadsApkScanner.scan(context) }
    }

    fun installUri(uri: Uri, isBundle: Boolean, label: String) {
        if (!context.packageManager.canRequestPackageInstalls()) { openUnknownSources(context); return }
        if (installing) return
        installing = true
        resultMessage = context.getString(R.string.tv_receive_installing, label)
        scope.launch {
            val r = withContext(Dispatchers.IO) { ApkInstaller(context).install(uri, isBundle) }
            installing = false
            resultMessage = when (r) {
                is ApkInstaller.Result.Success -> context.getString(R.string.tv_receive_installed_success, label)
                is ApkInstaller.Result.Failure -> context.getString(R.string.tv_receive_failed, r.message)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 48.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(stringResource(R.string.tv_receive_from_phone), style = MaterialTheme.typography.headlineMedium) }

        item {
            when (val s = status) {
                is ReceiverStatus.Running -> Row(verticalAlignment = Alignment.Top) {
                    QrCode(data = s.url, modifier = Modifier.size(220.dp))
                    Spacer(Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.tv_receive_step1), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.tv_receive_step2), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.tv_receive_step3), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text("http://${s.ip}:${s.port}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                ReceiverStatus.Stopped -> Text(stringResource(R.string.tv_receive_starting), style = MaterialTheme.typography.bodyMedium)
            }
        }

        resultMessage?.let { msg ->
            item { Text(msg, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
        }

        pending?.let { p ->
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tv_receive_received_title, p.fileName), style = MaterialTheme.typography.titleLarge)
                    Text(formatSize(p.sizeBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { installUri(Uri.fromFile(File(p.path)), p.fileName.isBundleName(), p.fileName) },
                            modifier = Modifier.focusRequester(installFocus),
                        ) { Text(if (installing) stringResource(R.string.tv_receive_installing_plain) else stringResource(R.string.tv_receive_install)) }
                        Button(onClick = { pending = null; resultMessage = null }) { Text(stringResource(R.string.tv_receive_dismiss)) }
                    }
                }
            }
        }

        // ── On this TV (MediaStore) ──────────────
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.tv_receive_on_tv), style = MaterialTheme.typography.titleLarge)
        }
        if (!hasStorage) {
            item {
                Card(onClick = { readPerm?.let { permLauncher.launch(it) } }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.tv_receive_allow_storage_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.tv_receive_allow_storage_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (downloads.isEmpty()) {
            item { Text(stringResource(R.string.tv_receive_no_apks), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(downloads, key = { it.uri }) { apk ->
                Card(
                    onClick = { installUri(Uri.parse(apk.uri), apk.isBundle, apk.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(apk.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(formatSize(apk.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun String.isBundleName(): Boolean =
    substringAfterLast('.', "").lowercase() in setOf("apks", "xapk", "apkm", "apk+")

private fun openUnknownSources(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
