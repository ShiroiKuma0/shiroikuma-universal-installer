package app.pwhs.universalinstaller.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.BuildConfig
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.ui.UiConfigBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The sister-app **state-export automation contract**, implemented by this app for itself — the
 * same wire shape every 白い熊 app exposes so a 保存復元 task can back them all up headlessly
 * (reference implementations: renrakusaki's BackupContactsReceiver, the EMUI-proven round-trip,
 * and 自由作業盤's StateExportReceiver).
 *
 * - [ACTION_EXPORT_STATE]: run the full category-ZIP export ([UiConfigBackup]) without UI.
 *   Extras (all String): `token` (required — [AutomationAuth]), `path` (optional absolute
 *   directory, wins over the configured SAF directory), `items` (optional comma list of
 *   category ids, sub-option ids included; absent/empty = all), `progress_action` (optional —
 *   see below), plus the reply trio `reply_action` / `reply_package` / `reply_id`.
 * - [ACTION_LIST_CATEGORIES]: token-gated category enumeration for the caller's item picker,
 *   `id<TAB>label` per line with an optional third `parent-id` field for sub-options.
 *
 * Reply: a FRESH broadcast to `reply_package` with action `reply_action`, extras `reply_id`
 * (echoed verbatim) + `result` = `OK:<path>|<bytes>|<human size>|<n> categories` (EXPORT_STATE),
 * `OK:` + the `id<TAB>label` lines (LIST_CATEGORIES), or `ERROR:<reason>`. Exactly one terminal
 * reply, single-fire guarded. NO binders and NO ordered-result reliance — EMUI severs both
 * between third-party apps (verified 2026-07-23); the plain reply broadcast is the only working
 * channel. [Intent.FLAG_INCLUDE_STOPPED_PACKAGES] so a stopped caller still hears it.
 *
 * Progress: while exporting, plain broadcasts to `reply_package` with action `progress_action`,
 * extras `reply_id`, `app` (display label), `text` (numbers-first, e.g. `区分 3/9 — Engines`),
 * and structured `current`/`total` (long) + `unit` (String), throttled to one per 500 ms with
 * the completing one always sent.
 */
class StateExportReceiver : BroadcastReceiver() {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val action = intent.action ?: return
        val token = intent.getStringExtra(EXTRA_TOKEN)
        val replyAction = intent.getStringExtra(EXTRA_REPLY_ACTION)?.trim().orEmpty()
        val replyPackage = intent.getStringExtra(EXTRA_REPLY_PACKAGE)?.trim().orEmpty()
        val replyId = intent.getStringExtra(EXTRA_REPLY_ID)?.trim().orEmpty()
        val progressAction = intent.getStringExtra(EXTRA_PROGRESS_ACTION)?.trim().orEmpty()
        val pathOverride = intent.getStringExtra(EXTRA_PATH)?.trim().orEmpty()
        val items = intent.getStringExtra(EXTRA_ITEMS)?.trim().orEmpty()

        val replied = AtomicBoolean(false)
        fun reply(result: String) {
            // Logged on every build (Timber is debug-only): the contract is tested against the
            // signed APK with `adb logcat -s StateExport`. The token is never logged.
            Log.i(TAG, "$action → $result")
            if (replyAction.isEmpty() || replyPackage.isEmpty()) return
            if (!replied.compareAndSet(false, true)) return
            app.sendBroadcast(
                Intent(replyAction).apply {
                    setPackage(replyPackage)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    putExtra(EXTRA_REPLY_ID, replyId)
                    putExtra(EXTRA_RESULT, result)
                },
            )
        }

        // Gate first, and report "disabled" and "bad token" distinctly (renrakusaki convention).
        if (!AutomationAuth.enabled(app)) {
            reply("ERROR:automation disabled")
            return
        }
        if (!AutomationAuth.isTokenValid(app, token)) {
            reply("ERROR:bad token")
            return
        }

        when (action) {
            ACTION_LIST_CATEGORIES -> reply(
                "OK:" + UiConfigBackup.items(app).joinToString("\n") { item ->
                    listOfNotNull(item.id, item.label, item.parent).joinToString("\t")
                },
            )

            ACTION_EXPORT_STATE -> {
                val selection = if (items.isEmpty()) {
                    UiConfigBackup.Selection.all()
                } else {
                    val ids = items.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    UiConfigBackup.selectionOf(ids)
                        ?: run {
                            reply("ERROR:unknown category in items: $items")
                            return
                        }
                }
                if (selection.isEmpty) {
                    reply("ERROR:unknown category in items: $items")
                    return
                }

                val appLabel = app.packageManager.getApplicationLabel(app.applicationInfo).toString()
                val fileName = UiConfigBackup.exportFileName()
                var lastProgressMs = 0L
                fun progress(done: Int, total: Int, label: String) {
                    if (progressAction.isEmpty() || replyPackage.isEmpty()) return
                    val now = SystemClock.elapsedRealtime()
                    if (done < total && now - lastProgressMs < PROGRESS_MIN_INTERVAL_MS) return
                    lastProgressMs = now
                    app.sendBroadcast(
                        Intent(progressAction).apply {
                            setPackage(replyPackage)
                            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                            putExtra(EXTRA_REPLY_ID, replyId)
                            putExtra(EXTRA_PROGRESS_APP, appLabel)
                            putExtra(EXTRA_PROGRESS_TEXT, "$PROGRESS_UNIT $done/$total — $label")
                            putExtra(EXTRA_PROGRESS_CURRENT, done.toLong())
                            putExtra(EXTRA_PROGRESS_TOTAL, total.toLong())
                            putExtra(EXTRA_PROGRESS_UNIT, PROGRESS_UNIT)
                        },
                    )
                }

                // The export reads DataStore and writes ZIP entries — go async, finish from IO.
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val bytes: Long
                        val shownPath: String
                        val absolute = pathOverride.isNotEmpty() && canWriteAbsolutePaths()
                        if (absolute) {
                            // Absolute-directory override (MANAGE_EXTERNAL_STORAGE) — the normal
                            // automation route; the caller's %BR_Dir wins over our own directory.
                            val dir = File(pathOverride)
                            dir.mkdirs()
                            require(dir.isDirectory) { "not a directory: $pathOverride" }
                            val file = File(dir, fileName)
                            file.outputStream().use { out ->
                                UiConfigBackup.export(app, selection, out, ::progress)
                            }
                            bytes = file.length()
                            shownPath = file.absolutePath
                        } else {
                            val dir = configuredExportDir(app)
                                ?: error(if (pathOverride.isEmpty()) "no-directory" else "no-storage-access")
                            val doc = dir.createFile("application/zip", fileName)
                                ?: error("cannot create $fileName in the export directory")
                            app.contentResolver.openOutputStream(doc.uri)?.use { out ->
                                UiConfigBackup.export(app, selection, out, ::progress)
                            } ?: error("cannot open $fileName for writing")
                            bytes = doc.length()
                            shownPath = "${dir.name ?: dir.uri.lastPathSegment}/${doc.name ?: fileName}"
                        }
                        reply(
                            "OK:$shownPath|$bytes|${humanSize(bytes)}|${selection.categoryCount} categories",
                        )
                    } catch (e: Exception) {
                        reply("ERROR:${e.message ?: e.javaClass.simpleName}")
                    } finally {
                        pending.finish()
                    }
                }
            }

            else -> reply("ERROR:unknown action: $action")
        }
    }

    /** All-Files-Access, needed to write the caller's absolute `path` with plain [File] I/O. */
    private fun canWriteAbsolutePaths(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    /** The SAF directory picked in the Export/Import panel, or null when unset/unreachable. */
    private suspend fun configuredExportDir(context: Context): DocumentFile? {
        val uri = context.dataStore.data.first()[PreferencesKeys.UI_EXPORT_DIR]
            ?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri)) }
            .getOrNull()?.takeIf { it.isDirectory }
    }

    companion object {
        val ACTION_EXPORT_STATE = "${BuildConfig.APPLICATION_ID}.action.EXPORT_STATE"
        val ACTION_LIST_CATEGORIES = "${BuildConfig.APPLICATION_ID}.action.LIST_CATEGORIES"

        // Contract extras — deliberately bare names, shared verbatim by every sister app.
        const val EXTRA_TOKEN = "token"
        const val EXTRA_PATH = "path"
        const val EXTRA_ITEMS = "items"
        const val EXTRA_PROGRESS_ACTION = "progress_action"
        const val EXTRA_REPLY_ACTION = "reply_action"
        const val EXTRA_REPLY_PACKAGE = "reply_package"
        const val EXTRA_REPLY_ID = "reply_id"
        const val EXTRA_RESULT = "result"
        const val EXTRA_PROGRESS_APP = "app"
        const val EXTRA_PROGRESS_TEXT = "text"
        const val EXTRA_PROGRESS_CURRENT = "current"
        const val EXTRA_PROGRESS_TOTAL = "total"
        const val EXTRA_PROGRESS_UNIT = "unit"

        /** logcat tag — `adb logcat -s StateExport` while testing the contract. */
        private const val TAG = "StateExport"
        private const val PROGRESS_MIN_INTERVAL_MS = 500L
        private const val PROGRESS_UNIT = "区分"

        fun humanSize(bytes: Long): String = when {
            bytes >= 1L shl 30 -> "%.2f GB".format(bytes / (1L shl 30).toDouble())
            bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1L shl 20).toDouble())
            bytes >= 1L shl 10 -> "%.1f KB".format(bytes / (1L shl 10).toDouble())
            else -> "$bytes B"
        }
    }
}
