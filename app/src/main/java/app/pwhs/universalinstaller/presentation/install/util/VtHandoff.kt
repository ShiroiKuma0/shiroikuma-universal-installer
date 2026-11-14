package app.pwhs.universalinstaller.presentation.install.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

/**
 * The install end of the APK vetting chain (白い熊, 2026-11-14): the file manager (shoruikanri)
 * opens an APK in 応用管理, which runs the full VirusTotal scan and shows the report, and that report's Install
 * pill hands the file to this app. The names below are a contract shared by the three apps — never
 * rename one here alone.
 *
 * Every direction falls back to the old behaviour when the other side is missing, so neither app
 * depends on the other being updated first.
 */
object VtHandoff {

    const val SCANNER_PACKAGE = "shiroikuma.oyokanri"
    private const val SCANNER_ACTIVITY = "io.github.muntashirakon.AppManager.scanner.vt.VtReportActivity"
    private const val ACTION_VT_SCAN = "shiroikuma.oyokanri.action.VT_SCAN"

    const val EXTRA_REAL_PATH = "shiroikuma.extra.REAL_PATH"
    private const val EXTRA_VT_REVIEWED = "shiroikuma.extra.VT_REVIEWED"
    private const val EXTRA_VT_SHA256 = "shiroikuma.extra.VT_SHA256"

    /**
     * 応用管理 showed its user installed-version → new-version for this exact file and they pressed
     * Install / Update / Reinstall / Downgrade there (白い熊, 2026-11-14; sent from 応用管理 +082).
     * Honoured under the same two conditions as [EXTRA_VT_REVIEWED] — the system-recorded caller
     * and the hash — and then only skips the "do you want to?" step: see `ConfirmedHandoff`.
     */
    private const val EXTRA_INSTALL_CONFIRMED = "shiroikuma.extra.INSTALL_CONFIRMED"

    private val SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

