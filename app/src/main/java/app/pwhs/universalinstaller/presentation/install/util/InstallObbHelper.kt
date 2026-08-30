package app.pwhs.universalinstaller.presentation.install.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.presentation.install.AttachedObb
import app.pwhs.universalinstaller.presentation.install.ObbCopyState
import app.pwhs.universalinstaller.presentation.install.ObbCopyWorker
import app.pwhs.universalinstaller.presentation.install.ObbEntry
import app.pwhs.universalinstaller.presentation.install.SafObbWriter
import app.pwhs.universalinstaller.presentation.install.ShizukuObbWriter
import app.pwhs.universalinstaller.util.extension.getDisplayName
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID

data class ObbCopyJob(
    val sourceUri: Uri?,
    val entries: List<ObbEntry>,
    val attached: List<AttachedObb>,
    val packageName: String,
    val appName: String,
)

sealed interface ObbStrategy {
    data object Direct : ObbStrategy
    data object Shizuku : ObbStrategy
    data class Saf(val treeUri: Uri) : ObbStrategy
    data object NeedSafGrant : ObbStrategy
}

object InstallObbHelper {

    suspend fun copyObbFiles(
        context: Context,
        sourceUri: Uri?,
        entries: List<ObbEntry>,
        attached: List<AttachedObb>,
        packageName: String,
        appName: String,
        onStateChanged: (ObbCopyState) -> Unit,
        onJobCreated: (ObbCopyJob) -> Unit,
        onObserveWork: (UUID, String, String) -> Unit,
    ) {
        val totalBytes = entries.sumOf { it.sizeBytes.coerceAtLeast(0L) } + attached.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        onStateChanged(ObbCopyState.Running(appName, packageName, 0L, totalBytes))

        val strategy = resolveObbStrategy(context, packageName)
        when (strategy) {
            is ObbStrategy.NeedSafGrant -> {
                onJobCreated(ObbCopyJob(sourceUri, entries, attached, packageName, appName))
                onStateChanged(ObbCopyState.NeedSafGrant(appName, packageName))
            }
            is ObbStrategy.Direct -> {
                val data = ObbCopyWorker.buildInputData(
                    ObbCopyWorker.STRATEGY_DIRECT, packageName, appName, sourceUri, entries, attached, null
                )
                val req = ObbCopyWorker.buildRequest(ObbCopyWorker.workNameFor(UUID.randomUUID()), data)
                WorkManager.getInstance(context).enqueue(req)
                onObserveWork(req.id, appName, packageName)
            }
            is ObbStrategy.Shizuku -> {
                val data = ObbCopyWorker.buildInputData(
                    ObbCopyWorker.STRATEGY_SHIZUKU, packageName, appName, sourceUri, entries, attached, null
                )
                val req = ObbCopyWorker.buildRequest(ObbCopyWorker.workNameFor(UUID.randomUUID()), data)
                WorkManager.getInstance(context).enqueue(req)
                onObserveWork(req.id, appName, packageName)
            }
            is ObbStrategy.Saf -> {
                val data = ObbCopyWorker.buildInputData(
                    ObbCopyWorker.STRATEGY_SAF, packageName, appName, sourceUri, entries, attached, strategy.treeUri
                )
                val req = ObbCopyWorker.buildRequest(ObbCopyWorker.workNameFor(UUID.randomUUID()), data)
                WorkManager.getInstance(context).enqueue(req)
                onObserveWork(req.id, appName, packageName)
            }
        }
    }

    suspend fun observeObbWorker(
        context: Context,
        workId: UUID,
        appName: String,
        packageName: String,
        onStateChanged: (ObbCopyState) -> Unit,
    ) {
        WorkManager.getInstance(context).getWorkInfoByIdFlow(workId).collect { info ->
            if (info == null) return@collect
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                    onStateChanged(
                        ObbCopyState.Running(
                            appName,
                            packageName,
                            info.progress.getLong(ObbCopyWorker.KEY_PROGRESS_BYTES, 0L),
                            info.progress.getLong(ObbCopyWorker.KEY_PROGRESS_TOTAL, 0L)
                        )
                    )
                }
                WorkInfo.State.SUCCEEDED -> {
                    onStateChanged(ObbCopyState.Done(appName, info.outputData.getInt(ObbCopyWorker.KEY_RESULT_FILE_COUNT, 0)))
                }
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                    onStateChanged(ObbCopyState.Error(appName, info.outputData.getString(ObbCopyWorker.KEY_RESULT_ERROR) ?: "Copy failed"))
                }
            }
        }
    }

    private suspend fun resolveObbStrategy(context: Context, packageName: String): ObbStrategy {
        val destDir = File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "obb/$packageName")
        val canDirectWrite = try {
            destDir.mkdirs()
            val probe = File(destDir, ".probe_${System.currentTimeMillis()}")
            val ok = probe.createNewFile()
            if (ok) probe.delete()
            ok
        } catch (_: Exception) {
            false
        }
        if (canDirectWrite) return ObbStrategy.Direct
        if (ShizukuObbWriter.isReady()) return ObbStrategy.Shizuku
        val savedTree = readObbTreeGrant(context, packageName)
        if (savedTree != null && treeUriStillGranted(context, savedTree)) return ObbStrategy.Saf(savedTree)
        return ObbStrategy.NeedSafGrant
    }

    private fun treeUriStillGranted(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission && it.isWritePermission
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readObbTreeGrant(context: Context, packageName: String): Uri? = try {
        val prefs = context.dataStore.data.first()
        val key = stringPreferencesKey("obb_tree_$packageName")
        prefs[key]?.let(Uri::parse)
    } catch (_: Exception) {
        null
    }

    suspend fun saveObbTreeGrant(context: Context, packageName: String, uri: Uri) {
        try {
            context.dataStore.edit { prefs ->
                val key = stringPreferencesKey("obb_tree_$packageName")
                prefs[key] = uri.toString()
            }
        } catch (_: Exception) {
            /* best-effort */
        }
    }

    fun attachObbFile(context: Context, uri: Uri, currentAttached: List<AttachedObb>): List<AttachedObb> {
        val displayName = context.contentResolver.getDisplayName(uri)
        if (!displayName.lowercase().endsWith(".obb")) return currentAttached
        if (currentAttached.any { it.uri == uri }) return currentAttached
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {
            /* best-effort */
        }
        val size = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0) c.getLong(idx) else 0L
                    } else 0L
                } ?: 0L
        } catch (_: Exception) {
            0L
        }
        return currentAttached + AttachedObb(uri, displayName, size)
    }
}
