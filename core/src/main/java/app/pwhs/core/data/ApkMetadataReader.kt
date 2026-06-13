package app.pwhs.core.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import app.pwhs.core.domain.PackageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.ZippedApkSplits
import java.io.File

/**
 * Utility to extract app metadata (icon, label, version) from an APK or bundle URI.
 * Uses Ackpine to handle split APKs by identifying and reading the base APK.
 */
class ApkMetadataReader(private val context: Context) {

    suspend fun readMetadata(uri: Uri, isBundle: Boolean): PackageMetadata? = withContext(Dispatchers.IO) {
        if (isBundle) {
            readBundleMetadata(uri)
        } else {
            readSingleApkMetadata(uri)
        }
    }

    private fun readBundleMetadata(uri: Uri): PackageMetadata? {
        val apks = ZippedApkSplits.getApksForUri(uri, context)
        var baseApk: Apk.Base? = null
        try {
            for (apk in apks) {
                if (apk is Apk.Base) {
                    baseApk = apk
                    break
                }
            }
        } finally {
            apks.close()
        }

        return baseApk?.let { readSingleApkMetadata(it.uri, isBundle = true) }
    }

    private fun readSingleApkMetadata(uri: Uri, isBundle: Boolean = false): PackageMetadata? {
        val tempFile = File(context.cacheDir, "temp_metadata_${System.currentTimeMillis()}.apk")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val pm = context.packageManager
            val pi = pm.getPackageArchiveInfo(tempFile.absolutePath, 0) ?: return null
            val appInfo = pi.applicationInfo ?: return null
            
            // Important: set source paths so loadIcon/loadLabel work correctly
            appInfo.sourceDir = tempFile.absolutePath
            appInfo.publicSourceDir = tempFile.absolutePath

            return PackageMetadata(
                packageName = pi.packageName,
                appName = appInfo.loadLabel(pm).toString(),
                versionName = pi.versionName ?: "",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong(),
                icon = appInfo.loadIcon(pm).toBitmap(512, 512),
                isBundle = isBundle,
                minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 0,
                targetSdk = appInfo.targetSdkVersion
            )
        } catch (e: Exception) {
            return null
        } finally {
            tempFile.delete()
        }
    }
}
