package app.pwhs.universalinstaller.presentation.setting.sections

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.presentation.setting.about.AboutActivity
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery
import app.pwhs.universalinstaller.presentation.setting.diagnostics.DiagnosticsActivity
import app.pwhs.universalinstaller.presentation.setting.help.HelpActivity
import app.pwhs.universalinstaller.util.AndroidAutoCompat

internal fun LazyListScope.AboutSection(
    q: String,
    aboutLabels: List<String>,
    context: Context,
    appVersion: String,
    onReplayTutorial: () -> Unit,
) {
    if (matchesQuery(q, aboutLabels)) item {
        SettingsSection(title = stringResource(R.string.setting_section_about), icon = Icons.Rounded.Info) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.help_title)) },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, HelpActivity::class.java))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.help_replay_tutorial)) },
                supportingContent = { Text(stringResource(R.string.help_replay_tutorial_sub)) },
                leadingContent = { Icon(Icons.Rounded.Replay, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onReplayTutorial() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_section_about)) },
                supportingContent = { Text("v$appVersion") },
                leadingContent = { Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, AboutActivity::class.java))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_diagnostics_title)) },
                leadingContent = { Icon(Icons.Rounded.BugReport, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, DiagnosticsActivity::class.java))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            val isAaInstalled = AndroidAutoCompat.isAndroidAutoInstalled(context)
            if (isAaInstalled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.aa_open_settings)) },
                    supportingContent = { Text(stringResource(R.string.aa_open_settings_sub)) },
                    leadingContent = { Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        val aaIntent = AndroidAutoCompat.getSettingsIntent(context)
                        if (aaIntent != null) {
                            runCatching { context.startActivity(aaIntent) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}
