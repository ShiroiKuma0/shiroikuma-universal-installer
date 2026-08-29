@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package app.pwhs.universalinstaller.presentation.setting.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.ui.res.stringResource
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.telemetry.Telemetry
import app.pwhs.universalinstaller.presentation.setting.components.SearchableItem
import app.pwhs.universalinstaller.presentation.setting.components.SwitchPreference
import app.pwhs.universalinstaller.presentation.setting.components.matchesQuery
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.ExperimentalFoundationApi

internal fun LazyListScope.PrivacySection(
    q: String,
    privacyLabels: List<String>,
    analyticsEnabled: Boolean,
    onAnalyticsEnabledChanged: (Boolean) -> Unit
) {
    if (Telemetry.isCollecting && matchesQuery(q, privacyLabels)) item {
        SettingsSection(title = stringResource(R.string.setting_section_privacy), icon = Icons.Rounded.PrivacyTip) {
            SearchableItem(q, stringResource(R.string.setting_analytics_title), "privacy analytics crash data") {
                SwitchPreference(
                    title = stringResource(R.string.setting_analytics_title),
                    subtitle = stringResource(R.string.setting_analytics_subtitle),
                    checked = analyticsEnabled,
                    onCheckedChange = onAnalyticsEnabledChanged,
                )
            }
        }
    }
}
