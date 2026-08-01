package app.pwhs.universalinstaller.presentation.install.dialog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.ui.theme.dialogTextStyle

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = dialogTextStyle("detail_label", MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant),
        )
        // Weighted and wrapping rather than one ellipsised line: a long version name
        // (`6.3.0-alpha.2026-07-30.g5c0ed6a3+002`) is exactly the value worth reading in full,
        // and it used to be cut off mid-string here.
        Text(
            text = value,
            style = dialogTextStyle("detail_value", MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), MaterialTheme.colorScheme.onSurface),
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f, fill = false),
        )
    }
}
