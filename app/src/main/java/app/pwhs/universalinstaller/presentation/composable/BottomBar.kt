package app.pwhs.universalinstaller.presentation.composable

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.pwhs.core.R as CoreR
import app.pwhs.universalinstaller.presentation.install.InstallActivity
import app.pwhs.universalinstaller.presentation.manage.ManageActivity
import app.pwhs.universalinstaller.presentation.setting.SettingActivity
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.presentation.setting.ui.InstallerUiActivity
import app.pwhs.universalinstaller.ui.theme.BottomBarThemeStore
import app.pwhs.universalinstaller.util.extension.disableSceneTransition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

enum class BottomBarItem(
    val activityClass: Class<*>?,
    val label: Int,
    val icon: ImageVector,
) {
    Install(
        activityClass = InstallActivity::class.java,
        label = CoreR.string.nav_install,
        icon = Icons.Rounded.InstallMobile,
    ),
    Updates(
        activityClass = runCatching { Class.forName("app.pwhs.updater.presentation.UpdatesActivity") }.getOrNull(),
        label = CoreR.string.nav_updates,
        icon = Icons.Rounded.RocketLaunch,
    ),
    Manage(
        activityClass = ManageActivity::class.java,
        label = CoreR.string.nav_manage,
        icon = Icons.Rounded.Apps,
    ),
    Settings(
        activityClass = SettingActivity::class.java,
        label = CoreR.string.nav_settings,
        icon = Icons.Rounded.Settings,
    );
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomBar(
    currentTab: BottomBarItem,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    // App-wide bottom-bar override, read straight from DataStore so the bar is identical on every tab.
    val initialBb = remember { runBlocking { BottomBarThemeStore.from(context.dataStore.data.first()) } }
    val bbFlow = remember { context.dataStore.data.map { BottomBarThemeStore.from(it) } }
    val bb by bbFlow.collectAsState(initial = initialBb)
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = bb.selectedIcon?.let { Color(it) } ?: colors.onPrimaryContainer,
        selectedTextColor = bb.selectedText?.let { Color(it) } ?: colors.primary,
        indicatorColor = bb.indicator?.let { Color(it) } ?: colors.primaryContainer,
        // 白い熊: unselected items follow the accent too (Manage/Settings in yellow, not grey).
        unselectedIconColor = bb.unselectedIcon?.let { Color(it) } ?: colors.primary,
        unselectedTextColor = bb.unselectedText?.let { Color(it) } ?: colors.primary,
    )

    val updateCount by produceState(initialValue = 0) {
        runCatching {
            val repoClass = Class.forName("app.pwhs.updater.data.repo.AppUpdateRepository")
            val repo = org.koin.java.KoinJavaComponent.get<Any>(repoClass)
            val method = repoClass.getMethod("getUpdateCount")
            @Suppress("UNCHECKED_CAST")
            val flow = method.invoke(repo) as? kotlinx.coroutines.flow.Flow<Int>
            flow?.collect { value = it }
        }
    }

    val destinations = remember {
        BottomBarItem.entries.filter { it.activityClass != null }
    }

    Column {
        // 白い熊: accent border along the bar's top edge.
        HorizontalDivider(thickness = 1.5.dp, color = colors.primary)
        NavigationBar(containerColor = bb.container?.let { Color(it) } ?: NavigationBarDefaults.containerColor) {
        destinations.forEach { destination ->
            val isSelected = currentTab == destination
            val navigate = {
                if (!isSelected && destination.activityClass != null) {
                    val intent = Intent(context, destination.activityClass).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.disableSceneTransition()
                }
            }
            NavigationBarItem(
                selected = isSelected,
                colors = itemColors,
                onClick = navigate,
                icon = {
                    // Long-pressing the Settings cog jumps straight to the 白い熊 Installer UI page.
                    val iconModifier = if (destination == BottomBarItem.Settings) {
                        Modifier.combinedClickable(
                            onClick = navigate,
                            onLongClick = {
                                context.startActivity(Intent(context, InstallerUiActivity::class.java))
                                (context as? Activity)?.disableSceneTransition()
                            },
                        )
                    } else {
                        Modifier
                    }
                    if (destination == BottomBarItem.Updates && updateCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(if (updateCount > 99) "99+" else updateCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.label),
                                modifier = iconModifier,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.label),
                            modifier = iconModifier,
                        )
                    }
                },
                label = { Text(stringResource(destination.label)) },
            )
        }
        }
    }
}