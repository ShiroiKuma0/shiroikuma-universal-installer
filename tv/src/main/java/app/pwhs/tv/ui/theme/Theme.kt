package app.pwhs.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.domain.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
)

// ── Accent presets ──────────────────────────────────────
// Parity with the mobile app's 5 accent presets (shared AppThemePreset + the same `theme_preset`
// dataStore key), so a preference set on the phone carries to the TV. Only the accent roles are
// overridden; the navy/slate surfaces stay constant so contrast holds in every preset.
@OptIn(ExperimentalTvMaterial3Api::class)
private fun ColorScheme.withAccent(dark: Boolean, preset: AppThemePreset): ColorScheme = when (preset) {
    // 白い熊 yellow — the fork default, shared with the phone through the `theme_preset` key.
    AppThemePreset.Yellow -> if (dark) copy(
        primary = Color(0xFFFFEB3B), primaryContainer = Color(0xFF3B3600), onPrimaryContainer = Color(0xFFFFEB3B),
        secondary = Color(0xFFFFD54F), secondaryContainer = Color(0xFF3B3600), tertiary = Color(0xFFFFD54F),
    ) else copy(
        primary = Color(0xFF8A7A00), primaryContainer = Color(0xFFFFF7B2), onPrimaryContainer = Color(0xFF3B3600),
        secondary = Color(0xFF8A7A00), secondaryContainer = Color(0xFFFFF7B2), tertiary = Color(0xFFB28900),
    )
    AppThemePreset.Orange -> this
    AppThemePreset.Blue -> if (dark) copy(
        primary = Color(0xFF38BDF8), primaryContainer = Color(0xFF0369A1), onPrimaryContainer = Color(0xFFE0F2FE),
        secondary = Color(0xFF2DD4BF), secondaryContainer = Color(0xFF0F766E), tertiary = Color(0xFF818CF8),
    ) else copy(
        primary = Color(0xFF0284C7), primaryContainer = Color(0xFFE0F2FE), onPrimaryContainer = Color(0xFF0369A1),
        secondary = Color(0xFF0F766E), secondaryContainer = Color(0xFFCCFBF1), tertiary = Color(0xFF6366F1),
    )
    AppThemePreset.Green -> if (dark) copy(
        primary = Color(0xFF4ADE80), primaryContainer = Color(0xFF15803D), onPrimaryContainer = Color(0xFFDCFCE7),
        // cyan-700 (not cyan-600) so the inherited light onSecondaryContainer clears WCAG 3:1.
        secondary = Color(0xFF22D3EE), secondaryContainer = Color(0xFF0E7490), tertiary = Color(0xFFFDE047),
    ) else copy(
        primary = Color(0xFF16A34A), primaryContainer = Color(0xFFDCFCE7), onPrimaryContainer = Color(0xFF14532D),
        secondary = Color(0xFF0891B2), secondaryContainer = Color(0xFFECFEFF), tertiary = Color(0xFFCA8A04),
    )
    AppThemePreset.Red -> if (dark) copy(
        primary = Color(0xFFFCA5A5), primaryContainer = Color(0xFFB91C1C), onPrimaryContainer = Color(0xFFFEE2E2),
        secondary = Color(0xFFF59E0B), secondaryContainer = Color(0xFF78350F), tertiary = Color(0xFF60A5FA),
    ) else copy(
        primary = Color(0xFFDC2626), primaryContainer = Color(0xFFFEE2E2), onPrimaryContainer = Color(0xFF7F1D1D),
        secondary = Color(0xFFD97706), secondaryContainer = Color(0xFFFEF3C7), tertiary = Color(0xFF2563EB),
    )
    AppThemePreset.Purple -> if (dark) copy(
        primary = Color(0xFFC084FC), primaryContainer = Color(0xFF6B21A8), onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFFF472B6), secondaryContainer = Color(0xFF831843), tertiary = Color(0xFF34D399),
    ) else copy(
        primary = Color(0xFF7C3AED), primaryContainer = Color(0xFFEDE9FE), onPrimaryContainer = Color(0xFF2E1065),
        secondary = Color(0xFFDB2777), secondaryContainer = Color(0xFFFCE7F3), tertiary = Color(0xFF059669),
    )
}

/** A vivid swatch color for the accent picker (independent of light/dark). */
fun accentSwatchColor(preset: AppThemePreset): Color = when (preset) {
    AppThemePreset.Yellow -> Color(0xFFFFEB3B)
    AppThemePreset.Orange -> Color(0xFFEA580C)
    AppThemePreset.Blue -> Color(0xFF0284C7)
    AppThemePreset.Green -> Color(0xFF16A34A)
    AppThemePreset.Red -> Color(0xFFDC2626)
    AppThemePreset.Purple -> Color(0xFF7C3AED)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UniversalInstallerTheme(
    themeMode: ThemeMode = ThemeMode.System,
    themePreset: AppThemePreset = AppThemePreset.Yellow,
    content: @Composable () -> Unit,
) {
    val isInDarkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val base = if (isInDarkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = base.withAccent(isInDarkTheme, themePreset)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
            content()
        }
    }
}
