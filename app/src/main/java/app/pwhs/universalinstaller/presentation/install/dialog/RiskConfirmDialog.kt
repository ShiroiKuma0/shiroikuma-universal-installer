package app.pwhs.universalinstaller.presentation.install.dialog

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.VtStatus

/**
 * A risk the user must explicitly confirm before installing. We surface only items
 * the user can actually act on — VT status flags get filtered to MALICIOUS/SUSPICIOUS;
 * informational states (CLEAN, SCANNING, NOT_FOUND, etc.) don't trigger this gate.
 */
sealed interface InstallRisk {
    /** The APK's versionCode is lower than the installed package's. */
    data class Downgrade(
        val installedVersionName: String,
        val newVersionName: String,
    ) : InstallRisk

    /**
     * The installed app is signed with a different key. Unlike the others this one cannot be
     * accepted and pushed through — Android will refuse the install until the old copy is gone.
     */
    data class SignatureMismatch(val packageName: String) : InstallRisk

    /** VirusTotal flagged the APK as malicious — N engines reported a threat. */
    data class VtMalicious(val engineCount: Int, val sha256: String = "") : InstallRisk

    /** VirusTotal flagged the APK as suspicious. */
    data class VtSuspicious(val engineCount: Int, val sha256: String = "") : InstallRisk

    /** Strict mode: APK was not scanned by VirusTotal. */
    data object VtUnscanned : InstallRisk
}

/**
 * True when installing [apkInfo] would replace a newer already-installed version.
 *
 * Shared with the install path on purpose: whether we *warn* about a downgrade and whether we
 * *allow* one must be the same question, or the app ends up asking for consent it then ignores.
 */
fun isDowngrade(apkInfo: ApkInfo): Boolean {
    val installedCode = apkInfo.installedVersionCode ?: return false
    return installedCode > 0 && apkInfo.versionCode < installedCode
}

fun detectInstallRisks(apkInfo: ApkInfo, strictVirusTotal: Boolean = false): List<InstallRisk> {
    val risks = mutableListOf<InstallRisk>()
    if (isDowngrade(apkInfo)) {
        risks += InstallRisk.Downgrade(
            installedVersionName = apkInfo.installedVersionName.orEmpty().ifBlank { "?" },
            newVersionName = apkInfo.versionName.ifBlank { "?" },
        )
    }
    // Only `true` counts. `null` means the check couldn't run and must not raise an alarm.
    if (apkInfo.signatureMismatch == true) {
        risks += InstallRisk.SignatureMismatch(apkInfo.packageName)
    }
    when (val status = apkInfo.vtResult?.status) {
        VtStatus.MALICIOUS -> risks += InstallRisk.VtMalicious(apkInfo.vtResult.malicious, apkInfo.sha256)
        VtStatus.SUSPICIOUS -> risks += InstallRisk.VtSuspicious(apkInfo.vtResult.suspicious, apkInfo.sha256)
        VtStatus.CLEAN -> Unit
        null -> if (strictVirusTotal) risks += InstallRisk.VtUnscanned
        else -> if (strictVirusTotal) risks += InstallRisk.VtUnscanned
    }
    return risks
}

@Composable
fun RiskConfirmDialog(
    risks: List<InstallRisk>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    if (risks.isEmpty()) return
    val severe = risks.any { it is InstallRisk.VtMalicious }
    val titleRes = if (severe) R.string.dialog_risk_title_severe else R.string.dialog_risk_title_warn
    val proceedRes = if (severe) R.string.dialog_risk_proceed_severe else R.string.dialog_risk_proceed_warn

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = if (severe) Icons.Rounded.Security else Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(titleRes),
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                risks.forEach { risk -> RiskRow(risk) }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dialog_risk_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(proceedRes),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.dialog_cancel_btn))
            }
        },
    )
}

@Composable
private fun RiskRow(risk: InstallRisk) {
    val (icon: ImageVector, line: String) = when (risk) {
        is InstallRisk.Downgrade -> Icons.Rounded.Warning to
            stringResource(R.string.dialog_risk_downgrade, risk.installedVersionName, risk.newVersionName)
        is InstallRisk.SignatureMismatch -> Icons.Rounded.Key to
            stringResource(R.string.dialog_risk_signature_mismatch)
        is InstallRisk.VtMalicious -> Icons.Rounded.Security to
            stringResource(R.string.dialog_risk_vt_malicious, risk.engineCount)
        is InstallRisk.VtSuspicious -> Icons.Rounded.Warning to
            stringResource(R.string.dialog_risk_vt_suspicious, risk.engineCount)
        is InstallRisk.VtUnscanned -> Icons.Rounded.Warning to
            stringResource(R.string.dialog_risk_vt_unscanned)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        RiskAction(risk)
    }
}

/**
 * The way out of this particular risk, when there is one. Rendered under its own row so each
 * problem carries its own remedy rather than leaving "Install anyway" as the only button.
 *
 * Resolved from LocalContext here rather than hoisted, so both InstallScreen and
 * DialogInstallActivity get the actions without either having to pass anything in.
 */
@Composable
private fun RiskAction(risk: InstallRisk) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val (labelRes, onClick) = when (risk) {
        is InstallRisk.SignatureMismatch -> R.string.dialog_risk_action_uninstall_existing to {
            // ACTION_DELETE shows the platform's own uninstall confirmation — we are not
            // removing anyone's app behind their back, and it needs no extra permission.
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_DELETE, "package:${risk.packageName}".toUri())
                )
            }
            Unit
        }
        is InstallRisk.VtMalicious ->
            if (risk.sha256.isBlank()) return
            else R.string.dialog_risk_action_view_report to {
                uriHandler.openUri("$VT_FILE_REPORT_URL${risk.sha256}")
            }
        is InstallRisk.VtSuspicious ->
            if (risk.sha256.isBlank()) return
            else R.string.dialog_risk_action_view_report to {
                uriHandler.openUri("$VT_FILE_REPORT_URL${risk.sha256}")
            }
        // Downgrade is consented to right here and carried into the session; unscanned has no
        // action beyond running the scan, which the install screen already offers.
        is InstallRisk.Downgrade, InstallRisk.VtUnscanned -> return
    }
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(start = 26.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
    }
}

private const val VT_FILE_REPORT_URL = "https://www.virustotal.com/gui/file/"
