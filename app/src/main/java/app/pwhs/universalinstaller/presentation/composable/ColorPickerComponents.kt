package app.pwhs.universalinstaller.presentation.composable

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import kotlin.math.roundToInt

/** Curated accent seeds shown as swatches on the 白い熊 Installer UI page. */
val AccentPalette: List<Color> = listOf(
    Color(0xFFFF7043), // brand orange
    Color(0xFFE53935), // red
    Color(0xFFD81B60), // pink
    Color(0xFF8E24AA), // purple
    Color(0xFF5E35B1), // deep purple
    Color(0xFF3949AB), // indigo
    Color(0xFF1E88E5), // blue
    Color(0xFF00ACC1), // cyan
    Color(0xFF00897B), // teal
    Color(0xFF43A047), // green
    Color(0xFFFDD835), // yellow
    Color(0xFFFB8C00), // amber
    Color(0xFF6D4C41), // brown
    Color(0xFF546E7A), // blue-grey
)

private fun Color.toHex(): String = String.format("#%08X", toArgb())

/**
 * RGB + T (alpha) + hex colour picker. The "T" slider is the alpha channel: 0 = fully transparent,
 * 255 = fully opaque. [onPick] returns the chosen colour (caller stores its ARGB int, alpha included).
 *
 * [recents] (most-recent first) and the curated palette are shown as one-touch hotpicks above the
 * sliders — tapping one immediately picks it. [onInherit], when non-null, adds an "Inherit (global)"
 * action (used by per-surface overrides).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
    recents: List<Int> = emptyList(),
    onInherit: (() -> Unit)? = null,
) {
    val argb0 = remember(initial) { initial.toArgb() }
    var r by remember { mutableIntStateOf((argb0 shr 16) and 0xFF) }
    var g by remember { mutableIntStateOf((argb0 shr 8) and 0xFF) }
    var b by remember { mutableIntStateOf(argb0 and 0xFF) }
    var a by remember { mutableIntStateOf((argb0 shr 24) and 0xFF) }

    val current = Color(red = r, green = g, blue = b, alpha = a)
    var hexText by remember { mutableStateOf(current.toHex()) }

    // Slider moves → refresh the hex field.
    LaunchedEffect(r, g, b, a) { hexText = current.toHex() }

    // Recently-used first, then the curated palette; deduped. One touch picks immediately.
    val hotpicks = remember(recents) {
        (recents + AccentPalette.map { it.toArgb() }).distinct().take(18)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_color_picker_title)) },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hotpicks.forEach { argb ->
                        ColorSwatch(color = Color(argb), selected = false, onClick = { onPick(Color(argb)) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Preview over a solid backdrop so the alpha channel is visible.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Box(Modifier.fillMaxSize().background(current))
                }
                Spacer(Modifier.height(16.dp))
                LabeledSlider(stringResource(R.string.ui_channel_r), r.toFloat(), 0f..255f) { r = it.roundToInt() }
                LabeledSlider(stringResource(R.string.ui_channel_g), g.toFloat(), 0f..255f) { g = it.roundToInt() }
                LabeledSlider(stringResource(R.string.ui_channel_b), b.toFloat(), 0f..255f) { b = it.roundToInt() }
                LabeledSlider(stringResource(R.string.ui_channel_t), a.toFloat(), 0f..255f) { a = it.roundToInt() }
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        runCatching { AndroidColor.parseColor(text.trim()) }.getOrNull()?.let { parsed ->
                            a = (parsed shr 24) and 0xFF
                            r = (parsed shr 16) and 0xFF
                            g = (parsed shr 8) and 0xFF
                            b = parsed and 0xFF
                        }
                    },
                    label = { Text(stringResource(R.string.ui_hex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = {
            Row {
                if (onInherit != null) {
                    TextButton(onClick = onInherit) { Text(stringResource(R.string.ui_inherit)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A round, tappable accent swatch with a ring when [selected]. */
@Composable
fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(if (selected) 40.dp else 36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (color.luminanceIsLight()) Color.Black else Color.White),
            )
        }
    }
}

private fun Color.luminanceIsLight(): Boolean {
    val argb = toArgb()
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return (0.299f * r + 0.587f * g + 0.114f * b) > 0.6f
}
