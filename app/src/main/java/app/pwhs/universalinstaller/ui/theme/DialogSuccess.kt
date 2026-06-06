package app.pwhs.universalinstaller.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Install-dialog success-badge override for the current surface: the circle colour and thickness (the
 * yellow ring width in dp) and the tick (checkmark) colour and thickness (its stroke width in dp). A
 * null field inherits the corresponding [SuccessBadge] default. The inner disc behind the tick is
 * always [SuccessBadge.Background] (black). Provided by [ThemedSurface], consumed by the success-stage
 * badge.
 */
@Immutable
data class DialogSuccessStyle(
    val circle: Int? = null,
    val tick: Int? = null,
    val circleThickness: Float? = null,
    val tickThickness: Float? = null,
)

/** Success-badge style for the current surface (install dialog); default = inherit the defaults below. */
val LocalDialogSuccessStyle = staticCompositionLocalOf { DialogSuccessStyle() }

/** Fixed defaults for the success badge: yellow circle/tick, black inner disc, thin ring & tick. */
object SuccessBadge {
    val DefaultColor = Color(0xFFFFEB3B)      // yellow — used when circle/tick colour is not overridden
    val Background = Color(0xFF000000)        // black inner disc behind the tick
    const val DefaultCircleThickness = 3f     // dp — yellow ring width when not overridden
    const val DefaultTickThickness = 4f       // dp — checkmark stroke width when not overridden
}
