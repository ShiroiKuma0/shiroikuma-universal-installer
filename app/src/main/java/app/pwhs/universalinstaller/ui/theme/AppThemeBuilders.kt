package app.pwhs.universalinstaller.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Builders used by [UniversalInstallerTheme] to apply the "白い熊 Installer UI" customizations on top
 * of the base [Typography]/[ExpressiveShapes]/[ColorScheme].
 */

/** A typography with [family]/[weight] overridden (when non-null) and every size scaled by [scale]. */
fun buildAppTypography(
    base: Typography,
    family: FontFamily?,
    weight: FontWeight?,
    scale: Float,
): Typography {
    if (family == null && weight == null && scale == 1f) return base

    fun TextStyle.tuned(): TextStyle = copy(
        fontFamily = family ?: fontFamily,
        fontWeight = weight ?: fontWeight,
        fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
    )

    return Typography(
        displayLarge = base.displayLarge.tuned(),
        displayMedium = base.displayMedium.tuned(),
        displaySmall = base.displaySmall.tuned(),
        headlineLarge = base.headlineLarge.tuned(),
        headlineMedium = base.headlineMedium.tuned(),
        headlineSmall = base.headlineSmall.tuned(),
        titleLarge = base.titleLarge.tuned(),
        titleMedium = base.titleMedium.tuned(),
        titleSmall = base.titleSmall.tuned(),
        bodyLarge = base.bodyLarge.tuned(),
        bodyMedium = base.bodyMedium.tuned(),
        bodySmall = base.bodySmall.tuned(),
        labelLarge = base.labelLarge.tuned(),
        labelMedium = base.labelMedium.tuned(),
        labelSmall = base.labelSmall.tuned(),
    )
}

/** The [ExpressiveShapes] corner radii multiplied by [scale] (1f = unchanged). */
fun appShapes(scale: Float): Shapes {
    if (scale == 1f) return ExpressiveShapes
    val s = scale.coerceIn(0f, 2f)
    return Shapes(
        extraSmall = RoundedCornerShape((8 * s).dp),
        small = RoundedCornerShape((12 * s).dp),
        medium = RoundedCornerShape((16 * s).dp),
        large = RoundedCornerShape((24 * s).dp),
        extraLarge = RoundedCornerShape((28 * s).dp),
    )
}

/**
 * Recolor a scheme around a single [accent]. We override only the primary group (and inversePrimary);
 * Material derives selection/containers/ripples from these, so one color restyles the whole app
 * without pulling in material-color-utilities.
 */
fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val container = lerp(accent, if (dark) Color.Black else Color.White, if (dark) 0.6f else 0.78f)
    val onContainer = if (dark) lerp(accent, Color.White, 0.6f) else lerp(accent, Color.Black, 0.5f)
    return copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        inversePrimary = lerp(accent, if (dark) Color.White else Color.Black, 0.3f),
    )
}
