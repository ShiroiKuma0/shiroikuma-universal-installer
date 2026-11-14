package app.pwhs.universalinstaller.presentation.install.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R

/**
 * Sends the file to 応用管理 for the full VirusTotal scan and report (see `VtHandoff`). Null when
 * the installed 応用管理 has no scan entry — the pill is then simply not there. Provided by
 * `DialogInstallActivity`, read where the VirusTotal status is shown.
 */
val LocalFullScanAction = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The "Full scan" action: an outlined pill, never filled — a regular action, 1.25 dp frame. */
@Composable
fun FullScanPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.25.dp, accent), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.GppGood,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.apk_info_vt_full_scan),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(11.dp),
        )
    }
}

/** Shown in place of the VirusTotal status once a reviewed 応用管理 hand-off was honoured. */
@Composable
fun VtReviewedChip(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(0.5.dp, accent.copy(alpha = 0.6f)), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.GppGood,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.apk_info_vt_reviewed),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}
