package app.pwhs.universalinstaller.presentation.install.dialog

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.presentation.composable.InstallerModeBadge
import app.pwhs.universalinstaller.ui.theme.DialogActionButton
import app.pwhs.universalinstaller.ui.theme.DialogButtonKind
import app.pwhs.universalinstaller.ui.theme.LocalExtendedColors
import app.pwhs.universalinstaller.ui.theme.dialogTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogPrepareContent(
    apkInfo: ApkInfo,
    installedVersionName: String? = null,
    installedVersionCode: Long? = null,
    onInstall: () -> Unit,
    onMenu: () -> Unit,
    onCancel: () -> Unit,
    onUnblock: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current
    val isInstalled = installedVersionCode != null && installedVersionCode > 0
    val isDowngrade = isInstalled && apkInfo.versionCode < installedVersionCode
    // Installing the versionCode that is already on the device is a re-install, not an update:
    // nothing moves forward, so it must not borrow the update's accent or its "Update" label.
    // The accent is the semantic success green rather than any of the theme's accent roles —
    // in the 白い熊 yellow scheme primary, secondary AND tertiary are all yellow, which is
    // exactly the colour this state has to be told apart from.
    val isSameVersion = isInstalled && apkInfo.versionCode == installedVersionCode
    val isUpdate = isInstalled && !isDowngrade && !isSameVersion

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Version Info ──
        AnimatedContent(
            targetState = VersionState(isUpdate, isDowngrade, isSameVersion, apkInfo.versionName),
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "VersionAnimation",
        ) { (update, downgrade, sameVersion, newVersion) ->
            when {
                downgrade -> {
                    Text(
                        text = stringResource(
                            R.string.dialog_version_downgrade,
                            installedVersionName ?: "?",
                            newVersion,
                        ),
                        style = dialogTextStyle("version", MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.error),
                        textAlign = TextAlign.Center,
                    )
                }
                sameVersion -> {
                    Text(
                        text = stringResource(
                            R.string.dialog_version_same,
                            newVersion,
                            apkInfo.versionCode,
                        ),
                        style = dialogTextStyle("version", MaterialTheme.typography.bodyMedium, extendedColors.success),
                        textAlign = TextAlign.Center,
                    )
                }
                update -> {
                    VersionTransition(
                        oldVersion = installedVersionName ?: "?",
                        newVersion = newVersion,
                    )
                }
                else -> {
                    Text(
                        text = "${newVersion} (${apkInfo.versionCode})",
                        style = dialogTextStyle("version", MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // ── Size ──
        val sizeText = Formatter.formatFileSize(context, apkInfo.fileSizeBytes)
        if (apkInfo.fileSizeBytes > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sizeText,
                style = dialogTextStyle("file_size", MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }

        // ── Warning Chips ──
        val hasChips = isDowngrade || isSameVersion || apkInfo.splitCount > 1 || apkInfo.obbFileNames.isNotEmpty()
        AnimatedVisibility(visible = hasChips) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .animateContentSize(animationSpec = DialogMotion.ContentSpring),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isDowngrade) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.dialog_chip_downgrade), style = dialogTextStyle("chip", MaterialTheme.typography.labelLarge)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (isSameVersion) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.dialog_chip_same_version), style = dialogTextStyle("chip", MaterialTheme.typography.labelLarge)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                                tint = extendedColors.success,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = extendedColors.successContainer,
                            labelColor = extendedColors.success,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (apkInfo.splitCount > 1) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(stringResource(R.string.dialog_chip_split_apk), style = dialogTextStyle("chip", MaterialTheme.typography.labelLarge))
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (apkInfo.obbFileNames.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(stringResource(R.string.dialog_chip_has_obb), style = dialogTextStyle("chip", MaterialTheme.typography.labelLarge))
                        },
                    )
                }
            }
        }

        // ── Install engine (tap to switch) ──
        Spacer(modifier = Modifier.height(16.dp))
        InstallerModeBadge()

        Spacer(modifier = Modifier.height(20.dp))

        // ── Buttons: [Menu] [Install] ──
        if (apkInfo.isBlocked) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.install_blocked_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onUnblock(apkInfo.packageName) }) {
                        Text(
                            text = stringResource(R.string.install_blocked_unblock),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Menu button
            DialogActionButton(
                slot = "menu",
                kind = DialogButtonKind.Outlined,
                onClick = onMenu,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.dialog_menu_btn),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            // Install/Update/Reinstall/Downgrade button
            DialogActionButton(
                slot = "install",
                kind = DialogButtonKind.Filled,
                onClick = onInstall,
                modifier = Modifier.weight(1f),
                enabled = !apkInfo.isBlocked,
                defaultContainer = when {
                    isDowngrade -> MaterialTheme.colorScheme.error
                    isSameVersion -> extendedColors.success
                    else -> null
                },
                defaultContent = when {
                    isDowngrade -> MaterialTheme.colorScheme.onError
                    isSameVersion -> extendedColors.onSuccess
                    else -> null
                },
            ) {
                Text(
                    text = when {
                        isDowngrade -> stringResource(R.string.dialog_downgrade_btn)
                        isSameVersion -> stringResource(R.string.dialog_reinstall_btn)
                        isUpdate -> stringResource(R.string.dialog_update_btn)
                        else -> stringResource(R.string.dialog_install_btn)
                    },
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        // Cancel
        Spacer(modifier = Modifier.height(4.dp))
        DialogActionButton(
            slot = "cancel",
            kind = DialogButtonKind.Tonal,
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.dialog_cancel_btn))
        }
    }
}

/**
 * Animation key for the version line — a four-slot [Triple], since the line now distinguishes
 * install / update / re-install / downgrade.
 */
private data class VersionState(
    val update: Boolean,
    val downgrade: Boolean,
    val sameVersion: Boolean,
    val versionName: String,
)

/**
 * The "installed version → new version" line of an update.
 *
 * Long version names — our git-pinned `6.3.0-alpha.2026-07-30.g5c0ed6a3+002` style, or anything
 * with a build suffix — used to tear the plain Row apart: the old version was measured first and
 * ate the whole dialog width, leaving the new version a few pixels in which to wrap itself into a
 * one-character-per-line ribbon that spilled past the card edge. So we measure both strings up
 * front against the width we actually have: if they fit side by side nothing changes, otherwise
 * the pair is stacked with a downward arrow and each version soft-wraps over as many lines as it
 * needs. Nothing is ever clipped or ellipsised — the full version string always stays readable.
 */
@Composable
private fun VersionTransition(oldVersion: String, newVersion: String) {
    val oldStyle = dialogTextStyle(
        "version_old",
        MaterialTheme.typography.bodyMedium,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val newStyle = dialogTextStyle(
        "version",
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        MaterialTheme.colorScheme.primary,
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // The inline arrow costs 16dp of icon plus 4dp of padding on either side. maxWidth is
        // Dp.Infinity if we're ever measured unbounded, which simply keeps the side-by-side row.
        val available = maxWidth - 24.dp
        // Measured every composition on purpose: TextMeasurer caches internally, and a custom
        // dialog font that finishes loading late must re-measure rather than keep a stale verdict.
        val sideBySideWidth = with(density) {
            (measurer.measure(oldVersion, oldStyle).size.width +
                    measurer.measure(newVersion, newStyle).size.width).toDp()
        }

        if (sideBySideWidth <= available) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = oldVersion, style = oldStyle)
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )
                Text(text = newVersion, style = newStyle)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = oldVersion,
                    style = oldStyle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .size(16.dp),
                )
                Text(
                    text = newVersion,
                    style = newStyle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
