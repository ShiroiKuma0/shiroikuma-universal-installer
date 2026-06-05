package app.pwhs.universalinstaller.ui.theme

import androidx.datastore.preferences.core.Preferences
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Dedicated, app-wide theming for the bottom navigation bar (Install / Manage / Settings). Unlike the
 * per-surface [SurfaceTheme], this is global so the bar looks identical on every tab. Any null field
 * inherits the Material default. Read straight from DataStore by [app.pwhs.universalinstaller
 * .presentation.composable.BottomBar].
 */
@Serializable
data class BottomBarTheme(
    val container: Int? = null,        // bar background
    val selectedIcon: Int? = null,
    val selectedText: Int? = null,
    val indicator: Int? = null,        // selected pill behind the icon
    val unselectedIcon: Int? = null,
    val unselectedText: Int? = null,
) {
    val hasOverride: Boolean
        get() = container != null || selectedIcon != null || selectedText != null ||
            indicator != null || unselectedIcon != null || unselectedText != null
}

object BottomBarThemeStore {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(serialized: String?): BottomBarTheme =
        if (serialized.isNullOrBlank()) BottomBarTheme()
        else try { json.decodeFromString(serialized) } catch (e: Exception) { BottomBarTheme() }

    fun serialize(theme: BottomBarTheme): String = json.encodeToString(theme)

    fun from(prefs: Preferences): BottomBarTheme = parse(prefs[PreferencesKeys.UI_BOTTOM_BAR_THEME])
}
