package app.pwhs.universalinstaller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.ui.theme.ExtendedColors
import app.pwhs.core.ui.theme.LocalExtendedColors
import app.pwhs.core.ui.theme.ExpressiveShapes

// Type alias & Forwarding for 100% backwards compatibility in app module
typealias ExtendedColors = ExtendedColors

val LocalExtendedColors = LocalExtendedColors
val ExpressiveShapes = ExpressiveShapes

@Composable
fun UniversalInstallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoledMode: Boolean = false,
    themePreset: AppThemePreset = AppThemePreset.Orange,
    content: @Composable () -> Unit
) {
    app.pwhs.core.ui.theme.UniversalInstallerTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        amoledMode = amoledMode,
        themePreset = themePreset,
        content = content
    )
}