package app.pwhs.universalinstaller.presentation.composable

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    collapsible: Boolean = false,
    defaultExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    app.pwhs.core.ui.component.SettingsSection(
        title = title,
        icon = icon,
        modifier = modifier,
        collapsible = collapsible,
        defaultExpanded = defaultExpanded,
        content = content
    )
}
