package app.pwhs.universalinstaller.presentation.install.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.VtStatus
import app.pwhs.universalinstaller.ui.theme.LocalExtendedColors

@Composable
fun ApkInfoFooter(
    apkInfo: ApkInfo,
    isExpanded: Boolean,
    startCompact: Boolean,
    strictSecurity: Boolean,
    confirmText: String?,
    cancelText: String?,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onCheckVirusTotal: () -> Unit,
) {
    if (isExpanded) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }

    val isDowngrade = apkInfo.installedVersionCode != null &&
        apkInfo.installedVersionCode > 0 &&
        apkInfo.versionCode < apkInfo.installedVersionCode

    // Same versionCode already installed: a re-install, not an update — see ApkInfoContent.
    val isSameVersion = apkInfo.installedVersionCode != null &&
        apkInfo.installedVersionCode > 0 &&
        apkInfo.versionCode == apkInfo.installedVersionCode
    val extendedColors = LocalExtendedColors.current

    val hasVerdict = apkInfo.vtResult?.status in setOf(
        VtStatus.CLEAN,
        VtStatus.MALICIOUS,
        VtStatus.SUSPICIOUS,
    )
    val isScanCompleted = hasVerdict || !strictSecurity
    val isScanning = apkInfo.vtResult?.status in setOf(
        VtStatus.SCANNING,
        VtStatus.UPLOADING,
        VtStatus.QUEUED,
        VtStatus.ANALYZING,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isExpanded) {
            OutlinedButton(
                onClick = onExpand,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.dialog_menu_details),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    if (cancelText == null && isExpanded && startCompact) {
                        onCollapse()
                    } else {
                        onCancel()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = cancelText ?: if (isExpanded && startCompact) {
                        stringResource(R.string.dialog_back_btn)
                    } else {
                        stringResource(R.string.cancel)
                    },
                )
            }

            if (isScanCompleted) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !apkInfo.isBlocked,
                    colors = when {
                        isDowngrade ->
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        isSameVersion -> ButtonDefaults.buttonColors(
                            containerColor = extendedColors.success,
                            contentColor = extendedColors.onSuccess,
                        )
                        else -> ButtonDefaults.buttonColors()
                    },
                ) {
                    if (confirmText == null) {
                        Icon(
                            if (isSameVersion) Icons.Rounded.Autorenew else Icons.Rounded.InstallMobile,
                            null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = confirmText ?: when {
                            isDowngrade -> stringResource(R.string.dialog_downgrade_btn)
                            isSameVersion -> stringResource(R.string.dialog_reinstall_btn)
                            else -> stringResource(R.string.txt_install)
                        },
                    )
                }
            } else {
                Button(
                    onClick = onCheckVirusTotal,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isScanning,
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.scanning_progress))
                    } else {
                        Icon(Icons.Rounded.Security, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.scan_virustotal_btn))
                    }
                }
            }
        }

        if (!isScanCompleted) {
            TextButton(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
                enabled = !apkInfo.isBlocked,
            ) {
                Text(stringResource(R.string.skip_and_install_btn))
            }
        }
    }
}
