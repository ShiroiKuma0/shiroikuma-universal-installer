package app.pwhs.universalinstaller.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified

/** Per-text-category overrides for the current surface (install dialog); empty = all default. */
val LocalDialogTextStyles = staticCompositionLocalOf<Map<String, TextStyleOverride>> { emptyMap() }

/**
 * The effective [TextStyle] for a dialog text [category]: applies any user override (colour / font /
 * weight / size) on top of [base], falling back to [defaultColor] (the element's normal colour) when
 * no colour override is set. Pass the element's usual colour as [defaultColor] and drop its separate
 * `color =` argument, so the override can take effect.
 */
@Composable
fun dialogTextStyle(category: String, base: TextStyle, defaultColor: Color = Color.Unspecified): TextStyle {
    val resolvedDefault = if (defaultColor != Color.Unspecified) defaultColor else base.color
    val o = LocalDialogTextStyles.current[category]
        ?: return if (resolvedDefault != base.color) base.copy(color = resolvedDefault) else base

    val context = LocalContext.current
    val family = o.fontFamily?.let {
        if (it.isEmpty()) FontFamily.Default else composeFontFamily(context, it) ?: FontFamily.Default
    }
    return base.copy(
        color = o.color?.let { Color(it) } ?: resolvedDefault,
        fontFamily = family ?: base.fontFamily,
        fontWeight = o.fontWeight?.let { FontWeight(it) } ?: base.fontWeight,
        fontSize = if (o.fontScale != null && base.fontSize.isSpecified) base.fontSize * o.fontScale else base.fontSize,
    )
}
