package app.pwhs.tv

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

private const val REPO_URL = "https://github.com/pass-with-high-score/universal-installer"

/**
 * Settings/About for the TV app. Every row is a focusable [Card] so the D-pad can travel
 * down the list and the [LazyColumn] scrolls to keep the focused row on screen — a
 * non-focusable list can't be scrolled with a remote.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.tv_settings_title), style = MaterialTheme.typography.headlineMedium) }

        item { SectionHeader(stringResource(R.string.tv_settings_section_about)) }
        item {
            SettingsCard(onClick = {}) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(stringResource(R.string.tv_settings_app_name), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.tv_settings_app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (version.isNotBlank()) {
                            Text(
                                stringResource(R.string.tv_settings_version, version),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.tv_settings_section_install)) }
        item {
            SettingsCard(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) {
                TitleValue(
                    stringResource(R.string.tv_settings_allow_installs_title),
                    stringResource(R.string.tv_settings_allow_installs_subtitle)
                )
            }
        }
        item {
            SettingsCard(onClick = {}) {
                TitleValue(
                    stringResource(R.string.tv_settings_receiver_title),
                    stringResource(R.string.tv_settings_receiver_subtitle)
                )
            }
        }

        item { SectionHeader(stringResource(R.string.tv_settings_section_device)) }
        item {
            SettingsCard(onClick = {}) {
                TitleValue(
                    stringResource(R.string.tv_settings_device_title),
                    stringResource(
                        R.string.tv_settings_device_info,
                        Build.MANUFACTURER,
                        Build.MODEL,
                        Build.VERSION.RELEASE,
                        Build.VERSION.SDK_INT
                    ),
                )
            }
        }

        item { SectionHeader(stringResource(R.string.tv_settings_section_project)) }
        item {
            SettingsCard(onClick = {}) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QrCode(data = REPO_URL, modifier = Modifier.size(120.dp))
                    Spacer(Modifier.width(24.dp))
                    Column {
                        Text(stringResource(R.string.tv_settings_open_source_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            REPO_URL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(R.string.tv_settings_scan_to_view),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TitleValue(title: String, value: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(
        value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}
