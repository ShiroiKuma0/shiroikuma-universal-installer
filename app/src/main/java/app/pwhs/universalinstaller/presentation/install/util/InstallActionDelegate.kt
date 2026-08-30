package app.pwhs.universalinstaller.presentation.install.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.domain.manager.InstallBlacklist
import app.pwhs.universalinstaller.domain.manager.ProfileManager
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys

object InstallActionDelegate {

    suspend fun setAllUsers(context: Context, enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.SHIZUKU_ALL_USERS] = enabled
            it[PreferencesKeys.ROOT_ALL_USERS] = enabled
            if (enabled) it.remove(PreferencesKeys.INSTALL_USER_ID)
        }
    }

    suspend fun setUserId(context: Context, id: Int?) {
        context.dataStore.edit {
            if (id != null) {
                it[PreferencesKeys.INSTALL_USER_ID] = id
                it[PreferencesKeys.SHIZUKU_ALL_USERS] = false
                it[PreferencesKeys.ROOT_ALL_USERS] = false
            } else {
                it.remove(PreferencesKeys.INSTALL_USER_ID)
            }
        }
    }

    suspend fun setAppProfileMapping(context: Context, packageName: String, profileId: String?) {
        context.dataStore.edit { prefs ->
            val current = ProfileManager.parseMapping(prefs[PreferencesKeys.APP_PROFILE_MAPPING]).toMutableMap()
            if (profileId != null) current[packageName] = profileId else current.remove(packageName)
            prefs[PreferencesKeys.APP_PROFILE_MAPPING] = ProfileManager.serializeMapping(current)
        }
    }

    suspend fun unblockPackage(context: Context, packageName: String) {
        context.dataStore.edit { p ->
            p[InstallBlacklist.KEY] = InstallBlacklist.remove(InstallBlacklist.read(p), packageName)
        }
    }
}
