package app.pwhs.updater.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UpdatesBannerCard(
    updateCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdates = updateCount > 0

    val containerColor = if (hasUpdates) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (hasUpdates) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasUpdates) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = if (hasUpdates) Icons.Rounded.RocketLaunch else Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = if (hasUpdates) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (hasUpdates) {
                            "Có $updateCount bản cập nhật mới"
                        } else {
                            "Theo dõi cập nhật ứng dụng"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                    Text(
                        text = if (hasUpdates) {
                            "Nhấn để tải và cài đặt phiên bản mới nhất"
                        } else {
                            "Tự động theo dõi release từ GitHub / GitLab"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open updates",
                tint = contentColor.copy(alpha = 0.7f),
            )
        }
    }
}
