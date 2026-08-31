package app.pwhs.updater.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.pwhs.core.data.local.SharedPrefsKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.domain.ThemeMode
import app.pwhs.core.ui.theme.UniversalInstallerTheme
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.viewModel

class UpdatesActivity : ComponentActivity() {

    private val viewModel: UpdatesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeFlow = remember {
                dataStore.data.map { prefs ->
                    val modeName = prefs[SharedPrefsKeys.THEME_MODE] ?: ThemeMode.System.name
                    val themeMode = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.System)
                    val presetName = prefs[SharedPrefsKeys.THEME_PRESET] ?: AppThemePreset.Orange.name
                    val themePreset = runCatching { AppThemePreset.valueOf(presetName) }.getOrDefault(AppThemePreset.Orange)
                    val dynamicColor = prefs[SharedPrefsKeys.DYNAMIC_COLOR] ?: false
                    val amoledMode = prefs[SharedPrefsKeys.AMOLED_MODE] ?: false
                    ThemeConfig(themeMode, themePreset, dynamicColor, amoledMode)
                }
            }
            val themeConfig by themeFlow.collectAsState(initial = ThemeConfig())

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeConfig.mode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
                onDispose {}
            }

            UniversalInstallerTheme(
                darkTheme = darkTheme,
                dynamicColor = themeConfig.dynamicColor,
                amoledMode = themeConfig.amoledMode,
                themePreset = themeConfig.preset
            ) {
                UpdatesScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                )
            }
        }
    }

    private data class ThemeConfig(
        val mode: ThemeMode = ThemeMode.System,
        val preset: AppThemePreset = AppThemePreset.Orange,
        val dynamicColor: Boolean = false,
        val amoledMode: Boolean = false,
    )
}
