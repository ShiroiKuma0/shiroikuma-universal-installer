package app.pwhs.universalinstaller.presentation.install.dialog

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import app.pwhs.universalinstaller.domain.model.ExternalOpenMode
import app.pwhs.universalinstaller.presentation.install.InstallPromptNotifier
import app.pwhs.universalinstaller.presentation.install.InstallViewModel
import app.pwhs.universalinstaller.presentation.install.PendingInstallStore
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val PARSE_TIMEOUT_MS = 30_000L
private const val LOG = "NotifInstall"

@Composable
fun HeadlessNotificationInstall(
    mode: ExternalOpenMode,
    uri: Uri,
    viewModel: InstallViewModel,
    promptNotifier: InstallPromptNotifier,
    onFallbackToDialog: (skipParse: Boolean) -> Unit,
    onInstallFromNotificationAction: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(uri) {
        Timber.i("$LOG: mode=$mode, parsing $uri")
        runCatching {
            DialogInstallUriHelper.parseAndPush(context, uri, viewModel)
        }.onFailure { e ->
            Timber.e(e, "$LOG: parse threw - giving up")
            onFinish()
            return@LaunchedEffect
        }

        val apkInfo = withTimeoutOrNull(PARSE_TIMEOUT_MS) {
            viewModel.uiState.map { it.pendingApkInfo }.filterNotNull().first()
        }
        if (apkInfo == null) {
            Timber.w("$LOG: no parse result within ${PARSE_TIMEOUT_MS}ms - falling back to the dialog")
            onFallbackToDialog(true)
            return@LaunchedEffect
        }

        Timber.i("$LOG: parsed ${apkInfo.packageName}, ${apkInfo.splitEntries.size} split(s)")

        val risks = detectInstallRisks(apkInfo)
        if (mode == ExternalOpenMode.AutoNotification && risks.isEmpty()) {
            Timber.i("$LOG: auto mode, no risks - installing without asking")
            onInstallFromNotificationAction()
            return@LaunchedEffect
        }

        if (risks.isNotEmpty()) {
            Timber.i("$LOG: ${risks.size} risk(s) - asking rather than auto-installing")
        }
        val canPost = promptNotifier.canPost()
        if (!canPost) Timber.w("$LOG: notifications unavailable")
        val entry = if (canPost) viewModel.stashPendingInstall() else null
        if (canPost && entry == null) {
            Timber.w("$LOG: nothing to stash - staging the APKs must have failed")
        }
        if (entry == null || !promptNotifier.prompt(entry)) {
            Timber.w("$LOG: falling back to the dialog")
            entry?.let {
                viewModel.restorePendingInstall(it)
                PendingInstallStore.consume(it.id)
            }
            onFallbackToDialog(entry != null)
            return@LaunchedEffect
        }
        Timber.i("$LOG: prompt ${entry.id} posted for ${entry.packageName}, ${entry.apkUris.size} staged uri(s)")
        onFinish()
    }
}
