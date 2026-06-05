package app.pwhs.universalinstaller.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Native look of a dialog button, used to pick sensible defaults before applying overrides. */
enum class DialogButtonKind { Filled, Outlined, Tonal }

/** Per-button overrides for the current surface (install dialog), keyed by slot; empty = all default. */
val LocalDialogButtonStyles = staticCompositionLocalOf<Map<String, ButtonStyle>> { emptyMap() }

/**
 * A dialog action button whose background / content colour / border / font can be individually overridden
 * per [slot] via the 白い熊 Installer UI page. Rendered explicitly (Box/Row + Modifier) rather than via the
 * Material Button composables, because those don't reliably reflect per-instance colour/border on this
 * Compose version. Falls back to [kind]'s defaults (or [defaultContainer]/[defaultContent], e.g. the
 * downgrade-red Install button) when nothing is set.
 */
@Composable
fun DialogActionButton(
    slot: String,
    kind: DialogButtonKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    defaultContainer: Color? = null,
    defaultContent: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val style = LocalDialogButtonStyles.current[slot]
    val context = LocalContext.current

    val baseContainer = defaultContainer ?: when (kind) {
        DialogButtonKind.Filled -> MaterialTheme.colorScheme.primary
        DialogButtonKind.Tonal -> MaterialTheme.colorScheme.secondaryContainer
        DialogButtonKind.Outlined -> Color.Transparent
    }
    val baseContent = defaultContent ?: when (kind) {
        DialogButtonKind.Filled -> MaterialTheme.colorScheme.onPrimary
        DialogButtonKind.Tonal -> MaterialTheme.colorScheme.onSecondaryContainer
        DialogButtonKind.Outlined -> MaterialTheme.colorScheme.primary
    }
    val container = style?.bg?.let { Color(it) } ?: baseContainer
    val contentColor = style?.content?.let { Color(it) } ?: baseContent

    val defaultBorder = if (kind == DialogButtonKind.Outlined) BorderStroke(1.dp, contentColor) else null
    val border = run {
        val w = style?.borderWidth
        val c = style?.borderColor
        if (w != null && w > 0f && c != null) BorderStroke(w.dp, Color(c)) else defaultBorder
    }

    val family = style?.fontFamily?.let {
        if (it.isEmpty()) FontFamily.Default else composeFontFamily(context, it) ?: FontFamily.Default
    }
    val weight = style?.fontWeight?.let { FontWeight(it) }
    val scale = style?.fontScale
    val shape = MaterialTheme.shapes.medium
    val baseTextStyle = MaterialTheme.typography.labelLarge

    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rowScope = this
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides baseTextStyle.copy(
                color = contentColor,
                fontFamily = family ?: baseTextStyle.fontFamily,
                fontWeight = weight ?: baseTextStyle.fontWeight,
                fontSize = if (scale != null) baseTextStyle.fontSize * scale else baseTextStyle.fontSize,
            ),
        ) {
            with(rowScope) { content() }
        }
    }
}
