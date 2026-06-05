package app.pwhs.universalinstaller.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Per-surface look-and-feel override for the 白い熊 Installer UI page. Each surface (the install pop-up
 * dialog, the main page) can override a handful of friendly colour roles and a font; anything left null
 * inherits the global app theme. Applied by wrapping the surface in a nested [MaterialTheme] (see
 * [ThemedSurface]), so every element that reads `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*`
 * restyles automatically.
 */
enum class AppSurface { Dialog, Main }

/** Border stroke for the current surface's card (the install dialog reads this); null = no border. */
val LocalSurfaceBorder = staticCompositionLocalOf<BorderStroke?> { null }

@Serializable
data class SurfaceTheme(
    val accent: Int? = null,         // -> primary
    val titleText: Int? = null,      // -> onSurface / onBackground
    val secondaryText: Int? = null,  // -> onSurfaceVariant
    val card: Int? = null,           // -> surfaceContainer* (cards + dialog card)
    val background: Int? = null,     // -> background / surface
    val danger: Int? = null,         // -> error
    val success: Int? = null,        // -> tertiary
    val highlight: Int? = null,      // -> primaryContainer
    val borderColor: Int? = null,    // dialog card border colour
    val borderWidth: Float? = null,  // dialog card border width in dp; null/0 = no border
    val fontFamily: String? = null,  // null = inherit; "" = system; "@monospace"; else imported filename
    val fontWeight: Int? = null,     // null = inherit; else 100..900
    val fontScale: Float? = null,    // null/1f = inherit size
    // Per-button overrides (install dialog only), keyed by button slot (menu/install/cancel/…).
    val buttons: Map<String, ButtonStyle> = emptyMap(),
) {
    val hasColorOverride: Boolean
        get() = accent != null || titleText != null || secondaryText != null || card != null ||
            background != null || danger != null || success != null || highlight != null

    val hasAnyOverride: Boolean
        get() = hasColorOverride || borderColor != null || borderWidth != null ||
            fontFamily != null || fontWeight != null || fontScale != null || buttons.isNotEmpty()
}

/** Per-button style override; any null field inherits the button's default. */
@Serializable
data class ButtonStyle(
    val bg: Int? = null,           // container / background
    val content: Int? = null,      // text + icon colour
    val borderColor: Int? = null,
    val borderWidth: Float? = null,
    val fontFamily: String? = null,
    val fontWeight: Int? = null,
    val fontScale: Float? = null,  // text-size multiplier; null = inherit
)

object SurfaceThemeStore {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(serialized: String?): SurfaceTheme =
        if (serialized.isNullOrBlank()) SurfaceTheme()
        else try { json.decodeFromString(serialized) } catch (e: Exception) { SurfaceTheme() }

    fun serialize(theme: SurfaceTheme): String = json.encodeToString(theme)

    fun key(surface: AppSurface) = when (surface) {
        AppSurface.Dialog -> PreferencesKeys.UI_DIALOG_THEME
        AppSurface.Main -> PreferencesKeys.UI_MAIN_THEME
    }

    fun from(prefs: Preferences, surface: AppSurface): SurfaceTheme = parse(prefs[key(surface)])
}

private fun onColorFor(c: Color): Color = if (c.luminance() > 0.5f) Color.Black else Color.White
private fun containerFor(c: Color, dark: Boolean): Color =
    lerp(c, if (dark) Color.Black else Color.White, if (dark) 0.6f else 0.78f)
private fun onContainerFor(c: Color, dark: Boolean): Color =
    if (dark) lerp(c, Color.White, 0.6f) else lerp(c, Color.Black, 0.5f)

/** Overlay a surface's chosen colour roles onto this scheme; untouched roles inherit. */
fun ColorScheme.applySurface(t: SurfaceTheme, isDark: Boolean): ColorScheme {
    if (!t.hasColorOverride) return this
    var s = this
    t.accent?.let { Color(it).let { c -> s = s.copy(
        primary = c, onPrimary = onColorFor(c),
        inversePrimary = lerp(c, if (isDark) Color.White else Color.Black, 0.3f),
    ) } }
    t.titleText?.let { s = s.copy(onSurface = Color(it), onBackground = Color(it)) }
    t.secondaryText?.let { s = s.copy(onSurfaceVariant = Color(it)) }
    // Background recolours the whole backdrop — every surface/container level — so surfaces that use an
    // elevated container (e.g. the dialog card = surfaceContainerHigh) follow it too. Applied BEFORE
    // card so Card can still set the elevated card/dialog surfaces to a different shade.
    t.background?.let { Color(it).let { c -> s = s.copy(
        background = c, surface = c, surfaceVariant = c,
        surfaceContainerLowest = c, surfaceContainerLow = c, surfaceContainer = c,
        surfaceContainerHigh = c, surfaceContainerHighest = c,
    ) } }
    t.card?.let { Color(it).let { c -> s = s.copy(
        surfaceContainerLow = c, surfaceContainer = c, surfaceContainerHigh = c, surfaceContainerHighest = c,
    ) } }
    t.danger?.let { Color(it).let { c -> s = s.copy(
        error = c, onError = onColorFor(c),
        errorContainer = containerFor(c, isDark), onErrorContainer = onContainerFor(c, isDark),
    ) } }
    t.success?.let { Color(it).let { c -> s = s.copy(
        tertiary = c, onTertiary = onColorFor(c),
        tertiaryContainer = containerFor(c, isDark), onTertiaryContainer = onContainerFor(c, isDark),
    ) } }
    t.highlight?.let { Color(it).let { c -> s = s.copy(
        primaryContainer = c, onPrimaryContainer = onColorFor(c),
    ) } }
    return s
}

/** Wrap [content] in a nested theme carrying [surface]'s overrides on top of the global app theme. */
@Composable
fun ThemedSurface(surface: AppSurface, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val initial = remember(surface) {
        runBlocking { SurfaceThemeStore.from(context.dataStore.data.first(), surface) }
    }
    val flow = remember(surface) { context.dataStore.data.map { SurfaceThemeStore.from(it, surface) } }
    val theme by flow.collectAsState(initial = initial)

    if (!theme.hasAnyOverride) {
        content()
        return
    }

    val base = MaterialTheme.colorScheme
    val isDark = base.background.luminance() < 0.5f
    val scheme = base.applySurface(theme, isDark)

    val family: FontFamily? = theme.fontFamily?.let { fam ->
        if (fam.isEmpty()) FontFamily.Default else composeFontFamily(context, fam) ?: FontFamily.Default
    }
    val weight = theme.fontWeight?.takeIf { it in 100..1000 }?.let { FontWeight(it) }
    val typography = buildAppTypography(MaterialTheme.typography, family, weight, theme.fontScale ?: 1f)

    val border = (theme.borderWidth ?: 0f).takeIf { it > 0f }?.let { w ->
        theme.borderColor?.let { BorderStroke(w.dp, Color(it)) }
    }

    CompositionLocalProvider(
        LocalSurfaceBorder provides border,
        LocalDialogButtonStyles provides theme.buttons,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
