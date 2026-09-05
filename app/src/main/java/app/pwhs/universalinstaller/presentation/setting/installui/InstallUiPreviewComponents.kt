package app.pwhs.universalinstaller.presentation.setting.installui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ExternalOpenMode
import app.pwhs.universalinstaller.domain.model.InstallUiStyle

/**
 * A phone-shaped frame with the install card drawn where it will actually land, or a notification
 * shade line when the chosen mode never draws a card.
 */
@Composable
internal fun InstallUiPreview(mode: ExternalOpenMode, style: InstallUiStyle) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 150.dp, height = 264.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (mode) {
                    ExternalOpenMode.Notification,
                    ExternalOpenMode.AutoNotification -> MiniNotification(
                        modifier = Modifier.align(Alignment.TopCenter),
                        accent = accent,
                    )

                    ExternalOpenMode.Dialog -> MiniCard(
                        modifier = Modifier.align(
                            if (style == InstallUiStyle.Sheet) Alignment.BottomCenter else Alignment.Center,
                        ),
                        widthFraction = if (style == InstallUiStyle.Sheet) 1f else 0.86f,
                        accent = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCard(modifier: Modifier, widthFraction: Float, accent: Color) {
    Column(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MiniLine(0.7f, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        MiniLine(1f, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        Spacer(Modifier.height(2.dp))
        MiniLine(0.45f, accent, height = 7.dp)
    }
}

@Composable
private fun MiniNotification(modifier: Modifier, accent: Color) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent),
        )
        Spacer(Modifier.width(6.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniLine(0.8f, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            MiniLine(1f, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun MiniLine(widthFraction: Float, color: Color, height: Dp = 4.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

/**
 * Centered or bottom, as two thumbnails.
 */
@Composable
internal fun CardPositionPicker(
    current: InstallUiStyle,
    onChange: (InstallUiStyle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            InstallUiStyle.Dialog to R.string.setting_install_ui_style_dialog,
            InstallUiStyle.Sheet to R.string.setting_install_ui_style_sheet,
        ).forEach { (style, labelRes) ->
            PositionThumbnail(
                style = style,
                label = stringResource(labelRes),
                selected = style == current,
                onClick = { if (style != current) onChange(style) },
            )
        }
    }
}

@Composable
private fun PositionThumbnail(
    style: InstallUiStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(width = 84.dp, height = 112.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(9.dp),
                contentAlignment = if (style == InstallUiStyle.Sheet) {
                    Alignment.BottomCenter
                } else {
                    Alignment.Center
                },
            ) {
                MiniCard(
                    modifier = Modifier,
                    widthFraction = if (style == InstallUiStyle.Sheet) 1f else 0.82f,
                    accent = accent,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
