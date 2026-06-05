package app.pwhs.universalinstaller.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.ui.theme.availableFontOptions
import app.pwhs.universalinstaller.ui.theme.composeFontFamily

/**
 * Lists every available font with its name drawn **in its own typeface**, plus an "Add font…" action.
 * Compose port of the sister forks' FontPickerDialog.
 */
@Composable
fun FontPickerDialog(
    onDismiss: () -> Unit,
    onAddFont: () -> Unit,
    onPick: (fileName: String) -> Unit,
    // When non-null, an "Inherit (global)" row is shown first (used by per-surface overrides).
    onInherit: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val options = remember { context.availableFontOptions() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.theme_font),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (onInherit != null) {
                        Text(
                            text = stringResource(R.string.ui_inherit),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInherit() }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                    options.forEach { option ->
                        Text(
                            text = option.displayName,
                            fontFamily = composeFontFamily(context, option.fileName) ?: FontFamily.Default,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(option.fileName) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.theme_add_font),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddFont() }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
