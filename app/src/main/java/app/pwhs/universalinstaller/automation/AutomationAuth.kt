package app.pwhs.universalinstaller.automation

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * External-automation intent surface ([StateExportReceiver]): a master switch plus a shared secret
 * that every automation broadcast must carry — the same model as the renrakusaki fork's Config
 * (automation_enabled / automation_token) and 自由作業盤's AutomationAuth.
 *
 * Device-local by design: these live in their own SharedPreferences file, while the export covers
 * DataStore keys only, so the token can never travel in a backup ZIP or leave the phone.
 */
object AutomationAuth {

    private const val PREFS_FILE = "universalinstaller_automation"
    private const val KEY_ENABLED = "automation_enabled"
    private const val KEY_TOKEN = "automation_token"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    /** The shared secret; generated on first read so the settings row always shows a value. */
    fun token(context: Context): String =
        prefs(context).getString(KEY_TOKEN, null)?.takeIf { it.isNotEmpty() }
            ?: regenerateToken(context)

    fun regenerateToken(context: Context): String {
        val bytes = ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        val token = bytes.joinToString("") { "%02x".format(it) }
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
        return token
    }

    /**
     * True when the caller's token matches the stored secret (constant-time). The enabled check is
     * kept separate so callers can report "disabled" and "bad token" as distinct failures.
     */
    fun isTokenValid(context: Context, candidate: String?): Boolean {
        if (candidate.isNullOrEmpty()) return false
        return MessageDigest.isEqual(candidate.toByteArray(), token(context).toByteArray())
    }

    private const val TOKEN_BYTES = 24
}
