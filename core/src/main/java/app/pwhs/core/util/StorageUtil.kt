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
    fun getStorageStats(): StorageStats {
        val stat = StatFs(Environment.getDataDirectory().path)
        val free = stat.availableBytes
        val total = stat.totalBytes
        val used = (total - free).coerceAtLeast(0L)
        val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
        return StorageStats(free, total, used, progress)
    }
}
