package app.pwhs.updater.presentation.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun AppIconView(
    packageName: String,
    appName: String,
    iconUrl: String? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val installedIcon: Drawable? = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (installedIcon != null) {
            AsyncImage(
                model = installedIcon,
                contentDescription = appName,
                modifier = Modifier.size(size),
            )
        } else if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = appName,
                modifier = Modifier.size(size),
            )
        } else {
            // Monogram letter fallback with modern container styling
            val initial = appName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
            Box(
                modifier = Modifier
                    .size(size)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (initial.isNotBlank() && initial[0].isLetterOrDigit()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (size.value * 0.45).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(size * 0.6f),
                    )
                }
            }
        }
    }
}
