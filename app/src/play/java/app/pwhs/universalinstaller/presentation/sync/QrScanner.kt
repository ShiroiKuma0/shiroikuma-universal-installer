package app.pwhs.universalinstaller.presentation.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * QR scanner launcher for `play` flavor using Google Code Scanner (ML Kit).
 *
 * Runs without requiring CAMERA permission in the manifest, supports auto-zoom, and provides
 * a smooth native Google Play Services scanning experience.
 */
@Composable
fun rememberQrScanner(
    onScanned: (String) -> Unit,
    onError: ((String) -> Unit)? = null,
): () -> Unit {
    val context = LocalContext.current
    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    return remember(scanner, onScanned, onError) {
        {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { onScanned(it) }
                }
                .addOnFailureListener { e ->
                    onError?.invoke(e.localizedMessage ?: "Scan failed")
                }
        }
    }
}
