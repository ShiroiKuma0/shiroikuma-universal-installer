package app.pwhs.universalinstaller.presentation.install.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.presentation.install.AttachedObb
import app.pwhs.universalinstaller.presentation.install.ObbEntry
import app.pwhs.universalinstaller.presentation.install.PendingInstallStore
import app.pwhs.universalinstaller.presentation.install.dialog.isDowngrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object PendingInstallStager {

    private const val PENDING_INSTALL_DIR = "pending_install"
    private const val PENDING_INSTALL_TTL_MS = 60 * 60 * 1000L

    suspend fun stashPendingInstall(
        context: Context,
        apkInfo: ApkInfo?,
        pendingFileName: String?,
        pendingOriginalUri: Uri?,
        pendingApkUris: List<Uri>?,
        pendingObbEntries: List<ObbEntry>,
        attachedObbFiles: List<AttachedObb>,
        cachedIconPath: String?,
    ): PendingInstallStore.Entry? {
        apkInfo ?: return null
        val splits = apkInfo.splitEntries
        val fileName = pendingFileName ?: return null

        val sourceUris = if (splits.isNotEmpty()) splits.map { it.uri } else pendingApkUris
        if (sourceUris.isNullOrEmpty()) return null
        val stagedUris = copyForLaterInstall(context, sourceUris) ?: return null

        val stagedInfo = if (splits.isNotEmpty()) {
            apkInfo.copy(
                splitEntries = splits.zip(stagedUris).map { (split, staged) -> split.copy(uri = staged) },
            )
        } else {
            apkInfo
        }

        return PendingInstallStore.Entry(
            id = java.util.UUID.randomUUID().toString(),
            apkInfo = stagedInfo,
            fileName = fileName,
            packageName = apkInfo.packageName,
            appName = apkInfo.appName,
            iconPath = cachedIconPath,
            originalUri = pendingOriginalUri,
            apkUris = stagedUris,
            obbEntries = pendingObbEntries,
            attachedObbs = attachedObbFiles,
            isDowngrade = isDowngrade(apkInfo),
        )
    }

    private suspend fun copyForLaterInstall(context: Context, uris: List<Uri>): List<Uri>? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, PENDING_INSTALL_DIR).apply { mkdirs() }
        pruneOldStagedFiles(dir)
        val out = mutableListOf<Uri>()
        for ((idx, srcUri) in uris.withIndex()) {
            val dst = File(dir, "staged_${System.currentTimeMillis()}_$idx.apk")
            try {
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    dst.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    dst,
                )
                out += fileUri
            } catch (e: Exception) {
                Timber.e(e, "Failed to copy APK for later install: $srcUri")
                dst.delete()
                return@withContext null
            }
        }
        out
    }

    private fun pruneOldStagedFiles(dir: File) {
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > PENDING_INSTALL_TTL_MS) {
                file.delete()
            }
        }
    }
}
