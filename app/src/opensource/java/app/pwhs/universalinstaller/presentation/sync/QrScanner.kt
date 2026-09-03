package app.pwhs.universalinstaller.presentation.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.pwhs.universalinstaller.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * QR scanner launcher for `opensource` flavor using ZXing Android Embedded.
 *
 * Keeps the build 100% open-source for F-Droid without any Google Play Services dependencies.
 */
@Composable
fun rememberQrScanner(
    onScanned: (String) -> Unit,
    onError: ((String) -> Unit)? = null,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { onScanned(it) }
    }

    return remember(launcher, context) {
        {
            runCatching {
                launcher.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt(context.getString(R.string.tv_sync_prompt_scan))
                        .setBeepEnabled(false)
                        .setOrientationLocked(true)
                        .setCaptureActivity(CustomScannerActivity::class.java)
                )
            }.onFailure { e ->
                onError?.invoke(e.localizedMessage ?: context.getString(R.string.error_cannot_open_app))
            }
        }
    }
}
