package app.pwhs.universalinstaller.presentation.install

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.pwhs.universalinstaller.IntentHandoff
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ExternalOpenMode
import app.pwhs.universalinstaller.domain.model.InstallUiStyle
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.SecurityLevel
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.util.PermissionMonitor
import app.pwhs.universalinstaller.presentation.install.dialog.DialogFailedContent
import app.pwhs.universalinstaller.presentation.install.dialog.DialogInstallingContent
import app.pwhs.universalinstaller.presentation.install.dialog.DialogMenuContent
import app.pwhs.universalinstaller.presentation.install.dialog.DialogPrepareContent
import app.pwhs.universalinstaller.presentation.install.dialog.DialogSuccessContent
import app.pwhs.universalinstaller.presentation.install.dialog.InstallRisk
import app.pwhs.universalinstaller.presentation.install.dialog.RiskConfirmDialog
import app.pwhs.universalinstaller.ui.theme.AppSurface
import app.pwhs.universalinstaller.ui.theme.LocalSurfaceBorder
import app.pwhs.universalinstaller.ui.theme.ThemedSurface
import app.pwhs.universalinstaller.presentation.install.dialog.detectInstallRisks
import app.pwhs.universalinstaller.ui.theme.UniversalInstallerTheme
import app.pwhs.universalinstaller.util.SystemIntentInstaller
import app.pwhs.universalinstaller.util.SystemInstallerFallback
import app.pwhs.universalinstaller.util.LocaleHelper
import app.pwhs.universalinstaller.util.WindowBlurEffect
import app.pwhs.universalinstaller.util.extension.getDisplayName
import app.pwhs.universalinstaller.presentation.install.dialog.DialogMotion
import app.pwhs.universalinstaller.presentation.install.dialog.PositionDialog
import app.pwhs.universalinstaller.presentation.install.dialog.dialogInnerWidget
import app.pwhs.universalinstaller.presentation.install.dialog.generateDialogParams
import app.pwhs.universalinstaller.presentation.install.dialog.LoadingContent
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.solrudev.ackpine.splits.ApkSplits.validate
import ru.solrudev.ackpine.splits.SplitPackage.Companion.toSplitPackage
import ru.solrudev.ackpine.splits.ZippedApkSplits
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import app.pwhs.core.domain.ThemeMode
import app.pwhs.core.domain.AppThemePreset
import app.pwhs.universalinstaller.ui.theme.ForkUiDefaults
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Translucent activity that shows a focused install dialog when an external app (file
 * manager, Chrome, Telegram, share sheet) opens an APK / APKS / XAPK / APKM. This activity
 * owns the install intent filters directly — there is no router activity in front, so the
 * user goes straight from their file picker into the dialog with no flash of our app
 * chrome.
 *
 * Architecturally inspired by InstallerX's `InstallerActivity` pattern (read for approach,
 * not copied — they're GPL):
 * - File-open intent filters live on this activity, not on the launcher activity.
 * - `singleInstance` + `excludeFromRecents` keep this off the recents stack.
 * - `Theme.UniversalInstaller.Dialog` is translucent so the calling app stays visible
 *   behind our scrim.
 * - The dialog content transitions through stages: Loading → Prepare → Menu → Installing → Result.
 *
 * Dismissal paths:
 * - Tap outside the card → dismiss. Detected via stacked `pointerInput` blocks (outer
 *   scrim Box dispatches dismiss; inner Surface consumes taps).
 * - Cancel button → same.
 * - Install button → starts install; dialog shows progress then result.
 */
@OptIn(ExperimentalMaterial3Api::class)
class DialogInstallActivity : ComponentActivity() {

    private val viewModel: InstallViewModel by viewModel()
    private val installNotifier: InstallProgressNotifier by inject()
    private val promptNotifier: InstallPromptNotifier by inject()

