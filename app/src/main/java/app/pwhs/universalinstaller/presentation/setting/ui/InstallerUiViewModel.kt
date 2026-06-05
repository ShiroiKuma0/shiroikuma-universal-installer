package app.pwhs.universalinstaller.presentation.setting.ui

import android.app.Application
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.dataStore
import app.pwhs.universalinstaller.ui.theme.AppSurface
import app.pwhs.universalinstaller.ui.theme.SurfaceTheme
import app.pwhs.universalinstaller.ui.theme.SurfaceThemeStore
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

    // Per-surface overrides (install dialog / main page).
    val dialogTheme: StateFlow<SurfaceTheme> = surfaceFlow(AppSurface.Dialog)
    val mainTheme: StateFlow<SurfaceTheme> = surfaceFlow(AppSurface.Main)

    private fun surfaceFlow(surface: AppSurface): StateFlow<SurfaceTheme> =
        application.dataStore.data
            .map { SurfaceThemeStore.from(it, surface) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurfaceTheme())

    fun setSurfaceTheme(surface: AppSurface, theme: SurfaceTheme) =
        edit { it[SurfaceThemeStore.key(surface)] = SurfaceThemeStore.serialize(theme) }

    // Recently-picked colours (most-recent first, deduped, capped) — shown as one-touch picker hotpicks.
    val recentColors: StateFlow<List<Int>> = application.dataStore.data
        .map { prefs ->
            (prefs[PreferencesKeys.UI_RECENT_COLORS] ?: "")
                .split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordRecentColor(argb: Int) = edit { prefs ->
        val current = (prefs[PreferencesKeys.UI_RECENT_COLORS] ?: "")
            .split(",").mapNotNull { it.trim().toIntOrNull() }
        val updated = (listOf(argb) + current).distinct().take(12)
        prefs[PreferencesKeys.UI_RECENT_COLORS] = updated.joinToString(",")
    }

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
