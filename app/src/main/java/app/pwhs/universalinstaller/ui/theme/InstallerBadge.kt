package app.pwhs.universalinstaller.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Install-backend badge override for the current surface — the "Using Shizuku / Root / PackageInstaller"
 * pill. Overrides the pill background, the text + icon colour, and the border colour & width (in dp). A
 * null field inherits the corresponding [InstallerBadgeDefaults] value. Provided by [ThemedSurface],
 * consumed by `InstallerModeBadge`.
 */
@Immutable
data class DialogBadgeStyle(
    val background: Int? = null,
    val text: Int? = null,
    val border: Int? = null,
    val borderWidth: Float? = null,
)

/** Badge style for the current surface; default = the black/yellow 白い熊 defaults below. */
val LocalInstallerBadgeStyle = staticCompositionLocalOf { DialogBadgeStyle() }

/** Fixed 白い熊 defaults for the backend badge: black pill, yellow text + icon, thin yellow border. */
object InstallerBadgeDefaults {
    val Background = Color(0xFF000000)   // black pill
    val Content = Color(0xFFFFEB3B)      // yellow text + icon
    val Border = Color(0xFFFFEB3B)       // yellow border
    const val BorderWidth = 1.5f         // dp — border width when not overridden
}
