package app.pwhs.updater.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.SubcomposeAsyncImage

@Composable
fun AppIconView(
    packageName: String,
    appName: String,
    iconUrl: String? = null,
    sourceUrl: String? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 1. Direct synchronous PackageManager icon bitmap extraction for installed packages
    val installedBitmap: ImageBitmap? = remember(packageName) {
        runCatching {
            if (!packageName.startsWith("tracked.")) {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap(192, 192).asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }

    // 2. Resolve remote icon URL if not provided directly
    val resolvedRemoteUrl = remember(iconUrl, sourceUrl, packageName) {
        iconUrl?.takeIf { it.isNotBlank() } ?: resolveFallbackIconUrl(sourceUrl, packageName)
    }

    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (installedBitmap != null) {
            Image(
                bitmap = installedBitmap,
                contentDescription = appName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!resolvedRemoteUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = resolvedRemoteUrl,
                contentDescription = appName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(size * 0.4f),
                        )
                    }
                },
                error = {
                    MonogramFallback(appName = appName, size = size)
                },
            )
        } else {
            MonogramFallback(appName = appName, size = size)
        }
    }
}

@Composable
private fun MonogramFallback(appName: String, size: Dp) {
    val initial = appName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
    Box(
        modifier = Modifier
            .fillMaxSize()
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

private fun resolveFallbackIconUrl(sourceUrl: String?, packageName: String): String? {
    if (sourceUrl.isNullOrBlank()) return null
    val lower = sourceUrl.lowercase().trim()
    return when {
        lower.contains("github.com") -> {
            val clean = sourceUrl.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                .removePrefix("github.com/").trim()
            val parts = clean.split("/").filter { it.isNotBlank() }
            if (parts.isNotEmpty()) "https://github.com/${parts[0]}.png" else null
        }
        lower.contains("f-droid.org") || lower.contains("apt.izzysoft.de") -> {
            "https://f-droid.org/repo/icons-640/$packageName.png"
        }
        lower.contains("gitlab.com") -> {
            val clean = sourceUrl.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                .removePrefix("gitlab.com/").trim()
            val parts = clean.split("/").filter { it.isNotBlank() }
            if (parts.isNotEmpty()) "https://gitlab.com/${parts[0]}.png" else null
        }
        else -> null
    }
}
