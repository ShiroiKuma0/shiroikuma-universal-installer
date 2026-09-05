package app.pwhs.core.presentation.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.pwhs.core.R
import app.pwhs.core.data.local.SharedPrefsKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.util.DeviceCompat
import app.pwhs.core.util.PermissionMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where a free VirusTotal API key comes from — the same URL the scanner error message cites. */
private const val VIRUSTOTAL_API_KEY_URL = "https://www.virustotal.com/gui/my-apikey"

/**
 * Shared onboarding screen for both Mobile and TV.
 *
 * @param showXiaomiTip inserts an extra page warning about MIUI/HyperOS optimization silently
 *   blocking installs (issue #104). Callers gate it on [DeviceCompat.isXiaomi]; TV leaves it off
 *   because the toggle doesn't exist on Xiaomi's TV builds.
 * @param showVirusTotalTip inserts a page explaining the VirusTotal scan. Mobile-only — the
 *   scanner lives in the app module and TV has no Settings screen to paste an API key into.
 * @param showAnalyticsConsent inserts a page offering to turn anonymous reporting off. Callers
 *   pass true only on a build that has reporting to offer, which today is the phone app's `play`
 *   flavor; every other build has nothing to consent to and must not be asked.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    showXiaomiTip: Boolean = false,
    showVirusTotalTip: Boolean = false,
    showAnalyticsConsent: Boolean = false,
    onRestoreBackup: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val uriHandler = LocalUriHandler.current
    // Hide the shortcut when Developer options aren't reachable — the text alone still tells the
    // user what to look for.
    val developerOptions = remember(showXiaomiTip) {
        if (showXiaomiTip) DeviceCompat.developerOptionsIntent(context) else null
    }

    val pages = buildList {
        add(
            OnboardingPage(
                icon = Icons.Rounded.InstallMobile,
                title = stringResource(R.string.onboarding_page1_title),
                description = stringResource(R.string.onboarding_page1_desc),
            )
        )
        add(
            OnboardingPage(
                icon = Icons.Rounded.Widgets,
                title = stringResource(R.string.onboarding_page2_title),
                description = stringResource(R.string.onboarding_page2_desc),
            )
        )
        if (onRestoreBackup != null) {
            add(
                OnboardingPage(
                    icon = Icons.Rounded.Restore,
                    title = stringResource(R.string.onboarding_restore_title),
                    description = stringResource(R.string.onboarding_restore_desc),
                    actionLabel = stringResource(R.string.onboarding_restore_action),
                    actionIcon = Icons.Rounded.Restore,
                    onAction = onRestoreBackup,
                )
            )
        }
        if (showVirusTotalTip) {
            add(
                OnboardingPage(
                    icon = Icons.Rounded.GppGood,
                    title = stringResource(R.string.onboarding_virustotal_title),
                    description = stringResource(R.string.onboarding_virustotal_desc),
                    securityPicker = true,
                    virusTotalKeyField = true,
                    // Rendered as a text link inside the key block rather than the shared button,
                    // so "get a key" and "paste it here" read as one step.
                    actionLabel = stringResource(R.string.onboarding_virustotal_get_key),
                    actionIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onAction = { uriHandler.openUri(VIRUSTOTAL_API_KEY_URL) },
                )
            )
        }
        if (showXiaomiTip) {
            add(
                OnboardingPage(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.onboarding_xiaomi_title),
                    description = stringResource(R.string.onboarding_xiaomi_desc),
                    actionLabel = developerOptions?.let {
                        stringResource(R.string.onboarding_xiaomi_open_developer_options)
                    },
                    actionIcon = Icons.Rounded.Tune,
                    onAction = developerOptions?.let { intent -> { context.startActivity(intent) } },
                )
            )
        }
        if (showAnalyticsConsent) {
            add(
                OnboardingPage(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.onboarding_analytics_title),
                    description = stringResource(R.string.onboarding_analytics_desc),
                    analyticsToggle = true,
                )
            )
        }
        // Must stay last: PageContent keys the permission UI off `page == pages.lastIndex`.
        add(
            OnboardingPage(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.onboarding_page3_title),
                description = stringResource(R.string.onboarding_page3_desc),
            )
        )
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    // Normal by default — the level only becomes Strict if the user picks it here.
    var strictSecurity by remember { mutableStateOf(false) }

    // Opted in unless the user says otherwise, which is also how an absent preference reads
    // everywhere else. Seeded from the store so replaying the tour shows the current answer.
    var analyticsEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(showAnalyticsConsent) {
        if (!showAnalyticsConsent) return@LaunchedEffect
        analyticsEnabled = context.dataStore.data.first()[SharedPrefsKeys.ANALYTICS_ENABLED] ?: true
    }

    // The key the user pastes on the VirusTotal page. Seeded from whatever Settings already holds,
    // so replaying the tour doesn't look like the key was lost.
    var virusTotalKey by remember { mutableStateOf("") }
    var virusTotalKeyEdited by remember { mutableStateOf(false) }
    LaunchedEffect(showVirusTotalTip) {
        if (!showVirusTotalTip) return@LaunchedEffect
        val stored = context.dataStore.data.first()[SharedPrefsKeys.VIRUSTOTAL_API_KEY].orEmpty()
        if (!virusTotalKeyEdited) virusTotalKey = stored
    }

    // Track install permission state — refreshes on resume
    var hasInstallPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true
        )
    }

    LifecycleResumeEffect(Unit) {
        hasInstallPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
        PermissionMonitor.stop()
        onPauseOrDispose {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding(),
        ) {
            // Skip button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (pagerState.currentPage < pages.lastIndex) {
                    TextButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pages.lastIndex)
                        }
                    }) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                PageContent(
                    strictSecurity = strictSecurity,
                    onStrictSecurityChange = { strict ->
                        strictSecurity = strict
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[SharedPrefsKeys.SECURITY_LEVEL] = if (strict) "Strict" else "Normal"
                                prefs[SharedPrefsKeys.STRICT_VIRUSTOTAL_CHECK] = strict
                            }
                        }
                    },
                    virusTotalKey = virusTotalKey,
                    onVirusTotalKeyChange = { value ->
                        virusTotalKeyEdited = true
                        virusTotalKey = value
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[SharedPrefsKeys.VIRUSTOTAL_API_KEY] = value.trim()
                            }
                        }
                    },
                    analyticsEnabled = analyticsEnabled,
                    onAnalyticsEnabledChange = { enabled ->
                        analyticsEnabled = enabled
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[SharedPrefsKeys.ANALYTICS_ENABLED] = enabled
                            }
                        }
                    },
                    page = pages[page],
                    isPermissionPage = page == pages.lastIndex,
                    hasPermission = hasInstallPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")
                            )
                            if (activity != null) {
                                PermissionMonitor.start(activity) {
                                    context.packageManager.canRequestPackageInstalls()
                                }
                            }
                            context.startActivity(intent)
                        }
                    },
                )
            }

            // Page indicator + navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val color by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            animationSpec = tween(200),
                            label = "dot",
                        )
                        Surface(
                            modifier = Modifier.size(if (isSelected) 24.dp else 8.dp, 8.dp),
                            shape = CircleShape,
                            color = color,
                        ) {}
                    }
                }

                // Next / Get Started button
                if (pagerState.currentPage < pages.lastIndex) {
                    FilledTonalButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Text(stringResource(R.string.onboarding_next))
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Button(onClick = {
                        scope.launch {
                            context.dataStore.edit {
                                it[SharedPrefsKeys.ONBOARDING_COMPLETED] = true
                            }
                            onFinish()
                        }
                    }) {
                        Text(stringResource(R.string.onboarding_get_started))
                    }
                }
            }
        }
    }
}
