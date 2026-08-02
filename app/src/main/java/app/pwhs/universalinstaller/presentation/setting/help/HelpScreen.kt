package app.pwhs.universalinstaller.presentation.setting.help

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R

private data class HelpTopic(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
)

/**
 * Plain-language explanations of the settings, reachable from Settings → Help (issue #102: a Play
 * review called the settings "very sophisticated").
 *
 * Deliberately not a copy of the switch subtitles. Those say what a toggle *is*; these say when to
 * reach for it and what it cannot do — the gaps that actually generate bug reports, like expecting
 * "Replace existing" to cover a downgrade, or expecting a spoofed install source to fool Play.
 */
private val TOPICS = listOf(
    HelpTopic(Icons.Rounded.SettingsApplications, R.string.help_modes_title, R.string.help_modes_body),
    HelpTopic(Icons.Rounded.Tune, R.string.help_conflict_title, R.string.help_conflict_body),
    HelpTopic(Icons.Rounded.AdminPanelSettings, R.string.help_profiles_title, R.string.help_profiles_body),
    HelpTopic(Icons.Rounded.Security, R.string.help_virustotal_title, R.string.help_virustotal_body),
    HelpTopic(Icons.Rounded.DeleteSweep, R.string.help_delete_title, R.string.help_delete_body),
    HelpTopic(Icons.Rounded.Key, R.string.help_source_title, R.string.help_source_body),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? Activity

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            items(TOPICS, key = { it.titleRes }) { topic -> TopicCard(topic) }
        }
    }
}

@Composable
private fun TopicCard(topic: HelpTopic) {
    // Saveable so an expanded topic survives rotation — long bodies are annoying to re-open.
    var expanded by rememberSaveable(topic.titleRes) { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = topic.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(topic.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(topic.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 34.dp),
                )
            }
        }
    }
}
