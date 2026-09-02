package app.pwhs.universalinstaller.wearos.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * In-memory + disk repository for APK files received from the paired phone.
 *
 * In a production version this would be backed by DataStore/Room, but for the
 * initial release an in-memory list kept alive by the Application scope is
 * sufficient — the watch session is typically short.
 */
class WearApkRepository(private val context: Context) {

    private val cacheDir: File
        get() = File(context.filesDir, "wear_apk_cache").also { it.mkdirs() }

    private val _apks = MutableStateFlow<List<WearApkInfo>>(emptyList())
    val apks: StateFlow<List<WearApkInfo>> = _apks.asStateFlow()

    /** Called by WearReceiverService once a full APK has been written to disk. */
    suspend fun addApk(apkFile: File): WearApkInfo? = withContext(Dispatchers.IO) {
        val info = extractApkInfo(apkFile) ?: return@withContext null
        _apks.value = _apks.value + info
        info
    }

    fun getById(id: String): WearApkInfo? = _apks.value.find { it.id == id }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        val entry = getById(id) ?: return@withContext
        File(entry.cachedFilePath).delete()
        _apks.value = _apks.value.filter { it.id != id }
    }

    /** Create a temp file in the cache dir to write incoming bytes into. */
    fun createTempApkFile(fileName: String): File {
        val safe = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(cacheDir, "${UUID.randomUUID()}_$safe")
    }

    // ── private helpers ───────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun extractApkInfo(apkFile: File): WearApkInfo? = runCatching {
        val pm = context.packageManager
        val pkgInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
        }
        pkgInfo ?: return@runCatching null
        pkgInfo.applicationInfo?.sourceDir = apkFile.absolutePath
        pkgInfo.applicationInfo?.publicSourceDir = apkFile.absolutePath

        val appName = pkgInfo.applicationInfo?.let {
            pm.getApplicationLabel(it).toString()
        } ?: apkFile.nameWithoutExtension

        WearApkInfo(
            id = UUID.randomUUID().toString(),
            fileName = apkFile.name,
            appName = appName,
            packageName = pkgInfo.packageName,
            versionName = pkgInfo.versionName ?: "?",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                pkgInfo.longVersionCode else pkgInfo.versionCode.toLong(),
            sizeBytes = apkFile.length(),
            cachedFilePath = apkFile.absolutePath,
        )
    }.getOrNull()
}
