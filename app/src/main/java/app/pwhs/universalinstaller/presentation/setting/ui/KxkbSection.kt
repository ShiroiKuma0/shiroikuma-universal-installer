package app.pwhs.universalinstaller.presentation.setting.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.ui.theme.InstallerBadgeDefaults

/**
 * kxkb-style page furniture (mirrors 白い熊's kxkb "Keyboard UI" page): section headings are
 * accent-coloured with an underline exactly as wide as the heading text, sub-headings get a
 * thinner short underline, and sections are separated by a full-width hairline spacer.
 */

/** Warning red for "not set" states (真 red — M3's dark-theme error is a washed-out pink). */
val KxkbWarnRed = Color(0xFFFF5252)

/** A top-level section: a thin full-width spacer above (except the first), then the underlined
 *  heading, then the section content — flat, no card. */
@Composable
fun KxkbSectionFrame(
    title: String,
    first: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (!first) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.primary)
        }
        // width(IntrinsicSize.Max) + single-line text sizes the column — and therefore the
        // divider — to exactly the heading's width (Min would clip to the widest word).
        Column(
            Modifier
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
                .width(IntrinsicSize.Max),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

/** A sub-heading one level below a section: accent text with a short text-wide underline. */
@Composable
fun KxkbSubHeader(text: String, indent: Int = 72) {
    Column(
        Modifier
            .padding(start = indent.dp, top = 12.dp, end = 16.dp, bottom = 2.dp)
            .width(IntrinsicSize.Max),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary)
    }
}

/** A black/yellow selector chip: black fill, accent label, accent border (thicker when selected). */
@Composable
fun KxkbChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else null) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Black,
            labelColor = MaterialTheme.colorScheme.primary,
            selectedContainerColor = Color.Black,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.primary,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.5.dp,
        ),
    )
}

/** An Arcanechat-style round pill button: black fill, thin yellow border, yellow label. */
@Composable
fun PillButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, InstallerBadgeDefaults.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black,
            contentColor = InstallerBadgeDefaults.Content,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text(text)
    }
}
