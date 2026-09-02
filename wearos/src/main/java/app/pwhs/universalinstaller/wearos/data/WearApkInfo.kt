package app.pwhs.universalinstaller.wearos.data

/**
 * Lightweight APK metadata received from the paired phone and cached on the watch.
 */
data class WearApkInfo(
    val id: String,
    val fileName: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeBytes: Long,
    /** Absolute path to the cached .apk file in internal storage. */
    val cachedFilePath: String,
    val receivedAt: Long = System.currentTimeMillis(),
)
