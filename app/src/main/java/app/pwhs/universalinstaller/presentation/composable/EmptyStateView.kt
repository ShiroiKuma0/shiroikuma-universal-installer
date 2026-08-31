package app.pwhs.universalinstaller.presentation.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    app.pwhs.core.ui.component.EmptyStateView(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}