    /**
     * The package that started [activity], from sources the caller cannot forge.
     *
     * [CallerAppDetector] is deliberately not used for this: it also reads `EXTRA_REFERRER` and the
     * content-URI authority, which any app (or `adb shell am start`) can set to whatever it likes.
     * That is fine for picking an auto-approve rule, not for vouching that a scan was reviewed.
     * Below API 34, [Activity.getReferrer] is the launching package recorded by the system (checked
     * against the caller's uid since Android 11) — but only when the intent carries no referrer
     * extra, which would take precedence over it.
     */
    private fun trustedLauncherPackage(activity: Activity, intent: Intent): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return activity.launchedFromPackage?.takeIf { it.isNotBlank() }
        }
        if (intent.hasExtra(Intent.EXTRA_REFERRER) || intent.hasExtra(Intent.EXTRA_REFERRER_NAME)) return null
        val referrer = runCatching { activity.referrer }.getOrNull() ?: return null
        return referrer.host?.takeIf { referrer.scheme == "android-app" && it.isNotBlank() }
    }

    /**
     * The SHA-256 the user reviewed in 応用管理, or null when the hand-off is not to be honoured:
     * the caller is not 応用管理, the reviewed flag is missing, or the hash is malformed. The hash
     * still has to match the file before anything changes — see `InstallParseDelegate`.
     */
    fun reviewedSha256(activity: Activity, intent: Intent): String? =
        trustedSha256(activity, intent, EXTRA_VT_REVIEWED)

    /**
     * The SHA-256 of the file whose install the user already confirmed in 応用管理, or null when
     * that claim is not to be honoured. Same trust conditions as [reviewedSha256], and likewise
     * nothing changes until the hash matches the file.
     */
    fun confirmedSha256(activity: Activity, intent: Intent): String? =
        trustedSha256(activity, intent, EXTRA_INSTALL_CONFIRMED)

    private fun trustedSha256(activity: Activity, intent: Intent, flagExtra: String): String? {
        if (!intent.getBooleanExtra(flagExtra, false)) return null
        val sha256 = intent.getStringExtra(EXTRA_VT_SHA256)?.trim()
        val caller = trustedLauncherPackage(activity, intent)
        if (caller != SCANNER_PACKAGE || sha256 == null || !SHA256_HEX.matches(sha256)) {
            Timber.w("Hand-off $flagExtra ignored: caller=$caller, sha256 well-formed=${sha256?.let(SHA256_HEX::matches)}")
            return null
        }
        return sha256.lowercase()
    }

    /**
     * The file behind [uri] on disk, when the sender named it in [EXTRA_REAL_PATH] and this app can
     * read it (it holds all-files access). Rejected when it is not the same size as the URI's
     * content, so a stray path can never stand in for a different file.
     */
    fun readableRealPath(context: Context, intent: Intent, uri: Uri): File? {
        val path = intent.getStringExtra(EXTRA_REAL_PATH)?.takeIf { it.startsWith("/") } ?: return null
        val file = File(path)
        if (!runCatching { file.isFile && file.canRead() }.getOrDefault(false)) return null
        val uriSize = contentSize(context, uri)
        if (uriSize != null && uriSize != file.length()) {
            Timber.w("REAL_PATH $path ignored: ${file.length()} bytes, URI says $uriSize")
            return null
        }
        return file
    }

    private fun contentSize(context: Context, uri: Uri): Long? = runCatching {
        if (uri.scheme == "file") return@runCatching uri.path?.let { File(it).length() }
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst() && idx >= 0 && !c.isNull(idx)) c.getLong(idx) else null
        }
    }.getOrNull()

    /**
     * Best-effort original path of [uri] for the scan request: the sender's [EXTRA_REAL_PATH], a
     * `file://` path, or the legacy `_data` column that MediaStore and Downloads still fill in.
     * Never the staged cache copy, which is private and pruned.
     */
    fun originalPath(context: Context, intent: Intent, uri: Uri): String? {
        readableRealPath(context, intent, uri)?.let { return it.absolutePath }
        val candidate = when (uri.scheme) {
            "file" -> uri.path
            "content" -> runCatching {
                context.contentResolver.query(uri, arrayOf("_data"), null, null, null)?.use { c ->
                    val idx = c.getColumnIndex("_data")
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                }
            }.getOrNull()
            else -> null
        } ?: return null
        return candidate.takeIf { runCatching { File(it).isFile }.getOrDefault(false) }
    }

    private fun scannerComponent() = ComponentName(SCANNER_PACKAGE, SCANNER_ACTIVITY)

    /** Whether the installed 応用管理 has the scan entry. An older build or none hides the pill. */
    fun isScannerAvailable(context: Context): Boolean = runCatching {
        val probe = Intent(ACTION_VT_SCAN).setComponent(scannerComponent())
        context.packageManager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY) != null
    }.getOrDefault(false)

    /**
     * Opens 応用管理's scan and report for this file. The data URI is one this app may grant: the
     * original file through this app's own FileProvider when its path is known, else the incoming
     * URI (whose read grant this app holds and may pass on), else the local copy this app parsed
     * (a URL download). Returns false when the scanner could not be started.
     */
    fun startFullScan(
        context: Context,
        incomingUri: Uri,
        localUri: Uri?,
        realPath: String?,
        mimeType: String?,
    ): Boolean {
        fun shared(path: String?): Uri? = path?.let {
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(it))
            }.getOrNull()
        }
        val dataUri = shared(realPath)
            ?: incomingUri.takeIf { it.scheme == "content" }
            ?: shared(localUri?.takeIf { it.scheme == "file" }?.path)
            ?: return false
        val intent = Intent(ACTION_VT_SCAN)
            .setComponent(scannerComponent())
            .setDataAndType(dataUri, mimeType ?: "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (realPath != null) intent.putExtra(EXTRA_REAL_PATH, realPath)
        return runCatching { context.startActivity(intent) }
            .onFailure { Timber.e(it, "Could not start the 応用管理 scan") }
            .isSuccess
    }
}
