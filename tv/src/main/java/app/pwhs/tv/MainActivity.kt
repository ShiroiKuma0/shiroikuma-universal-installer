package app.pwhs.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.pwhs.core.data.local.SharedPrefsKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.presentation.onboarding.OnboardingScreen
import app.pwhs.tv.presentation.splash.SplashScreen
import app.pwhs.tv.ui.theme.UniversalInstallerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey
import app.pwhs.core.domain.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReceiverService.start(applicationContext)
        setContent {
            val themeModeName by dataStore.data
                .map { it[stringPreferencesKey("theme_mode")] ?: ThemeMode.System.name }
                .collectAsState(initial = ThemeMode.System.name)
            
            val themeMode = remember(themeModeName) {
                ThemeMode.entries.find { it.name == themeModeName } ?: ThemeMode.System
            }

            UniversalInstallerTheme(themeMode = themeMode) {
                val onboardingCompleted by dataStore.data
                    .map { it[SharedPrefsKeys.ONBOARDING_COMPLETED] ?: false }
                    .collectAsState(initial = null)

                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1800)
                    showSplash = false
                }

                if (onboardingCompleted == null) {
                    // Still loading preferences
                    SplashScreen()
                } else if (onboardingCompleted == false) {
                    OnboardingScreen(onFinish = {
                        // After onboarding, the UI will recompose because onboardingCompleted flow updates
                        // We need to actually trigger a write to the dataStore
                    })
                } else {
                    TvApp()
                    AnimatedVisibility(visible = showSplash, enter = fadeIn(), exit = fadeOut()) {
                        SplashScreen()
                    }
                }
            }
        }
    }
}
