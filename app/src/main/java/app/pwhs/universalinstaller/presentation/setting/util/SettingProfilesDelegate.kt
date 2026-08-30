package app.pwhs.universalinstaller.presentation.setting.util

import android.app.Application
import androidx.datastore.preferences.core.edit
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.domain.manager.ProfileManager
import app.pwhs.universalinstaller.domain.model.InstallerProfile
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingProfilesDelegate(
    application: Application,
    private val scope: CoroutineScope,
) {
    private val dataStore = application.dataStore

    fun saveProfile(profile: InstallerProfile, onSaved: () -> Unit = {}) {
        scope.launch {
            dataStore.edit { prefs ->
                val current = ProfileManager.parseProfiles(prefs[PreferencesKeys.INSTALLER_PROFILES])
                val index = current.indexOfFirst { it.id == profile.id }
                val updated = if (index != -1) {
                    current.toMutableList().apply { set(index, profile) }
                } else {
                    current + profile
                }
                prefs[PreferencesKeys.INSTALLER_PROFILES] = ProfileManager.serializeProfiles(updated)
            }
            onSaved()
        }
    }

    fun deleteProfile(profileId: String) {
        scope.launch {
            dataStore.edit { prefs ->
                val current = ProfileManager.parseProfiles(prefs[PreferencesKeys.INSTALLER_PROFILES])
                val updated = current.filterNot { it.id == profileId }
                prefs[PreferencesKeys.INSTALLER_PROFILES] = ProfileManager.serializeProfiles(updated)

                val currentMapping = ProfileManager.parseMapping(prefs[PreferencesKeys.APP_PROFILE_MAPPING])
                val updatedMapping = currentMapping.filterValues { it != profileId }
                prefs[PreferencesKeys.APP_PROFILE_MAPPING] = ProfileManager.serializeMapping(updatedMapping)
            }
        }
    }
}
