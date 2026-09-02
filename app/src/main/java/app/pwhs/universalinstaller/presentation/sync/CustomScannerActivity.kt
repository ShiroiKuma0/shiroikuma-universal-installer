package app.pwhs.universalinstaller.presentation.sync

import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings

class CustomScannerActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<DecoratedBarcodeView>(com.google.zxing.client.android.R.id.zxing_barcode_scanner)?.apply {
            cameraSettings = CameraSettings().apply {
                isAutoFocusEnabled = true
                isContinuousFocusEnabled = true
                isAutoTorchEnabled = false
            }
        }
    }
}
