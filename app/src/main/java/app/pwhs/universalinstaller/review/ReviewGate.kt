package app.pwhs.universalinstaller.review

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import app.pwhs.core.data.local.dataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Decides *when* asking for a review is appropriate, and announces the moment.
 *
 * Google's rules shape most of this: the sheet may not be attached to a button, may not be
 * preceded by a question of our own, and is rate-limited by Play itself — a call over quota shows
 * nothing and still reports success. So an ask we waste is an ask we don't get back, and the
 * thresholds below are deliberately conservative.
 *
 * The ask is announced rather than performed: [opportunities] has no buffer and no replay, so an
 * install that finishes with no resumed screen collecting — from a notification, or with the app
 * in the background — is dropped instead of queued. `InstallActivity` is the only collector.
 */
object ReviewGate {

    /** Stamped at first start, so an ask can't land on someone who installed us an hour ago. */
    private val FIRST_LAUNCH_AT = longPreferencesKey("review_first_launch_at")
    private val LAST_PROMPT_AT = longPreferencesKey("review_last_prompt_at")
    private val PROMPT_COUNT = intPreferencesKey("review_prompt_count")

    /**
     * Counted here rather than read from `install_history`, which the user can clear from the
     * Install screen — that clears their history, not the fact that the app worked for them.
     */
    private val SUCCESSFUL_INSTALLS = intPreferencesKey("review_successful_installs")

    /** Enough installs that the user has an opinion worth typing out. */
    private const val MIN_SUCCESSFUL_INSTALLS = 3
    private const val MIN_AGE_MS = 3L * 24 * 60 * 60 * 1000
    private const val MIN_GAP_MS = 90L * 24 * 60 * 60 * 1000
    private const val MAX_PROMPTS = 3

    // One slot of buffer, no replay: with a collector the ask is delivered, with none it is
    // dropped. A zero-buffer flow would instead make tryEmit fail on the one case that
    // matters — a screen actually watching.
    private val _opportunities = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val opportunities: SharedFlow<Unit> = _opportunities.asSharedFlow()

    /** Records the install's age on first run. Later runs leave the original stamp alone. */
    suspend fun rememberFirstLaunch(context: Context) {
        if (!AppReview.isAvailable) return
        edit(context) { prefs ->
            if (prefs[FIRST_LAUNCH_AT] == null) prefs[FIRST_LAUNCH_AT] = now()
        }
    }

    /**
     * One install worked. Announces an opportunity if that was enough to clear the gates.
     *
     * Called from the install controllers, which run whether or not any screen is in front —
     * see the note on [opportunities] for what happens then.
     */
    suspend fun recordSuccessfulInstall(context: Context) {
        if (!AppReview.isAvailable) return
        val prefs = edit(context) { it[SUCCESSFUL_INSTALLS] = (it[SUCCESSFUL_INSTALLS] ?: 0) + 1 }
            ?: return
        // tryEmit, not emit: with no collector the moment has passed, and the install path must
        // not wait on it either way.
        if (isEligible(prefs)) _opportunities.tryEmit(Unit)
    }

    /** Whether an ask would be allowed right now, for deciding to pre-warm the review flow. */
    suspend fun isEligible(context: Context): Boolean {
        if (!AppReview.isAvailable) return false
        val prefs = runCatching { context.dataStore.data.first() }
            .onFailure { Timber.w(it, "Could not read the review preferences") }
            .getOrNull() ?: return false
        return isEligible(prefs)
    }

    /** Called after the sheet was asked for — whether or not Play actually showed anything. */
    suspend fun recordPrompted(context: Context) {
        edit(context) { prefs ->
            prefs[LAST_PROMPT_AT] = now()
            prefs[PROMPT_COUNT] = (prefs[PROMPT_COUNT] ?: 0) + 1
        }
    }

    private fun isEligible(prefs: Preferences): Boolean {
        if ((prefs[SUCCESSFUL_INSTALLS] ?: 0) < MIN_SUCCESSFUL_INSTALLS) return false
        if ((prefs[PROMPT_COUNT] ?: 0) >= MAX_PROMPTS) return false
        val now = now()
        // Absent means this is the first run and the stamp is being written right about now;
        // treat it as "too new" rather than "infinitely old".
        val firstLaunch = prefs[FIRST_LAUNCH_AT] ?: now
        if (now - firstLaunch < MIN_AGE_MS) return false
        val lastPrompt = prefs[LAST_PROMPT_AT] ?: return true
        return now - lastPrompt >= MIN_GAP_MS
    }

    private suspend fun edit(
        context: Context,
        block: (MutablePreferences) -> Unit,
    ): Preferences? = runCatching { context.dataStore.edit(block) }
        .onFailure { Timber.w(it, "Could not update the review preferences") }
        .getOrNull()

    private fun now() = System.currentTimeMillis()
}
