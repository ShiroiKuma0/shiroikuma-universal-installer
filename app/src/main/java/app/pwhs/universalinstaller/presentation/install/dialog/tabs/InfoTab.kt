package app.pwhs.universalinstaller.presentation.install.dialog.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.presentation.install.dialog.components.DetailRow
import app.pwhs.universalinstaller.presentation.install.dialog.components.MenuCard
import app.pwhs.universalinstaller.ui.theme.dialogTextStyle

internal fun androidx.compose.foundation.lazy.LazyListScope.infoTab(
    apkInfo: ApkInfo,
    context: Context,
) {
    // 1. App Details
    item(key = "details") {
        MenuCard(
            title = stringResource(R.string.dialog_menu_details),
            description = stringResource(R.string.dialog_menu_details_desc),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            expanded = true, // Always expanded in this tab
            onClick = { /* Do nothing, static */ },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DetailRow(stringResource(R.string.apk_info_label_package), apkInfo.packageName)
                if (apkInfo.versionName.isNotBlank()) {
                    DetailRow(
                        stringResource(R.string.apk_info_label_version),
                        stringResource(R.string.apk_info_version_detail, apkInfo.versionName, apkInfo.versionCode),
                    )
                }
                if (apkInfo.minSdkVersion > 0) {
                    DetailRow(stringResource(R.string.apk_info_label_min_sdk), "API ${apkInfo.minSdkVersion}")
                }
                if (apkInfo.targetSdkVersion > 0) {
                    DetailRow(stringResource(R.string.apk_info_label_target_sdk), "API ${apkInfo.targetSdkVersion}")
                }
                if (apkInfo.fileSizeBytes > 0) {
                    DetailRow(stringResource(R.string.install_storage_title), Formatter.formatFileSize(context, apkInfo.fileSizeBytes))
                }
                DetailRow("Format", apkInfo.fileFormat)
                if (apkInfo.isAndroidAutoSupported) {
                    DetailRow(
                        stringResource(R.string.aa_compatibility_title),
                        stringResource(R.string.aa_compatibility_ok),
                    )
                }
            }
        }
    }

    // 2. Architectures
    if (apkInfo.supportedAbis.isNotEmpty()) {
        item(key = "architectures") {
            var expanded by remember { mutableStateOf(false) }
            MenuCard(
                title = stringResource(R.string.dialog_menu_architectures),
                description = apkInfo.supportedAbis.joinToString(", "),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                expanded = expanded,
                onClick = { expanded = !expanded },
                badge = "${apkInfo.supportedAbis.size}",
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    apkInfo.supportedAbis.forEach { abi ->
                        Text(
                            text = abi,
                            style = dialogTextStyle("detail_value", MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                }
            }
        }
    }

    // 4. SHA-256 Hash
    if (apkInfo.sha256.isNotBlank()) {
        item(key = "sha256") {
            var expanded by remember { mutableStateOf(false) }
            MenuCard(
                title = stringResource(R.string.dialog_menu_sha256),
                description = apkInfo.sha256.take(24) + "…",
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                expanded = expanded,
                onClick = { expanded = !expanded },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = apkInfo.sha256,
                        style = dialogTextStyle("detail_value", MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("SHA-256", apkInfo.sha256))
                            Toast.makeText(context, context.getString(R.string.dialog_menu_sha256_copied), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.about_btn_copy))
                    }
                }
            }
        }
    }
}
