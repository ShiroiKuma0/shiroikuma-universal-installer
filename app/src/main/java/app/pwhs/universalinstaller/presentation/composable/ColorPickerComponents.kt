package app.pwhs.universalinstaller.presentation.composable

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableFloatStateOf
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

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

/** HSV + hex color picker. [onPick] returns the chosen color (caller stores its ARGB int). */
@Composable
fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
) {
    val hsv = remember(initial) { FloatArray(3).also { AndroidColor.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    val current = Color.hsv(hue, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    var hexText by remember { mutableStateOf(current.toHex()) }

    // Slider moves → refresh the hex field to the normalized value.
    LaunchedEffect(hue, sat, value) { hexText = current.toHex() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_color_picker_title)) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(current),
                )
                Spacer(Modifier.height(16.dp))
                LabeledSlider(stringResource(R.string.ui_hue), hue, 0f..360f) { hue = it }
                LabeledSlider(stringResource(R.string.ui_saturation), sat, 0f..1f) { sat = it }
                LabeledSlider(stringResource(R.string.ui_brightness), value, 0f..1f) { value = it }
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        runCatching { AndroidColor.parseColor(text.trim()) }.getOrNull()?.let { parsed ->
                            val out = FloatArray(3)
                            AndroidColor.colorToHSV(parsed, out)
                            hue = out[0]; sat = out[1]; value = out[2]
                        }
                    },
                    label = { Text(stringResource(R.string.ui_hex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
            .background(color),
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
