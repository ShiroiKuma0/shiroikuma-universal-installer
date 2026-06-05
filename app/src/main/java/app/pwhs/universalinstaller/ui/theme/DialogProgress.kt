package app.pwhs.universalinstaller.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Install-dialog progress-line override (colour + thickness in dp) for the current surface; a null
 * field inherits the Material default (the accent/primary colour, 4 dp). Provided by [ThemedSurface],
 * consumed by the installing-stage progress bar.
 */
@Immutable
data class DialogProgressStyle(val color: Int? = null, val thickness: Float? = null)

/** Progress-line style for the current surface (install dialog); default = inherit Material defaults. */
val LocalDialogProgressStyle = staticCompositionLocalOf { DialogProgressStyle() }
