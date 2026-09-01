package app.pwhs.updater.domain.seed

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import app.pwhs.core.data.local.dataStore
import app.pwhs.updater.domain.model.TrackedApp
import app.pwhs.updater.domain.model.UpdateSourceType
import kotlinx.coroutines.flow.firstOrNull

/**
 * Seeds the default tracked app (Universal Installer itself) on first launch
 * so users get a self-update mechanism out of the box.
 */
object DefaultAppSeeder {

    private val SEEDED_KEY = booleanPreferencesKey("default_apps_seeded")

    private const val DEFAULT_SOURCE_URL = "https://github.com/pass-with-high-score/universal-installer"
    private const val DEFAULT_PACKAGE_NAME = "app.pwhs.universalinstaller"
    private const val DEFAULT_APP_NAME = "Universal Installer"

    /**
     * Returns true if seeding has already been performed.
     */
    suspend fun isSeeded(context: Context): Boolean {
        val prefs = context.dataStore.data.firstOrNull() ?: return false
        return prefs[SEEDED_KEY] == true
    }

    /**
     * Marks seeding as complete.
     */
    suspend fun markSeeded(context: Context) {
        context.dataStore.edit { it[SEEDED_KEY] = true }
    }

    /**
     * Returns the default seed app entry for Universal Installer.
     */
    fun defaultSeedApp(): TrackedApp {
        return TrackedApp(
            packageName = DEFAULT_PACKAGE_NAME,
            appName = DEFAULT_APP_NAME,
            sourceType = UpdateSourceType.GITHUB,
            sourceUrl = DEFAULT_SOURCE_URL,
            currentVersionName = "Not Installed",
            currentVersionCode = 0L,
        )
    }
}