    companion object {
        /** Set by [InstallPromptNotifier]: a parse waiting in [PendingInstallStore]. */
        const val EXTRA_PENDING_ID = "pending_install_id"

        /**
         * With [EXTRA_PENDING_ID]: show the dialog instead of installing. This is the
         * notification body tap — "let me look first" — as opposed to its Install action.
         */
        const val EXTRA_PENDING_SHOW_UI = "pending_install_show_ui"

        /** How long to wait for the real session id before giving up on the progress notification. */
        private const val SESSION_ID_TIMEOUT_MS = 3_000L

        /** Headless parse budget. Exceeding it falls back to the dialog rather than hanging. */
        private const val PARSE_TIMEOUT_MS = 30_000L

        /**
         * One grep-able tag for the whole notification-install path. The flow crosses an activity,
         * a store, a notifier and a broadcast receiver, so "which branch did it take" is otherwise
         * not answerable from a log.
         */
        private const val LOG = "NotifInstall"

        /** All four corners: the sheet floats inside the window rather than meeting its edge. */
        private val FLOATING_SHEET_SHAPE = RoundedCornerShape(28.dp)
    }

    // POST_NOTIFICATIONS gates the background-install notification on Android 13+. We ask
    // on first open of the dialog (not at Background-tap time) so the user has already
    // decided before they need the notification.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled at notification post time via canPost() */ }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Track whether system took us to a confirmation activity. */
    private var wentToSystemConfirm = false

    /**
     * Decide which of the two failures the user is looking at.
     *
     * A SecurityException or a missing file means the bytes never arrived — the sending app's
     * grant expired, or the file moved. Anything else got past reading and fell over on the
     * package itself. Blaming the package for the first case is what the old single message did.
     */
    private fun reportParseProblem(cause: Throwable) {
        val unreadable = cause is SecurityException ||
            cause is FileNotFoundException ||
            cause is IOException
        val detail = cause.message.orEmpty()
        if (unreadable) viewModel.dialogReadFailed(detail) else viewModel.dialogParseFailed(detail)
    }

    /** Whether Android will let us install at all. False means the install would fail instantly. */
    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            packageManager.canRequestPackageInstalls()

