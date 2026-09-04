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
import app.pwhs.universalinstaller.presentation.setting.ui.ExportCancelledException
import app.pwhs.universalinstaller.presentation.setting.ui.UiConfigBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The sister-app **state-export automation contract** (v2 §1), implemented by this app for itself —
 * the same wire shape every 白い熊 app exposes so a 保存復元 batch can back them all up headlessly.
 *
 * - [ACTION_EXPORT_STATE]: run the full category-ZIP export ([UiConfigBackup]) without UI.
 *   Extras (all String): `token` (**optional** — see [AutomationAuth], checked only when this app
 *   asks for one, and ignored rather than refused when it does not), `path` (optional absolute
 *   directory, wins over the configured SAF directory), `items` (optional comma list of category
 *   ids, sub-option ids included; absent/empty = the default set, which here is everything),
 *   `progress_action` (optional), plus the reply trio `reply_action` / `reply_package` /
 *   `reply_id`.
 * - [ACTION_LIST_CATEGORIES]: category enumeration for the caller's item picker, `id<TAB>label`
 *   per line with an optional third `parent-id` field for sub-options.
 * - [ACTION_CANCEL_EXPORT]: stop the running export. Fire-and-forget — it sends **no reply of its
 *   own**; the one terminal reply belongs to the export it stopped, which answers `ERROR:cancelled`.
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
 *
 * ## Why this half is unauthenticated by default in v2
 *
 * The master switch ships ON and the token is opt-in, so out of the box any app on the phone may
 * fire these actions. That is deliberate in the contract: the receiver only ever *writes where it
 * was told to* and reports what it did. Everything that moves data through a caller-supplied
 * descriptor — and `import`, which never gets a broadcast action at all — lives behind
 * [AutomationProvider], which knows who is calling.
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

        // CANCEL_EXPORT answers nobody — not even a refusal. It must be safe to send at any time:
        // when nothing is running, or after the export finished, it is a silent no-op.
        if (action == ACTION_CANCEL_EXPORT) {
            if (AutomationAuth.refuse(app, token) != null) return
            Log.i(TAG, "$action → signalled ${replyId.ifEmpty { "(running export)" }}")
            StateExportJob.cancel(replyId)
            return
        }

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

        // The whole gate in one call, so "disabled" and "bad token" can never drift apart.
        AutomationAuth.refuse(app, token)?.let {
            reply(it)
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

                // §1 forbids two at once, which is also what makes a CANCEL_EXPORT without a
                // reply_id unambiguous. Process-local and released in the `finally` below —
                // persisting this is how an app wedges itself for good after one crash.
                // Named `job`, not `run`: a local `val run` shadows the stdlib `run` used two
                // lines below, and the resulting error reads like a Kotlin bug rather than a
                // naming clash.
                val job = StateExportJob.begin(replyId) ?: run {
                    reply("ERROR:export already running")
                    return
                }

                val appLabel = app.packageManager.getApplicationLabel(app.applicationInfo).toString()
                val fileName = UiConfigBackup.exportFileName()
                var lastProgressMs = 0L
                fun progress(done: Int, total: Int, label: String, id: String) {
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
                            // §3: WHICH row is running. Without it the panel falls back to reading
                            // `current` as a position, which only works when `total` happens to
                            // equal the number of rows it is showing.
                            putExtra(EXTRA_PROGRESS_ITEM, id)
                            putExtra(EXTRA_PROGRESS_TEXT, "$PROGRESS_UNIT $done/$total — $label")
                            putExtra(EXTRA_PROGRESS_CURRENT, done.toLong())
                            putExtra(EXTRA_PROGRESS_TOTAL, total.toLong())
                            putExtra(EXTRA_PROGRESS_UNIT, PROGRESS_UNIT)
                        },
                    )
                }

                // The export reads DataStore and writes ZIP entries — go async, finish from IO.
                // goAsync() is honest here only because this export is a handful of small JSON
                // entries plus the imported fonts: it finishes in well under the broadcast window.
                // Anything that could walk thousands of files would need the §1 foreground service.
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val absolute = pathOverride.isNotEmpty() && canWriteAbsolutePaths()
                        // Each branch YIELDS (bytes, path) rather than assigning two outer vals:
                        // a `val` written inside a try/finally makes definite assignment a question
                        // the reader has to answer too.
                        val (bytes, shownPath) = if (absolute) {
                            // Absolute-directory override (MANAGE_EXTERNAL_STORAGE) — the normal
                            // automation route; the caller's %BR_Dir wins over our own directory.
                            val dir = File(pathOverride)
                            dir.mkdirs()
                            require(dir.isDirectory) { "not a directory: $pathOverride" }
                            val part = File(dir, fileName + PART_SUFFIX)
                            val finished = File(dir, fileName)
                            try {
                                part.outputStream().use { out ->
                                    UiConfigBackup.export(app, selection, out, ::progress) {
                                        job.cancelled
                                    }
                                }
                                if (job.cancelled) throw ExportCancelledException()
                                if (!part.renameTo(finished)) error("cannot finalise $fileName")
                                finished.length() to finished.absolutePath
                            } finally {
                                // Covers failure, cancellation AND success: after a rename there is
                                // no part-file left to delete, so this is a no-op on the good path.
                                part.delete()
                            }
                        } else {
                            val dir = configuredExportDir(app)
                                ?: error(if (pathOverride.isEmpty()) "no-directory" else "no-storage-access")
                            // Written under a name that is deliberately NOT `.zip`: the panel's
                            // "latest export" scan matches the prefix and a `.zip`/`.json` suffix,
                            // so a half-written archive must never wear one. The octet-stream mime
                            // keeps the provider from helpfully appending `.zip` for us.
                            val part = dir.createFile(PART_MIME, fileName + PART_SUFFIX)
                                ?: error("cannot create $fileName in the export directory")
                            try {
                                app.contentResolver.openOutputStream(part.uri)?.use { out ->
                                    UiConfigBackup.export(app, selection, out, ::progress) {
                                        job.cancelled
                                    }
                                } ?: error("cannot open $fileName for writing")
                                if (job.cancelled) throw ExportCancelledException()
                                if (!part.renameTo(fileName)) error("cannot finalise $fileName")
                                part.length() to
                                    "${dir.name ?: dir.uri.lastPathSegment}/${part.name ?: fileName}"
                            } catch (e: Throwable) {
                                // NOT a `finally`: a successful rename makes `part` the real
                                // backup, and deleting it there would throw away the export.
                                part.delete()
                                throw e
                            }
                        }
                        reply(
                            "OK:$shownPath|$bytes|${humanSize(bytes)}|${selection.categoryCount} categories",
                        )
                    } catch (e: ExportCancelledException) {
                        // The terminal reply for the ORIGINAL request, sent even though 自由作業盤
                        // stopped listening the moment it pressed 中止: it is what proves the run
                        // really ended rather than carrying on unseen.
                        reply("ERROR:cancelled")
                    } catch (e: Exception) {
                        reply("ERROR:${e.message ?: e.javaClass.simpleName}")
                    } finally {
                        StateExportJob.finish(job)
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
        val ACTION_CANCEL_EXPORT = "${BuildConfig.APPLICATION_ID}.action.CANCEL_EXPORT"

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
        const val EXTRA_PROGRESS_ITEM = "item"
        const val EXTRA_PROGRESS_TEXT = "text"
        const val EXTRA_PROGRESS_CURRENT = "current"
        const val EXTRA_PROGRESS_TOTAL = "total"
        const val EXTRA_PROGRESS_UNIT = "unit"

        /**
         * The in-flight name (§1: write to `<final-name>.part`, rename only once the archive is
         * closed and complete). A killed export otherwise leaves a file indistinguishable from a
         * real backup until someone tries to restore it — and 白い熊 keeps every app's backups in
         * one directory sorted by date, so a truncated one silently becomes "the latest backup".
         */
        private const val PART_SUFFIX = ".part"

        /** See the SAF branch: keeps the provider from appending `.zip` to the part-file. */
        private const val PART_MIME = "application/octet-stream"

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
