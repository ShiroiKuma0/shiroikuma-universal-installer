package app.pwhs.universalinstaller.domain.manager

import androidx.datastore.preferences.core.Preferences
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys

/**
 * Manages the whitelist of caller applications permitted to trigger automatic,
 * unprompted installations (when using a privileged backend like Shizuku or Root).
 */
object AutoApproveApps {

    data class PresetApp(
        val name: String,
        val packageName: String,
    )

    val PRESET_APPS = listOf(
        PresetApp(name = "F-Droid", packageName = "org.fdroid.fdroid"),
        PresetApp(name = "Obtainium", packageName = "dev.imranr.obtainium"),
        PresetApp(name = "Aurora Store", packageName = "com.aurora.store"),
        PresetApp(name = "MiXplorer", packageName = "com.mixplorer"),
        PresetApp(name = "Solid Explorer", packageName = "pl.solidexplorer2"),
        PresetApp(name = "Total Commander", packageName = "com.ghisler.android.TotalCommander"),
        PresetApp(name = "Files by Google", packageName = "com.google.android.apps.nbu.files"),
    )

    fun isEnabled(prefs: Preferences?): Boolean =
        prefs?.get(PreferencesKeys.AUTO_APPROVE_CALLER_APPS) ?: false

    fun read(prefs: Preferences?): Set<String> =
        prefs?.get(PreferencesKeys.AUTO_APPROVE_PACKAGES).orEmpty()

    /**
     * Checks if [callerPackage] is permitted to auto-install under the current preferences.
     * Returns true only if the auto-approve feature is enabled and [callerPackage] is non-blank
     * and in the approved set.
     */
    fun isAutoApproved(prefs: Preferences?, callerPackage: String?): Boolean {
        if (!isEnabled(prefs)) return false
        if (callerPackage.isNullOrBlank()) return false
        return callerPackage.trim() in read(prefs)
    }

    fun add(current: Set<String>, packageName: String): Set<String> =
        if (packageName.isBlank()) current else current + packageName.trim()

    fun remove(current: Set<String>, packageName: String): Set<String> =
        current - packageName.trim()
}
