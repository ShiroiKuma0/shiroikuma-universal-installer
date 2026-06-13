package app.pwhs.tv

import android.content.Context
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Human-readable byte size. */
fun formatSize(context: Context, bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> context.getString(R.string.tv_common_size_gb, gb)
        mb >= 1.0 -> context.getString(R.string.tv_common_size_mb, mb)
        kb >= 1.0 -> context.getString(R.string.tv_common_size_kb, kb)
        else -> context.getString(R.string.tv_common_size_bytes, bytes)
    }
}

/** Loads an app's launcher icon off the main thread; null until ready (or on failure). */
@Composable
fun rememberAppIcon(packageName: String, sizePx: Int = 256): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = packageName, key2 = sizePx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                // Try to get XXXHDPI icon if possible
                val drawable = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                    pm.getResourcesForApplication(appInfo).getDrawableForDensity(appInfo.icon, DisplayMetrics.DENSITY_XXXHIGH, null)
                } else {
                    pm.getApplicationIcon(appInfo)
                }
                drawable?.toBitmap(sizePx, sizePx)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return icon
}
