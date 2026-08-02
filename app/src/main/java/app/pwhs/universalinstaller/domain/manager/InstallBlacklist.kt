package app.pwhs.universalinstaller.domain.manager

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Package names the user has chosen never to install.
 *
 * Deliberately *not* modelled as an [app.pwhs.universalinstaller.presentation.install.dialog.InstallRisk].
 * A risk is something you warn about and let the user push past — the risk dialog's whole shape is
 * "here is the problem, Install anyway". A blacklist that can be clicked through is not a
 * blacklist. This blocks the install outright and the only way forward is to remove the entry.
 *
 * The store is a plain `Set<String>` in DataStore rather than a Room table: it is a handful of
 * package names, read on every install, and a table would mean a migration for no benefit.
 */
object InstallBlacklist {

    val KEY: Preferences.Key<Set<String>> = stringSetPreferencesKey("install_blacklist")

    fun read(prefs: Preferences?): Set<String> = prefs?.get(KEY).orEmpty()

    /**
     * True when [packageName] is blocked.
     *
     * Blank package names never match. An APK we failed to parse reports its package as blank or
     * "Unknown", and blocking every unparseable file because the blacklist happens to be non-empty
     * would be a nasty surprise.
     */
    fun isBlocked(prefs: Preferences?, packageName: String): Boolean =
        packageName.isNotBlank() && packageName in read(prefs)

    fun add(current: Set<String>, packageName: String): Set<String> =
        if (packageName.isBlank()) current else current + packageName.trim()

    fun remove(current: Set<String>, packageName: String): Set<String> =
        current - packageName
}
