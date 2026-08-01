package app.pwhs.universalinstaller.base

import androidx.datastore.preferences.core.Preferences
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.domain.ThemeMode
import app.pwhs.universalinstaller.ui.theme.ForkUiDefaults

/**
 * Snapshot of every DataStore preference that affects how a screen is themed — the stock theme
 * (mode/dynamic/AMOLED) plus the 白い熊 Installer UI customizations (font, accent, shape, mono).
 * Shared so [BaseActivity] and [app.pwhs.universalinstaller.MainActivity] parse it identically.
 */
data class AppThemeState(
    val mode: ThemeMode,
    val dynamicColor: Boolean,
    val amoledMode: Boolean,
    val fontFamily: String,
    val fontWeight: Int,
    val fontScale: Float,
    val monoTechnical: Boolean,
    val accentColor: Int,
    val cornerScale: Float,
    val themePreset: AppThemePreset,
)

/**
 * Every fallback here is a compiled-in 白い熊 default ([ForkUiDefaults]) rather than an upstream
 * Material one, so a fresh install comes up black/yellow instead of orange (and, before this,
 * wallpaper-derived Material You: `dynamicColor` defaulted to true here while the Settings screen
 * showed the toggle off).
 */
fun Preferences.toAppThemeState(): AppThemeState {
    val name = this[PreferencesKeys.THEME_MODE] ?: ForkUiDefaults.Mode.name
    return AppThemeState(
        mode = ThemeMode.entries.find { it.name == name } ?: ForkUiDefaults.Mode,
        dynamicColor = this[PreferencesKeys.DYNAMIC_COLOR] ?: ForkUiDefaults.DynamicColor,
        amoledMode = this[PreferencesKeys.AMOLED_MODE] ?: ForkUiDefaults.Amoled,
        fontFamily = this[PreferencesKeys.UI_FONT_FAMILY] ?: "",
        fontWeight = this[PreferencesKeys.UI_FONT_WEIGHT] ?: 0,
        fontScale = this[PreferencesKeys.UI_FONT_SCALE] ?: 1f,
        monoTechnical = this[PreferencesKeys.UI_MONO_TECHNICAL] ?: false,
        accentColor = this[PreferencesKeys.UI_ACCENT_COLOR] ?: ForkUiDefaults.Yellow,
        cornerScale = this[PreferencesKeys.UI_CORNER_SCALE] ?: 1f,
        themePreset = AppThemePreset.entries.find {
            it.name == (this[PreferencesKeys.THEME_PRESET] ?: ForkUiDefaults.Preset.name)
        } ?: ForkUiDefaults.Preset,
    )
}
