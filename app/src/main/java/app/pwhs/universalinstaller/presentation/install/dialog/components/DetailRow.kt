package app.pwhs.universalinstaller.presentation.install.dialog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.ui.theme.dialogTextStyle

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = dialogTextStyle("detail_label", MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Text(
            text = value,
            style = dialogTextStyle("detail_value", MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
