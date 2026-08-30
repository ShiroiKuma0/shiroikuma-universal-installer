package app.pwhs.universalinstaller.presentation.manage.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.install.ApkExtractor
import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

object ManageExtractHelper {

    suspend fun scanVirusTotal(context: Context, app: InstalledApp) = withContext(Dispatchers.IO) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(app.packageName, 0)
            val baseApkFile = File(appInfo.sourceDir)
            if (baseApkFile.exists()) {
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                baseApkFile.inputStream().use { input ->
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        digest.update(buffer, 0, bytes)
                        bytes = input.read(buffer)
                    }
                }
                val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

                withContext(Dispatchers.Main) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.virustotal.com/gui/file/$sha256/detection"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to scan VT for ${app.packageName}")
        }
    }

    suspend fun performExtract(
        context: Context,
        packageName: String,
        cacheOutputDir: File?,
        useConfiguredPath: Boolean,
        onProgress: (Long, Long) -> Unit,
    ): ApkExtractor.Result {
        val prefs = context.dataStore.data.first()
        val customPathUri = prefs[PreferencesKeys.APK_EXTRACTOR_OUTPUT_PATH]
        val template = prefs[PreferencesKeys.APK_EXTRACTOR_FILENAME_TEMPLATE] ?: "{name}-{version}"
        val splitFormat = if (prefs[PreferencesKeys.APK_EXTRACTOR_SPLIT_FORMAT] == "xapk") {
            ApkExtractor.SplitFormat.XAPK
        } else {
            ApkExtractor.SplitFormat.APKS
        }
        return ApkExtractor.extract(
            context = context,
            packageName = packageName,
            outputDir = if (useConfiguredPath) {
                resolveConfiguredOutputDir(context, customPathUri)
            } else {
                cacheOutputDir?.let { DocumentFile.fromFile(it) }
            },
            filenameTemplate = template,
            splitFormat = splitFormat,
            onProgress = onProgress,
        )
    }

    fun resolveConfiguredOutputDir(context: Context, path: String?): DocumentFile? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("content://")) {
            DocumentFile.fromTreeUri(context, Uri.parse(path))
        } else {
            val dir = File(path).apply { if (!exists()) mkdirs() }
            if (dir.isDirectory) DocumentFile.fromFile(dir) else null
        }
    }
}
