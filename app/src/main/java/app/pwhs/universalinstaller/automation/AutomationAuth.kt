package app.pwhs.universalinstaller.automation

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The gate in front of both automation surfaces — the intent receiver ([StateExportReceiver]) and
 * the data door ([AutomationProvider]) — as sister-app contract **v2** defines it.
 *
 * ## What v2 changed, and why it had to
 *
 * v1 shipped every app closed: the master switch defaulted to false and a caller also had to carry
 * a 48-character secret 白い熊 had pasted from this app's settings into the caller's. That is the
 * wrong shape for where this is going. **A pasted secret cannot survive a wipe**, and the case the
 * whole family now exists to serve is 応用管理 restoring apps *and their data* onto a clean phone,
 * where nothing has been configured and nobody has pasted anything. A gate that only works once the
 * phone is already set up is no gate for setting the phone up.
 *
 * So the switch now ships **ON** and the token is **opt-in**. The token infrastructure is untouched
 * — same 24 `SecureRandom` bytes, same lazy generation, same constant-time compare — it is simply
 * no longer asked for unless [requireToken] says so.
 *
 * Device-local by design: these live in their own SharedPreferences file while the export covers
 * DataStore keys only, so the token can never travel inside a backup ZIP or leave the phone.
 */
object AutomationAuth {

    private const val PREFS_FILE = "universalinstaller_automation"
    private const val KEY_ENABLED = "automation_enabled"
    private const val KEY_REQUIRE_TOKEN = "automation_require_token"
    private const val KEY_TOKEN = "automation_token"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    /**
     * The master switch — **default ON** in v2.
     *
     * It stays a switch rather than being deleted because it is the only way to close this app off
     * entirely, and a feature that can be turned on but never off is one 白い熊 cannot retreat from.
     */
    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    /** 「Use authorization token?」 — **default OFF**; see [refuse] for what it gates. */
    fun requireToken(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REQUIRE_TOKEN, false)

    fun setRequireToken(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_REQUIRE_TOKEN, value).apply()
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
     * True when the caller's token matches the stored secret, compared in constant time.
     *
     * Only consulted when [requireToken] is on. The compare stays constant-time for that case: the
     * habit is worth keeping and costs nothing.
     */
    fun isTokenValid(context: Context, candidate: String?): Boolean {
        if (candidate.isNullOrEmpty()) return false
        return MessageDigest.isEqual(candidate.toByteArray(), token(context).toByteArray())
    }

    /**
     * The whole gate, in **one** function: null means proceed, anything else is the exact `ERROR:`
     * line to answer with.
     *
     * Deliberately one function rather than two checks written out at each entry point — that is
     * how "disabled" and "bad token" drift apart across forty-two sister apps. The two are reported
     * distinctly because they debug differently.
     *
     * ## A token we did not ask for is IGNORED, never refused
     *
     * This is required, not a nicety. Tokens live in task arguments and workspace variables that
     * outlive the setting they were pasted for, so a caller may still send one because it was
     * configured last year, or because another app on the batch does want one. Refusing it would
     * turn "白い熊 turned a switch off" into "half the batch mysteriously fails", which is exactly
     * the friction the switch exists to remove. Note the `&&`: when [requireToken] is off,
     * [candidate] is never even looked at.
     */
    fun refuse(context: Context, candidate: String?): String? = when {
        !enabled(context) -> "ERROR:automation disabled"
        requireToken(context) && !isTokenValid(context, candidate) -> "ERROR:bad token"
        else -> null
    }

    private const val TOKEN_BYTES = 24
}
