package app.pwhs.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SharedPrefsKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    /** TV: when true (default) and root is available, install silently via the root shell. */
    val ROOT_SILENT_INSTALL = booleanPreferencesKey("tv_root_silent_install")

    /**
     * "Normal" or "Strict". Declared here because onboarding lives in :core but the setting is
     * read by the phone app's install flow; both sides must agree on the key name.
     */
    val SECURITY_LEVEL = stringPreferencesKey("security_level")

    /** Legacy companion to [SECURITY_LEVEL], kept in step so older read sites still agree. */
    val STRICT_VIRUSTOTAL_CHECK = booleanPreferencesKey("strict_virustotal_check")

    /**
     * The VirusTotal API key. Onboarding writes it and the phone app's Settings screen reads and
     * writes the same preference, so the key name must match `SettingViewModel.PreferencesKeys`.
     */
    val VIRUSTOTAL_API_KEY = stringPreferencesKey("virustotal_api_key")
}
