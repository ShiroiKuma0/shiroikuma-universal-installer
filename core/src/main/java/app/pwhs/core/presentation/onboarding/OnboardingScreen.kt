package app.pwhs.core.presentation.onboarding

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import app.pwhs.core.R
import app.pwhs.core.util.DeviceCompat
import app.pwhs.core.util.PermissionMonitor
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.data.local.SharedPrefsKeys
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where a free VirusTotal API key comes from — the same URL the scanner error message cites. */
private const val VIRUSTOTAL_API_KEY_URL = "https://www.virustotal.com/gui/my-apikey"

/** A VirusTotal API key is a 64-character hex string; used only to catch a half-finished paste. */
private val VIRUSTOTAL_API_KEY_FORMAT = Regex("[0-9a-fA-F]{64}")

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    /** Renders a two-option security picker under the description. */
    val securityPicker: Boolean = false,
    /** Renders the VirusTotal API key field under the action button. */
    val virusTotalKeyField: Boolean = false,
    /** Renders the anonymous-reporting opt-out switch under the description. */
    val analyticsToggle: Boolean = false,
    /** Optional secondary action rendered under the description (e.g. "Open Developer options"). */
    val actionLabel: String? = null,
    val actionIcon: ImageVector? = null,
    val onAction: (() -> Unit)? = null,
)

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

@Composable
private fun PageContent(
    page: OnboardingPage,
    isPermissionPage: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    strictSecurity: Boolean = false,
    onStrictSecurityChange: (Boolean) -> Unit = {},
    virusTotalKey: String = "",
    onVirusTotalKeyChange: (String) -> Unit = {},
    analyticsEnabled: Boolean = true,
    onAnalyticsEnabledChange: (Boolean) -> Unit = {},
) {
    // Centered while it fits, scrollable once the keyboard takes half the screen away.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (page.securityPicker) {
                Spacer(Modifier.height(28.dp))
                OnboardingSecurityPicker(
                    strict = strictSecurity,
                    onChange = onStrictSecurityChange,
                )
            }

            if (page.analyticsToggle) {
                Spacer(Modifier.height(28.dp))
                OnboardingAnalyticsToggle(
                    enabled = analyticsEnabled,
                    onChange = onAnalyticsEnabledChange,
                )
            }

            // The key block draws its own link, so the shared button would only duplicate it.
            if (!page.virusTotalKeyField) {
                page.onAction?.let { action ->
                    val label = page.actionLabel ?: return@let
                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(onClick = action) {
                        page.actionIcon?.let { icon ->
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(label)
                    }
                }
            }

            // The key goes in right here, beside the link that fetches it. The page used to send the
            // user to Settings to paste it, which is where the flow lost people.
            if (page.virusTotalKeyField) {
                Spacer(Modifier.height(24.dp))
                VirusTotalKeySetup(
                    value = virusTotalKey,
                    onValueChange = onVirusTotalKeyChange,
                    getKeyLabel = page.actionLabel.orEmpty(),
                    onGetKey = page.onAction ?: {},
                )
            }

            // Permission button on last page
            if (isPermissionPage) {
                Spacer(Modifier.height(32.dp))
                if (hasPermission) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.onboarding_permission_granted),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    OutlinedButton(onClick = onRequestPermission) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_grant_permission))
                    }
                }
            }
        }
    }
}

/**
 * The VirusTotal API key field shown during onboarding, with the link that produces a key.
 *
 * Every keystroke is persisted, so there is no Save button to miss — walking away mid-paste still
 * leaves the key stored. The 64-hex check is advisory: it catches a truncated paste without
 * refusing a key format VirusTotal might change later.
 */
@Composable
private fun VirusTotalKeySetup(
    value: String,
    onValueChange: (String) -> Unit,
    getKeyLabel: String,
    onGetKey: () -> Unit,
) {
    val context = LocalContext.current
    val trimmed = value.trim()
    val looksValid = VIRUSTOTAL_API_KEY_FORMAT.matches(trimmed)
    val malformed = trimmed.isNotEmpty() && !looksValid

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboarding_virustotal_key_label)) },
            trailingIcon = {
                IconButton(onClick = { pasteFromClipboard(context)?.let(onValueChange) }) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        contentDescription = stringResource(R.string.onboarding_virustotal_paste),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            singleLine = true,
            isError = malformed,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            supportingText = {
                Text(
                    text = stringResource(
                        when {
                            trimmed.isEmpty() -> R.string.onboarding_virustotal_key_hint
                            looksValid -> R.string.onboarding_virustotal_key_saved
                            else -> R.string.onboarding_virustotal_key_malformed
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
        if (getKeyLabel.isNotEmpty()) {
            TextButton(onClick = onGetKey) {
                Text(getKeyLabel)
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun pasteFromClipboard(context: Context): String? {
    val clipboard = context.getSystemService<ClipboardManager>() ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}


/**
 * The reporting opt-out, presented on rather than off.
 *
 * A switch in a card rather than a segmented picker: this is one thing you turn off, not a
 * choice between two modes, and the row has to make the "off" path as easy to hit as "next".
 */
@Composable
private fun OnboardingAnalyticsToggle(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_analytics_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.onboarding_analytics_switch_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

/**
 * Normal vs Strict, offered during onboarding.
 *
 * Worth asking here rather than defaulting silently: Strict changes the install screen for every
 * install afterwards, and it needs a VirusTotal API key to be useful at all. Someone who never
 * gets a key should not be left with Scan sitting in the primary button forever.
 *
 * Uses the same segmented control as Settings > Advanced, so the two screens offering this choice
 * look like the same control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingSecurityPicker(
    strict: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val options = listOf(
        false to R.string.onboarding_security_normal,
        true to R.string.onboarding_security_strict,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, labelRes) ->
                SegmentedButton(
                    selected = value == strict,
                    onClick = { if (value != strict) onChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                if (strict) R.string.onboarding_security_strict_sub else R.string.onboarding_security_normal_sub
            ),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
