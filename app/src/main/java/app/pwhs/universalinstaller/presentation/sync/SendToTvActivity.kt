package app.pwhs.universalinstaller.presentation.sync

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.base.BaseActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mobile client screen for sending APKs to Android TV.
 */
class SendToTvActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme { SendToTvScreen(onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendToTvScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scanned by rememberSaveable { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploadBytes by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var currentFileName by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var isConnectingManual by remember { mutableStateOf(false) }
    var manualError by remember { mutableStateOf<String?>(null) }
    var savedTvIp by rememberSaveable { mutableStateOf("") }
    val discoveredTvs by remember { TvDiscovery.discover(context) }.collectAsState(initial = emptyList())

    fun connectManual(rawIp: String) {
        val trimmed = rawIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val resolvedIp = if (trimmed.length in 4..6 && trimmed.all { it.isLetter() }) {
            app.pwhs.core.receiver.IpEncoder.decode(trimmed, app.pwhs.core.receiver.LanAddress.siteLocalIpv4()) ?: trimmed
        } else trimmed

        val host: String
        val port: Int
        if (resolvedIp.contains(":")) {
            host = resolvedIp.substringBefore(":")
            port = resolvedIp.substringAfter(":").substringBefore("/").toIntOrNull() ?: 8787
        } else {
            host = resolvedIp.substringBefore("/")
            port = 8787
        }
        if (host.isBlank()) {
            manualError = context.getString(R.string.tv_sync_error_invalid_input)
            return
        }

        isConnectingManual = true
        manualError = null

        scope.launch {
            val candidatePorts = if (resolvedIp.contains(":")) listOf(port) else listOf(8787, 8788, 8789, 8790, 8791, 8792)
            var connectedTarget: String? = null
            var connectedPort: Int = port

            withContext(Dispatchers.IO) {
                for (p in candidatePorts) {
                    val pingUrl = "http://$host:$p/ping"
                    val ok = runCatching {
                        val conn = URL(pingUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        conn.setRequestProperty("User-Agent", "${Build.MANUFACTURER} ${Build.MODEL} (Universal Installer App)")
                        val code = conn.responseCode
                        conn.disconnect()
                        code == 200
                    }.getOrDefault(false)

                    if (ok) {
                        connectedTarget = "http://$host:$p/"
                        connectedPort = p
                        break
                    }
                }
            }

            isConnectingManual = false
            if (connectedTarget != null) {
                savedTvIp = if (resolvedIp.contains(":")) resolvedIp else "$host:$connectedPort"
                scanned = connectedTarget
                errorMessage = null
                isSuccess = false
                showManualDialog = false
            } else {
                manualError = context.getString(R.string.tv_sync_error_connect_failed)
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        android.util.Log.d("SendToTv", "Scan result: contents=${result.contents}")
        result.contents?.let {
            scanned = it
            errorMessage = null
            isSuccess = false
        }
    }

    val apkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = scanned
        if (uri != null && target != null) {
            val name = queryDisplayName(context, uri)
            currentFileName = name
            uploading = true
            uploadProgress = 0
            uploadBytes = null
            errorMessage = null
            isSuccess = false

            scope.launch {
                val result = TvUploadClient.upload(context, target, uri, name) { copied, total, pct ->
                    uploadProgress = pct
                    uploadBytes = Pair(copied, total)
                }
                uploading = false
                when (result) {
                    is TvUploadClient.Result.Success -> {
                        isSuccess = true
                    }
                    is TvUploadClient.Result.Failure -> {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    fun disconnect() {
        val currentTarget = scanned
        if (currentTarget != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val parsed = Uri.parse(currentTarget)
                    val host = parsed.host
                    val port = parsed.port.takeIf { it > 0 } ?: 8787
                    val conn = URL("http://$host:$port/disconnect").openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.responseCode
                    conn.disconnect()
                }
            }
        }
        scanned = null
        uploading = false
        errorMessage = null
        isSuccess = false
        currentFileName = null
        uploadProgress = 0
        uploadBytes = null
    }

    // Ping TV periodically to keep connection active
    LaunchedEffect(scanned) {
        val target = scanned ?: return@LaunchedEffect
        val parsed = runCatching { Uri.parse(target) }.getOrNull() ?: return@LaunchedEffect
        val host = parsed.host
        if (host.isNullOrBlank()) return@LaunchedEffect
        val port = parsed.port.takeIf { it > 0 } ?: 8787
        val pingUrl = "http://$host:$port/ping"

        while (isActive) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val conn = URL(pingUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.setRequestProperty("User-Agent", "${Build.MANUFACTURER} ${Build.MODEL} (Universal Installer App)")
                    val code = conn.responseCode
                    if (code == 200) {
                        conn.inputStream.use { it.readBytes() }
                    }
                    conn.disconnect()
                }
            }
            delay(2500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.tv_sync_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                actions = {
                    if (scanned != null && !uploading) {
                        IconButton(onClick = { disconnect() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = stringResource(R.string.tv_sync_btn_disconnect),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                TvSyncHeroSection(isConnected = scanned != null)
            }

            item {
                AnimatedContent(
                    targetState = when {
                        scanned == null -> TvScreenMode.Scan
                        uploading -> TvScreenMode.Uploading
                        isSuccess -> TvScreenMode.Success
                        errorMessage != null -> TvScreenMode.Error
                        else -> TvScreenMode.Connected
                    },
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 50)) +
                                slideInVertically(animationSpec = tween(250)) { it / 8 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(150)) +
                                        slideOutVertically(animationSpec = tween(150)) { -it / 8 }
                            )
                    },
                    label = "TvSyncModeTransition"
                ) { mode ->
                    when (mode) {
                        TvScreenMode.Scan -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TvDiscoveredDevicesSection(
                                    devices = discoveredTvs,
                                    isSearching = true,
                                    onSelectTv = { tv ->
                                        scanned = "http://${tv.host}:${tv.port}/"
                                        errorMessage = null
                                        isSuccess = false
                                    }
                                )

                                OutlinedButton(
                                    onClick = {
                                        manualError = null
                                        showManualDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        stringResource(R.string.tv_sync_btn_enter_code),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        scanLauncher.launch(
                                            ScanOptions()
                                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                .setPrompt(context.getString(R.string.tv_sync_prompt_scan))
                                                .setBeepEnabled(false)
                                                .setOrientationLocked(true)
                                                .setCaptureActivity(CustomScannerActivity::class.java)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.tv_sync_btn_scan), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        TvScreenMode.Connected -> {
                            val host = scanned?.let { Uri.parse(it).host } ?: "Android TV"
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TvConnectedDeviceCard(host = host, onDisconnect = { disconnect() })
                                TvFileDropzoneCard(
                                    onPickApk = { apkLauncher.launch(arrayOf("*/*")) }
                                )
                                OutlinedButton(
                                    onClick = {
                                        scanLauncher.launch(
                                            ScanOptions()
                                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                .setPrompt(context.getString(R.string.tv_sync_prompt_scan))
                                                .setBeepEnabled(false)
                                                .setOrientationLocked(true)
                                                .setCaptureActivity(CustomScannerActivity::class.java)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.tv_sync_btn_rescan))
                                }
                            }
                        }

                        TvScreenMode.Uploading -> {
                            TvUploadingProgressCard(
                                fileName = currentFileName ?: "application.apk",
                                progress = uploadProgress,
                                bytes = uploadBytes
                            )
                        }

                        TvScreenMode.Success -> {
                            TvUploadSuccessCard(
                                fileName = currentFileName ?: "application.apk",
                                onSendAnother = {
                                    isSuccess = false
                                    errorMessage = null
                                    currentFileName = null
                                    apkLauncher.launch(arrayOf("*/*"))
                                },
                                onDone = { onBack() }
                            )
                        }

                        TvScreenMode.Error -> {
                            val host = scanned?.let { Uri.parse(it).host } ?: "Android TV"
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TvConnectedDeviceCard(host = host, onDisconnect = { disconnect() })
                                TvErrorCard(
                                    error = errorMessage ?: "Upload failed",
                                    onRetry = {
                                        errorMessage = null
                                        apkLauncher.launch(arrayOf("*/*"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualConnectDialog(
            initialIp = savedTvIp,
            isConnecting = isConnectingManual,
            errorMessage = manualError,
            onDismiss = {
                if (!isConnectingManual) {
                    showManualDialog = false
                    manualError = null
                }
            },
            onConnect = { ip ->
                connectManual(ip)
            }
        )
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0) ?: "app.apk"
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "app.apk"
}
