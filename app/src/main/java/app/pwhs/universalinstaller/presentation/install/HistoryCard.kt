package app.pwhs.universalinstaller.presentation.install

import android.graphics.BitmapFactory
import android.widget.Toast
import timber.log.Timber
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.data.local.InstallHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun HistoryCard(
    entry: InstallHistoryEntity,
    modifier: Modifier = Modifier,
    onReinstall: ((File) -> Unit)? = null,
) {
    val dateFormat = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val context = LocalContext.current
    var showDetailSheet by rememberSaveable(entry.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { showDetailSheet = true },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon
                val iconBitmap = remember(entry.iconPath) {
                    entry.iconPath?.let { path ->
                        try {
                            val file = File(path)
                            if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }

                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = entry.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (entry.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (entry.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (entry.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = entry.appName.ifBlank { entry.fileName },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        // Operation Badge
                        if (!entry.operationType.isNullOrBlank()) {
                            val badgeColor = when (entry.operationType) {
                                "UPDATE" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                "DOWNGRADE" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.first,
                            ) {
                                Text(
                                    text = entry.operationType,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor.second,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                            }
                        }

                        // Mode Badge
                        if (!entry.installerMode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = entry.installerMode,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = entry.packageName.ifBlank { entry.fileName },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = dateFormat.format(Date(entry.installedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        if (entry.versionName.isNotBlank()) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                            Text(
                                text = entry.versionName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Quick Launch button / Status
                val launchIntent = if (entry.success) context.packageManager.getLaunchIntentForPackage(entry.packageName) else null
                if (launchIntent != null) {
                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(launchIntent)
                            }.onFailure { e ->
                                Timber.e(e, "Failed to launch app %s", entry.packageName)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_cannot_open_app),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open App",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (entry.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                        contentDescription = if (entry.success) stringResource(R.string.status_success) else stringResource(R.string.status_failed),
                        tint = if (entry.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (showDetailSheet) {
        HistoryDetailSheet(
            entry = entry,
            onDismiss = { showDetailSheet = false },
            onReinstall = onReinstall,
        )
    }
}
