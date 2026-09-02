package app.pwhs.universalinstaller.presentation.composable

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QrCode(
    data: String,
    modifier: Modifier = Modifier,
    fgColor: Int = Color.BLACK,
    bgColor: Int = Color.WHITE
) {
    var bitmap by remember(data, fgColor, bgColor) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(data, fgColor, bgColor) {
        if (data.isNotEmpty()) {
            bitmap = withContext(Dispatchers.Default) {
                generateQrCode(data, fgColor, bgColor)
            }
        } else {
            bitmap = null
        }
    }
    
    Box(modifier = modifier.aspectRatio(1f)) {
        bitmap?.let { b ->
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None
            )
        }
    }
}

private fun generateQrCode(data: String, fgColor: Int, bgColor: Int): Bitmap? = runCatching {
    val size = 512
    val hints = mapOf(
        EncodeHintType.MARGIN to 2,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(
        data, BarcodeFormat.QR_CODE, size, size, hints
    )
    val pixels = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
        pixels[y * matrix.width + x] = if (matrix.get(x, y)) fgColor else bgColor
    }
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}.getOrNull()