    private fun openInstallPermissionSettings() {
        PermissionMonitor.start(this) { packageManager.canRequestPackageInstalls() }
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName"),
            )
        )
    }

    /**
     * The parts that make a sheet a sheet rather than a lowered dialog: a drag handle to grab, and
     * content kept clear of the gesture bar so the action row is not sitting on the system inset.
     *
     * Mirrors what InstallerX Revived's sheet does — its own action row carries
     * `navigationBarsPadding()` plus extra bottom padding under gesture navigation.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SheetChrome(enabled: Boolean, content: @Composable () -> Unit) {
        if (!enabled) {
            content()
            return
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BottomSheetDefaults.DragHandle()
            content()
        }
    }

    /**
     * Slides the sheet up on first composition. The dialog style is left untouched — it already
     * arrives with the activity's own window animation, and a second one on top reads as a stutter.
     */
    @Composable
    private fun SheetEntryAnimation(enabled: Boolean, content: @Composable () -> Unit) {
        if (!enabled) {
            content()
            return
        }
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            content()
        }
    }

    /**
     * True when the parse was restored from [PendingInstallStore] instead of read from the
     * intent. The dialog must not re-parse in that case: the source URI's read grant died with
     * the activity that received it, and re-parsing an archive from its extracted members would
     * silently lose the split set.
     */
    private var skipInitialParse = false

    /**
     * Flips the headless path back to the visible dialog. Set when a notification prompt cannot
     * be posted or the parse yields nothing — the install must still be answerable.
     */
    private val forceDialogUi = MutableStateFlow(false)

    private fun fallbackToDialog() {
        forceDialogUi.value = true
    }

    /**
     * The Install action on a prompt notification. No window is drawn — the install is fired and
     * handed to [InstallProgressNotifier], which is already the app's background-install
     * reporter, then this activity goes away.
     *
     * Runs through [InstallViewModel.confirmInstall] rather than talking to a controller
     * directly, so the blacklist gate, profile flags and targeted-user handling all apply exactly
     * as they do from the dialog.
     */
    private fun installFromNotificationAction() {
        Timber.i("$LOG: firing install from the notification action")
        viewModel.confirmInstall(trackDialogTarget = true)
        lifecycleScope.launch {
            // confirmInstall publishes the real session id asynchronously (ackpine mints it).
            // The install itself runs in the process-scoped appScope, so a timeout here costs
            // the progress notification, never the install.
            val target = withTimeoutOrNull(SESSION_ID_TIMEOUT_MS) {
                viewModel.dialogTarget.filterNotNull().first()
            }
            if (target != null) {
                installNotifier.track(
                    sessionId = target.sessionId,
                    packageName = target.packageName,
                    appName = target.appName,
                    iconPath = target.iconPath,
                )
            } else {
                Timber.w("No session id within ${SESSION_ID_TIMEOUT_MS}ms — install continues untracked")
            }
            viewModel.clearDialogTarget()
            finish()
        }
    }

    private suspend fun readInstallUiStyle(): InstallUiStyle = runCatching {
        InstallUiStyle.from(dataStore.data.first()[PreferencesKeys.INSTALL_UI_STYLE])
    }.getOrDefault(InstallUiStyle.Dialog)

    private suspend fun readExternalOpenMode(): ExternalOpenMode = runCatching {
        ExternalOpenMode.from(dataStore.data.first()[PreferencesKeys.EXTERNAL_OPEN_MODE])
    }.getOrDefault(ExternalOpenMode.Dialog)

    /**
     * The no-window path for [ExternalOpenMode.Notification] / [ExternalOpenMode.AutoNotification]: parse,
     * then either ask in a notification or install straight away. Nothing is ever drawn, so the
     * app the user opened the file from stays in front the whole time.
     *
     * A composable only because it needs to observe the parse; it emits no UI.
     */
    @Composable
    private fun HeadlessNotificationInstall(mode: ExternalOpenMode, uri: Uri) {
        val context = LocalContext.current
        LaunchedEffect(uri) {
            Timber.i("$LOG: mode=$mode, parsing $uri")
            runCatching { parseAndPush(context, uri) }.onFailure { e ->
                Timber.e(e, "$LOG: parse threw - giving up")
                finish()
                return@LaunchedEffect
            }

            val apkInfo = withTimeoutOrNull(PARSE_TIMEOUT_MS) {
                viewModel.uiState.map { it.pendingApkInfo }.filterNotNull().first()
            }
            if (apkInfo == null) {
                Timber.w("$LOG: no parse result within ${PARSE_TIMEOUT_MS}ms - falling back to the dialog")
                skipInitialParse = true
                fallbackToDialog()
                return@LaunchedEffect
            }

            Timber.i("$LOG: parsed ${apkInfo.packageName}, ${apkInfo.splitEntries.size} split(s)")

            // Auto mode installs without asking, but not past a risk the user has never seen.
            // A downgrade or a signature mismatch still gets the prompt.
            val risks = detectInstallRisks(apkInfo)
            if (mode == ExternalOpenMode.AutoNotification && risks.isEmpty()) {
                Timber.i("$LOG: auto mode, no risks - installing without asking")
                installFromNotificationAction()
                return@LaunchedEffect
            }

            // Checked before stashing: a prompt that cannot be posted would strand the install
            // with nothing on screen, and the dialog is the only fallback left.
            if (risks.isNotEmpty()) Timber.i("$LOG: ${risks.size} risk(s) - asking rather than auto-installing")
            val canPost = promptNotifier.canPost()
            if (!canPost) Timber.w("$LOG: notifications unavailable")
            val entry = if (canPost) viewModel.stashPendingInstall() else null
            if (canPost && entry == null) Timber.w("$LOG: nothing to stash - staging the APKs must have failed")
            if (entry == null || !promptNotifier.prompt(entry)) {
                Timber.w("$LOG: falling back to the dialog")
                entry?.let {
                    viewModel.restorePendingInstall(it)
                    PendingInstallStore.consume(it.id)
                }
                skipInitialParse = entry != null
                fallbackToDialog()
                return@LaunchedEffect
            }
            Timber.i("$LOG: prompt ${entry.id} posted for ${entry.packageName}, ${entry.apkUris.size} staged uri(s)")
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()

        // Arriving from an install-prompt notification: the package was parsed by an earlier
        // instance of this activity, so there is no intent URI to read — the parse is in the
        // store. Handled before the URI check for exactly that reason.
        val pendingId = intent.getStringExtra(EXTRA_PENDING_ID)
        var restoredEntry: PendingInstallStore.Entry? = null
        if (pendingId != null) {
            val entry = PendingInstallStore.consume(pendingId)
            promptNotifier.cancel(pendingId)
            Timber.i("$LOG: resuming $pendingId, found=${entry != null}")
            if (entry == null) {
                // The process died while the prompt sat in the shade, taking the parse with it.
                // Nothing installable is left, so don't pretend otherwise.
                Timber.w("Install prompt $pendingId is stale — parse no longer in memory")
                finish()
                return
            }
            viewModel.restorePendingInstall(entry)
            if (!intent.getBooleanExtra(EXTRA_PENDING_SHOW_UI, false)) {
                installFromNotificationAction()
                return
            }
            restoredEntry = entry
        }

        val incomingUris = restoredEntry?.let { listOf(it.apkUris.first()) }
            ?: collectIncomingUris(intent)
        if (incomingUris.isEmpty()) {
            Timber.w("DialogInstallActivity launched without any content URIs — bailing")
            finish()
            return
        }

        if (incomingUris.size > 1) {
            // Multiple URIs → redirect to full app for batch install
            IntentHandoff.postBatch(incomingUris)
            val targetIntent = Intent(this, InstallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            forwardIncomingUris(intent, targetIntent)
            startActivity(targetIntent)
            finish()
            return
        }

        val incomingUri = incomingUris.first()

        // Start in Loading stage
        viewModel.dialogStartLoading()
        skipInitialParse = restoredEntry != null

        setContent {
            // Nothing is drawn until the mode is known, so the notification modes never flash a
            // dialog. The window is translucent and empty in the meantime.
            val mode by produceState<ExternalOpenMode?>(null) { value = readExternalOpenMode() }
            val forcedToDialog by forceDialogUi.collectAsState()
            val uiStyle by produceState(InstallUiStyle.Dialog) { value = readInstallUiStyle() }
            val resolvedMode = mode
            if (resolvedMode == null && !forcedToDialog) return@setContent
            // A restored parse already means the user asked to see the dialog.
            if (!forcedToDialog && restoredEntry == null && resolvedMode != ExternalOpenMode.Dialog) {
                HeadlessNotificationInstall(resolvedMode!!, incomingUri)
                return@setContent
            }

            val uiState by viewModel.uiState.collectAsState()
            val resource = LocalResources.current
            val context = LocalContext.current
            val isApk = remember(incomingUri) {
                val displayName = context.contentResolver.getDisplayName(incomingUri)
                val ext = displayName.substringAfterLast('.', "").lowercase()
                ext == "apk"
            }

            val obbPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                uris.forEach { viewModel.attachObbFile(context, it) }
            }

            // Dispatch parsing once. Keyed on the URI so a config-change recomposition
            // doesn't re-parse — the VM's pendingApkInfo state survives the recomposition.
            LaunchedEffect(incomingUri) {
                if (skipInitialParse) return@LaunchedEffect
                runCatching { parseAndPush(context, incomingUri) }.onFailure { e ->
                    Timber.e(e, "Parse failed for $incomingUri")
                    // Was a toast and finish(): the dialog vanished and the user was told
                    // "unsupported file" even when the real problem was that we never got to
                    // read it. Show which of the two it was, and stay on screen to say so.
                    reportParseProblem(e)
                }
            }


            // Dialog target snapshot — set inside confirmInstall before the install fires.
            // We watch this + the session list to drive Installing → Success/Failed transitions.
            val dialogTarget by viewModel.dialogTarget.collectAsState()

            // Auto-open-after-install pref — read directly from DataStore (no SettingViewModel
            // dependency in this activity). Drives the Success-stage countdown.
            val prefs by context.dataStore.data.collectAsState(initial = null)
            val autoOpenAfterInstall = prefs?.get(PreferencesKeys.AUTO_OPEN_AFTER_INSTALL) ?: false
            val autoConfirmExternalInstall = prefs?.get(PreferencesKeys.AUTO_CONFIRM_EXTERNAL_INSTALL) ?: false
            val strictVirusTotalCheck = SecurityLevel.from(
                stored = prefs?.get(PreferencesKeys.SECURITY_LEVEL),
                legacyStrict = prefs?.get(PreferencesKeys.STRICT_VIRUSTOTAL_CHECK) ?: false,
            ) == SecurityLevel.Strict

            // Tracks whether we've actually observed the captured session in the repository.
            // The session is added inside controller.install() AFTER createSession() suspends,
            // so there's a window where dialogTarget is set but the session isn't in the list
            // yet — without this guard we'd misread that as "session removed" and fire Success
            // immediately, dismissing the dialog while ackpine hasn't finished installing.
            var sessionEverSeen by remember(dialogTarget?.sessionId) { mutableStateOf(false) }

            // Resolve Installing → Success / Failed by watching the captured session.
            //   - session in list, error blank        → still Installing (mark as seen)
            //   - session in list, error non-blank   → Failed
            //   - session NOT in list (was there)    → Succeeded
            //   - session NOT in list (never seen)   → not started yet, keep waiting
            // BaseInstallController removes on Succeeded and calls setError() on Failed.
            LaunchedEffect(dialogTarget, uiState.sessions, uiState.dialogStage) {
                val target = dialogTarget ?: return@LaunchedEffect
                if (uiState.dialogStage !is DialogStage.Installing) return@LaunchedEffect
                val session = uiState.sessions.find { it.id == target.sessionId }
                if (session != null) {
                    sessionEverSeen = true
                    val msg = session.error.resolve(this@DialogInstallActivity)
                    if (msg.isNotBlank()) {
                        viewModel.dialogInstallFailed(msg)
                    }
                } else if (sessionEverSeen) {
                    viewModel.dialogInstallSuccess()
                }
            }

            // Risk-gate state — when non-empty we render the consent AlertDialog over the
            // main install Dialog. Confirming proceeds with the install; cancelling returns
            // the user to the Prepare/Menu stage (no install fires).
            var pendingRisks by remember { mutableStateOf<List<InstallRisk>>(emptyList()) }
            val proceedInstall = {
                // Show the Installing stage straight away. dialogTarget arrives a moment
                // later (after createSession suspends), at which point the Installing UI
                // re-renders with the real session data and progress bar.
                viewModel.dialogStartInstalling()
                viewModel.confirmInstall(trackDialogTarget = true)
            }
            val handleInstallTap = {
                val info = uiState.pendingApkInfo
                val risks = if (info != null) detectInstallRisks(info, strictVirusTotalCheck) else emptyList()
                when {
                    // Checked before anything else: without this the install starts, fails
                    // somewhere inside the system installer, and the failure gets reported as if
                    // the package were at fault.
                    !canInstallPackages() -> viewModel.dialogPermissionRequired()
                    risks.isNotEmpty() -> pendingRisks = risks
                    else -> proceedInstall()
                }
            }

            // Transition from Loading → Prepare when parse completes
            LaunchedEffect(uiState.pendingApkInfo, uiState.dialogStage) {
                if (uiState.pendingApkInfo != null && uiState.dialogStage == DialogStage.Loading) {
                    viewModel.dialogShowPrepare()
                }
            }

            // Auto-confirm logic for external intents (auto-install from Prepare stage)
            LaunchedEffect(uiState.dialogStage, autoConfirmExternalInstall) {
                if (uiState.dialogStage == DialogStage.Prepare && autoConfirmExternalInstall) {
                    handleInstallTap()
                } else if (uiState.dialogStage == DialogStage.Success && autoConfirmExternalInstall) {
                    viewModel.dialogClose()
                    finish()
                }
            }

            // Any dismiss path (Background button, Cancel, outside-tap, back press) while an
            // install is in flight must hand the session off to the process-scoped notifier so
            // the install doesn't continue silently. dialogTarget is set the moment the install
            // is fired, so its presence is the marker.
            val handoffInstall = {
                val t = dialogTarget
                val stage = uiState.dialogStage
                if (t != null && (stage is DialogStage.Installing || stage is DialogStage.None)) {
                    installNotifier.track(
                        sessionId = t.sessionId,
                        packageName = t.packageName,
                        appName = t.appName,
                        iconPath = t.iconPath,
                    )
                }
            }

            BackHandler {
                handoffInstall()
                viewModel.dismissPendingInstall()
                viewModel.dialogClose()
                viewModel.clearDialogTarget()
                finish()
            }

            // Fork defaults (ForkUiDefaults), same as Preferences.toAppThemeState() — the install
            // dialog reads the raw keys itself, so it has to agree with the rest of the app.
            val themeModeName = prefs?.get(stringPreferencesKey("theme_mode")) ?: ForkUiDefaults.Mode.name
            val themeMode = ThemeMode.entries.find { it.name == themeModeName } ?: ForkUiDefaults.Mode
            val dynamicColor = prefs?.get(booleanPreferencesKey("dynamic_color")) ?: ForkUiDefaults.DynamicColor
            val amoledMode = prefs?.get(booleanPreferencesKey("amoled_mode")) ?: ForkUiDefaults.Amoled
            val presetName = prefs?.get(stringPreferencesKey("theme_preset")) ?: ForkUiDefaults.Preset.name
            val themePreset = AppThemePreset.entries.find { it.name == presetName } ?: ForkUiDefaults.Preset

            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            UniversalInstallerTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                amoledMode = amoledMode,
                themePreset = themePreset
            ) {
                ThemedSurface(AppSurface.Dialog) {
                val configuration = LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                val maxDialogHeight = screenHeight * 0.8f

                // Window level blur (Android 12+)
                WindowBlurEffect(enabled = true)

                if (pendingRisks.isNotEmpty()) {
                    RiskConfirmDialog(
                        risks = pendingRisks,
                        onConfirm = {
                            pendingRisks = emptyList()
                            proceedInstall()
                        },
                        onCancel = {
                            pendingRisks = emptyList()
                            handoffInstall()
                            viewModel.dismissPendingInstall()
                            viewModel.dialogClose()
                            viewModel.clearDialogTarget()
                            finish()
                        },
                        onPrivilegedUninstall = viewModel::uninstallConflictingApp,
                        onExistingAppUninstalled = {
                            viewModel.onConflictingAppUninstalled()
                            pendingRisks = pendingRisks.filterNot {
                                it is InstallRisk.SignatureMismatch
                            }
                        },
                    )
                }

                // Dialog and sheet differ only in where the same card sits and what shape it
                // takes. The stage content below is identical for both — see [InstallUiStyle].
                val isSheet = uiStyle == InstallUiStyle.Sheet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                handoffInstall()
                                viewModel.dismissPendingInstall()
                                viewModel.dialogClose()
                                viewModel.clearDialogTarget()
                                finish()
                            })
                        },
                    contentAlignment = if (isSheet) Alignment.BottomCenter else Alignment.Center
                ) {
                  SheetEntryAnimation(enabled = isSheet) {
                    Surface(
                        modifier = (if (isSheet) {
                            // Floating rather than flush: inset from the edges and rounded on all
                            // four corners, the way InstallerX Revived's miuix sheet sits. A sheet
                            // glued to the bottom edge leaves two square corners against the
                            // gesture bar, which is what prompted this.
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .heightIn(max = screenHeight * 0.9f)
                        } else {
                            Modifier
                                .padding(24.dp)
                                .widthIn(max = 480.dp)
                                .heightIn(max = maxDialogHeight)
                        })
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { /* consume clicks */ })
                            },
                        // A sheet is attached to the edge, not floating over the screen: it takes
                        // the sheet container colour and no drop shadow. Lowering a dialog card to
                        // the bottom without this still reads as a dialog.
                        shape = if (isSheet) FLOATING_SHEET_SHAPE else AlertDialogDefaults.shape,
                        color = if (isSheet) BottomSheetDefaults.ContainerColor else AlertDialogDefaults.containerColor,
                        border = LocalSurfaceBorder.current,
                        tonalElevation = if (isSheet) BottomSheetDefaults.Elevation else AlertDialogDefaults.TonalElevation,
                        shadowElevation = if (isSheet) 0.dp else 12.dp,
                    ) {
                      SheetChrome(enabled = isSheet) {
                        val params = generateDialogParams(
                            uiState = uiState,
                            dialogTarget = dialogTarget,
                            autoOpenAfterInstall = autoOpenAfterInstall,
                            onInstall = handleInstallTap,
                            onCancel = {
                                handoffInstall()
                                viewModel.dismissPendingInstall()
                                viewModel.dialogClose()
                                viewModel.clearDialogTarget()
                                finish()
                            },
                            onMenu = viewModel::dialogShowMenu,
                            onUnblock = viewModel::unblockPackage,
                            onMenuBack = viewModel::dialogBackToPrepare,
                            onGrantInstallPermission = { openInstallPermissionSettings() },
                            onCheckVirusTotal = {
                                viewModel.scanVirusTotal(this@DialogInstallActivity)
                            },
                            onRemoveObb = { obb -> viewModel.removeAttachedObb(obb.uri) },
                            onToggleSplit = viewModel::toggleSplit,
                            onAttachObb = { obbPickerLauncher.launch(arrayOf("*/*")) },
                            onBackground = {
                                handoffInstall()
                                viewModel.dialogClose()
                                viewModel.clearDialogTarget()
                                finish()
                            },
                            onOpenInstalledApp = { pkg ->
                                viewModel.getAppLaunchIntent(pkg)?.let { startActivity(it) }
                                viewModel.dialogClose()
                                viewModel.clearDialogTarget()
                                finish()
                            },
                            onCloseAfterResult = {
                                viewModel.dialogClose()
                                viewModel.clearDialogTarget()
                                finish()
                            },
                            onRetry = {
                                // Not proceedInstall(): confirmInstall has already consumed the
                                // parsed state by now, so re-running it just reported "couldn't
                                // parse the package" instantly. #110
                                viewModel.retryDialogInstall()
                            },
                            onToggleAllUsers = viewModel::setAllUsers,
                            onSelectUserId = { viewModel.setUserId(it) },
                            onSkipParse = if (isApk) {
                                { viewModel.skipParseAndInstallSingle() }
                            } else null,
                            onFallbackInstall = {
                                val apkUri = dialogTarget?.apkUri
                                if (isApk && apkUri != null) {
                                    // Explicit component, not a bare ACTION_VIEW: once the user
                                    // has made us the default installer, the mime type resolves
                                    // back to this very activity and the button did nothing at
                                    // all. #110
                                    val intent = SystemIntentInstaller.createInstallIntent(context, apkUri)
                                    val launched = intent != null && runCatching { startActivity(intent) }
                                        .onFailure { Timber.e(it, "Failed to launch system installer fallback") }
                                        .isSuccess
                                    if (!launched) {
                                        Toast.makeText(
                                            context,
                                            getString(R.string.dialog_fallback_install_unavailable),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    viewModel.dialogClose()
                                    viewModel.clearDialogTarget()
                                    finish()
                                }
                            }.takeIf { isApk && dialogTarget?.apkUri != null },
                        )

                        PositionDialog(
                            modifier = if (uiState.dialogStage != DialogStage.Menu) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier
                            },
                            centerIcon = dialogInnerWidget(params.icon),
                            centerTitle = dialogInnerWidget(params.title),
                            centerSubtitle = dialogInnerWidget(params.subtitle),
                            centerText = dialogInnerWidget(params.text),
                            centerContent = dialogInnerWidget(params.content),
                            centerButton = dialogInnerWidget(params.buttons)
                        )
                      }
                    }
                  }
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uris = collectIncomingUris(intent)
        if (uris.isEmpty()) return

        if (uris.size > 1) {
            // Multiple URIs → redirect to full app for batch install
            IntentHandoff.postBatch(uris)
            val targetIntent = Intent(this, InstallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            forwardIncomingUris(intent, targetIntent)
            startActivity(targetIntent)
            finish()
            return
        }

        val uri = uris.first()
        viewModel.dismissPendingInstall()
        viewModel.dialogStartLoading()
        val context = this
        // Re-parse new intent
        lifecycleScope.launch {
            runCatching { parseAndPush(context, uri) }.onFailure { e ->
                Timber.e(e, "Parse failed for new intent $uri")
                reportParseProblem(e)
            }
        }
    }

    /**
     * Re-grant any content URIs we received on [source] to [target] so the install activity's
     * task can read them. Required because we launch [target] with `NEW_TASK | CLEAR_TASK` and
     * `finish()` ourselves — the original grant only covered DialogInstallActivity's task.
     */
    private fun forwardIncomingUris(source: Intent?, target: Intent) {
        if (source == null) return
        val uris = collectIncomingUris(source)
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            target.data = uris.first()
        } else {
            val clip = ClipData.newRawUri("", uris.first())
            for (i in 1 until uris.size) {
                clip.addItem(ClipData.Item(uris[i]))
            }
            target.clipData = clip
        }
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun onStop() {
        super.onStop()
        // Don't auto-finish if we went to system's install confirm dialog.
        // This prevents the dialog from disappearing when system shows confirmation.
    }

    /**
     * Parse the incoming URI through the same SplitPackage pipeline InstallScreen uses,
     * then hand to the shared VM so the install logic (split picker, VT scan, OBB) works
     * identically to the full-screen flow.
     */
    private fun parseAndPush(context: Context, uri: Uri) {
        val displayName = context.contentResolver.getDisplayName(uri)
        val mime = context.contentResolver.getType(uri)?.lowercase()
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val isApkMime = mime == "application/vnd.android.package-archive"
        val splitProvider = when {
            isApkMime || ext == "apk" -> SingletonApkSequence(uri, context).toSplitPackage()
            ext in setOf("apks", "xapk", "apkm", "apk+", "zip") ->
                ZippedApkSplits.getApksForUri(uri, context)
                    .validate()
                    .toSplitPackage()
                    .filterCompatible(context)
            else -> SingletonApkSequence(uri, context).toSplitPackage()
        }
        viewModel.parseApkInfo(context, uri, splitProvider, displayName)
    }

    /**
     * Pull all installable URIs off the launch intent. If multiple URIs are found,
     * onCreate/onNewIntent will redirect to the full app's batch flow.
     */
    private fun collectIncomingUris(source: Intent?): List<Uri> {
        if (source == null) return emptyList()
        val out = mutableListOf<Uri>()

        // 1. Data URI (VIEW / INSTALL_PACKAGE)
        source.data?.takeIf { it.scheme == "content" || it.scheme == "file" }?.let(out::add)

        // 2. EXTRA_STREAM (SEND / SEND_MULTIPLE)
        @Suppress("DEPRECATION")
        when (source.action) {
            Intent.ACTION_SEND ->
                (source.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let(out::add)
            Intent.ACTION_SEND_MULTIPLE ->
                source.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.filterNotNull()
                    ?.let(out::addAll)
        }

        // 3. ClipData (Alternative for some file managers)
        source.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                val u = clip.getItemAt(i).uri ?: continue
                if (u.scheme == "content" || u.scheme == "file") out.add(u)
            }
        }

        return out.distinct()
    }
}
