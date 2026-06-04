package app.pwhs.universalinstaller.presentation.setting.ui

import android.app.Application
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.dataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstallerUiState(
    val fontFamily: String = "",
    val fontWeight: Int = 0,
    val fontScale: Float = 1f,
    val monoTechnical: Boolean = false,
    val accentColor: Int = 0,
    val cornerScale: Float = 1f,
)

/** Reads/writes the six 白い熊 Installer UI preferences; the theme picks them up via DataStore. */
class InstallerUiViewModel(private val application: Application) : ViewModel() {

    val uiState: StateFlow<InstallerUiState> = application.dataStore.data
        .map { prefs ->
            InstallerUiState(
                fontFamily = prefs[PreferencesKeys.UI_FONT_FAMILY] ?: "",
                fontWeight = prefs[PreferencesKeys.UI_FONT_WEIGHT] ?: 0,
                fontScale = prefs[PreferencesKeys.UI_FONT_SCALE] ?: 1f,
                monoTechnical = prefs[PreferencesKeys.UI_MONO_TECHNICAL] ?: false,
                accentColor = prefs[PreferencesKeys.UI_ACCENT_COLOR] ?: 0,
                cornerScale = prefs[PreferencesKeys.UI_CORNER_SCALE] ?: 1f,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallerUiState())

    fun setFontFamily(value: String) = edit { it[PreferencesKeys.UI_FONT_FAMILY] = value }
    fun setFontWeight(value: Int) = edit { it[PreferencesKeys.UI_FONT_WEIGHT] = value }
    fun setFontScale(value: Float) = edit { it[PreferencesKeys.UI_FONT_SCALE] = value }
    fun setMonoTechnical(value: Boolean) = edit { it[PreferencesKeys.UI_MONO_TECHNICAL] = value }
    fun setAccentColor(value: Int) = edit { it[PreferencesKeys.UI_ACCENT_COLOR] = value }
    fun setCornerScale(value: Float) = edit { it[PreferencesKeys.UI_CORNER_SCALE] = value }

    private fun edit(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { application.dataStore.edit(block) }
    }
}
