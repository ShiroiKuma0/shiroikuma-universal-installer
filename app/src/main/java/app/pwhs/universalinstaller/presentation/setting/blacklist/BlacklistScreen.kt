package app.pwhs.universalinstaller.presentation.setting.blacklist

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.EmptyStateView
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import app.pwhs.universalinstaller.util.AppIconData
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

/**
 * The never-install list, on its own screen.
 *
 * It lived as a collapsed section inside Settings, buried between Sync and Advanced — reaching it
 * meant scrolling past everything and knowing to expand it. It is a list you occasionally review,
 * which is a screen, not a fold-out.
 *
 * Review-and-remove only: there is no way to type a package name in. Nobody recalls
 * `com.example.app` from memory, and a typo silently blocks nothing. Adding happens where the
 * user can see what they are blocking — Manage → an app → Block from installing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val activity = LocalContext.current as? Activity
    val blocked by viewModel.blacklist.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setting_blacklist_title)) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (blocked.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.Block,
                title = stringResource(R.string.setting_blacklist_empty),
                subtitle = stringResource(R.string.setting_blacklist_empty_hint),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.setting_blacklist_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            items(blocked, key = { it }) { pkg ->
                BlockedRow(pkg = pkg, onRemove = { viewModel.removeFromBlacklist(pkg) })
            }
        }
    }
}

@Composable
private fun BlockedRow(pkg: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    // Resolve the label so the row reads as an app rather than a string. A blocked package is
    // often *not* installed — that is the point — so fall back to the package name.
    val label = remember(pkg) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(label ?: pkg) },
            supportingContent = if (label != null) {
                { Text(pkg, style = MaterialTheme.typography.bodySmall) }
            } else null,
            leadingContent = {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(AppIconData(pkg)).build(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    error = rememberVectorPainter(Icons.Rounded.Block),
                    fallback = rememberVectorPainter(Icons.Rounded.Block),
                )
            },
            trailingContent = {
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.setting_blacklist_remove))
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
