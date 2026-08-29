package app.pwhs.universalinstaller.presentation.install.dialog.tabs

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.VtStatus
import app.pwhs.universalinstaller.ui.theme.LocalExtendedColors
import app.pwhs.universalinstaller.presentation.install.dialog.components.MenuCard
import app.pwhs.universalinstaller.presentation.install.dialog.components.PermissionRowList
import app.pwhs.universalinstaller.presentation.install.resolvePermissionEntries

internal fun androidx.compose.foundation.lazy.LazyListScope.securityTab(
    apkInfo: ApkInfo,
    context: Context,
    onCheckVirusTotal: () -> Unit,
) {
    // 1. VirusTotal
    item(key = "virustotal") {
        val vtResult = apkInfo.vtResult
        val vtErrorMsg = vtResult?.errorMessage?.takeIf { it.isNotBlank() }
        val extendedColors = LocalExtendedColors.current
        val vtDesc = when (vtResult?.status) {
            VtStatus.CLEAN -> stringResource(R.string.apk_info_vt_clean)
            VtStatus.MALICIOUS -> stringResource(R.string.apk_info_vt_malicious, vtResult.malicious)
            VtStatus.SUSPICIOUS -> stringResource(R.string.apk_info_vt_suspicious, vtResult.suspicious)
            VtStatus.NOT_FOUND -> stringResource(R.string.apk_info_vt_not_found)
            VtStatus.SCANNING -> stringResource(R.string.apk_info_vt_scanning)
            VtStatus.UPLOADING -> stringResource(R.string.apk_info_vt_uploading, vtResult.uploadProgress)
            VtStatus.QUEUED -> stringResource(R.string.apk_info_vt_queued)
            VtStatus.ANALYZING -> stringResource(R.string.apk_info_vt_analyzing)
            VtStatus.NO_API_KEY -> stringResource(R.string.apk_info_vt_no_api_key)
            VtStatus.INVALID_API_KEY -> stringResource(R.string.apk_info_vt_invalid_key)
            VtStatus.RATE_LIMITED -> vtErrorMsg?.takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.apk_info_vt_rate_limited_retry, it) }
                ?: stringResource(R.string.apk_info_vt_rate_limited)
            VtStatus.TOO_LARGE -> stringResource(R.string.apk_info_vt_too_large, vtErrorMsg.orEmpty())
            VtStatus.ERROR -> vtErrorMsg ?: stringResource(R.string.apk_info_vt_error)
            else -> stringResource(R.string.dialog_menu_virustotal_desc)
        }
        val vtColor = when (vtResult?.status) {
            VtStatus.CLEAN -> MaterialTheme.colorScheme.tertiary
            VtStatus.MALICIOUS,
            VtStatus.ERROR -> MaterialTheme.colorScheme.error
            // NO_API_KEY / TOO_LARGE / SUSPICIOUS are "needs attention" — amber, not red,
            // and crucially not the neutral grey that made the no-key state invisible.
            VtStatus.SUSPICIOUS,
            VtStatus.NO_API_KEY,
            VtStatus.INVALID_API_KEY,
            VtStatus.RATE_LIMITED,
            VtStatus.TOO_LARGE -> extendedColors.warning
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        MenuCard(
            title = stringResource(R.string.dialog_menu_virustotal),
            description = vtDesc,
            descriptionColor = vtColor,
            icon = {
                Icon(
                    imageVector = if (vtResult?.status == VtStatus.CLEAN)
                        Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = vtColor,
                )
            },
            onClick = {
                when {
                    vtResult?.status in listOf(VtStatus.CLEAN, VtStatus.MALICIOUS, VtStatus.SUSPICIOUS) -> {
                        if (apkInfo.sha256.isNotBlank()) {
                            uriHandler.openUri("https://www.virustotal.com/gui/file/${apkInfo.sha256}/detection")
                        }
                    }
                    // Without a key, tapping Check only rewrites the same "no key" line the user
                    // is already reading. Send them where the key is entered instead.
                    vtResult?.status == VtStatus.NO_API_KEY ||
                        vtResult?.status == VtStatus.INVALID_API_KEY -> {
                        context.startActivity(
                            android.content.Intent(
                                context,
                                app.pwhs.universalinstaller.presentation.setting.SettingActivity::class.java,
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    else -> onCheckVirusTotal()
                }
            },
        )
    }

    // 2. Permissions
    if (apkInfo.permissions.isNotEmpty()) {
        item(key = "permissions") {
            var expanded by remember { mutableStateOf(true) } // Expanded by default in this tab
            val entries = remember(apkInfo.permissions) {
                resolvePermissionEntries(context, apkInfo.permissions)
            }
            val dangerousCount = entries.count { it.isDangerous }
            val description = if (dangerousCount > 0) {
                stringResource(
                    R.string.dialog_menu_permissions_breakdown,
                    dangerousCount,
                    entries.size - dangerousCount,
                )
            } else {
                stringResource(R.string.dialog_menu_permissions_desc)
            }
            MenuCard(
                title = stringResource(R.string.dialog_menu_permissions),
                description = description,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                expanded = expanded,
                onClick = { expanded = !expanded },
                badge = "${apkInfo.permissions.size}",
            ) {
                PermissionRowList(
                    entries = entries,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
