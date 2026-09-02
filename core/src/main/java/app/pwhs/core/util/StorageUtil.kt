package app.pwhs.core.util

import android.os.Environment
import android.os.StatFs

data class StorageStats(
    val freeBytes: Long,
    val totalBytes: Long,
    val usedBytes: Long,
    val progress: Float
)

object StorageUtil {
    const val MIN_STORAGE_HEADROOM_BYTES = 50L * 1024 * 1024 // 50MB minimal operating headroom

    fun getStorageStats(): StorageStats {
        val stat = StatFs(Environment.getDataDirectory().path)
        val free = stat.availableBytes
        val total = stat.totalBytes
        val used = (total - free).coerceAtLeast(0L)
        val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
        return StorageStats(free, total, used, progress)
    }

    /**
     * Checks whether the device has enough free storage in /data to safely install the package.
     * Requires at least [MIN_STORAGE_HEADROOM_BYTES] (50MB) plus 2x the package size if known.
     */
    fun hasSufficientStorage(requiredBytes: Long = 0L): Boolean {
        val free = getStorageStats().freeBytes
        val needed = if (requiredBytes > 0L) {
            (requiredBytes * 2).coerceAtLeast(MIN_STORAGE_HEADROOM_BYTES)
        } else {
            MIN_STORAGE_HEADROOM_BYTES
        }
        return free >= needed
    }
}
