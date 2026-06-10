package app.pwhs.tv

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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Renders [data] as a QR code. Ported from the mobile app's QrCode composable. */
@Composable
fun QrCode(
    data: String,
    modifier: Modifier = Modifier,
    fgColor: Int = Color.BLACK,
    bgColor: Int = Color.WHITE,
) {
    var bitmap by remember(data) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(data) {
        bitmap = if (data.isEmpty()) null
        else withContext(Dispatchers.Default) { generateQrCode(data, fgColor, bgColor) }
    }
    Box(modifier = modifier.aspectRatio(1f)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.tv_common_qr_code),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        }
    }
}

private fun generateQrCode(data: String, fgColor: Int, bgColor: Int): Bitmap? = runCatching {
    val size = 512
    val matrix = QRCodeWriter().encode(
        data, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1)
    )
    val pixels = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
        pixels[y * matrix.width + x] = if (matrix.get(x, y)) fgColor else bgColor
    }
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}.getOrNull()
