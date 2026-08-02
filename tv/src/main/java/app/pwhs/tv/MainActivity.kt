package app.pwhs.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.pwhs.core.data.local.SharedPrefsKeys
import androidx.lifecycle.lifecycleScope
import app.pwhs.tv.presentation.receive.ExternalApkIntake
import app.pwhs.core.data.local.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import app.pwhs.core.presentation.onboarding.OnboardingScreen
import app.pwhs.tv.presentation.splash.SplashScreen
import app.pwhs.tv.ui.theme.OnboardingThemeBridge
import app.pwhs.tv.ui.theme.UniversalInstallerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey
import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.domain.ThemeMode

import app.pwhs.tv.util.LocaleHelper

private const val MIN_SPLASH_MS = 1500L

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    /** singleTop: a second "Open with" while we're already running lands here, not in onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeInExternalApks(intent)
    }

    /**
     * Stage anything another app opened with us and publish it as the pending install (#94).
     *
     * The intent is marked consumed so an Activity recreate — a locale change, say — doesn't
     * ingest the same APK a second time. TvReceiverState replays the last arrival, so this works
     * even while onboarding is still on screen: the card is waiting once the receive tab appears.
     */
    private fun takeInExternalApks(intent: Intent?) {
        val uris = ExternalApkIntake.urisFrom(intent)
        if (uris.isEmpty()) return
        ExternalApkIntake.markConsumed(intent)
        lifecycleScope.launch {
            uris.forEach { ExternalApkIntake.accept(applicationContext, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        takeInExternalApks(intent)

        // Only play the brand splash on a genuine cold start — not on config-change/locale recreate,
        // which would otherwise replay the full splash and feel like a reset.
        val coldStart = savedInstanceState == null

        setContent {
            val themeModeName by dataStore.data
                .map { it[stringPreferencesKey("theme_mode")] ?: ThemeMode.System.name }
                .collectAsState(initial = ThemeMode.System.name)

            val themeMode = remember(themeModeName) {
                ThemeMode.entries.find { it.name == themeModeName } ?: ThemeMode.System
            }

            val themePresetName by dataStore.data
                .map { it[stringPreferencesKey("theme_preset")] ?: AppThemePreset.Orange.name }
                .collectAsState(initial = AppThemePreset.Orange.name)
            val themePreset = remember(themePresetName) {
                AppThemePreset.entries.find { it.name == themePresetName } ?: AppThemePreset.Orange
            }

            UniversalInstallerTheme(themeMode = themeMode, themePreset = themePreset) {
                val onboardingCompleted by dataStore.data
                    .map { it[SharedPrefsKeys.ONBOARDING_COMPLETED] ?: false }
                    .collectAsState(initial = null)

                // Splash stays up until (a) the minimum brand-display time has elapsed AND
                // (b) preferences have resolved so we know which screen to reveal underneath.
                var minElapsed by remember { mutableStateOf(!coldStart) }
                LaunchedEffect(Unit) {
                    if (coldStart) {
                        delay(MIN_SPLASH_MS)
                        minElapsed = true
                    }
                }
                // Gate on coldStart too: on a locale recreate, onboardingCompleted is briefly null
                // again, which would otherwise flash the splash for a frame before fading out.
                val showSplash = coldStart && (onboardingCompleted == null || !minElapsed)

                Box(Modifier.fillMaxSize()) {
                    when (onboardingCompleted) {
                        false -> {
                            val scope = androidx.compose.runtime.rememberCoroutineScope()
                            // Bridge the TV brand palette into the shared (mobile-material3) onboarding.
                            OnboardingThemeBridge(themeMode) {
                                OnboardingScreen(onFinish = {
                                    scope.launch {
                                        dataStore.edit { prefs ->
                                            prefs[SharedPrefsKeys.ONBOARDING_COMPLETED] = true
                                        }
                                    }
                                })
                            }
                        }

                        true -> {
                            LaunchedEffect(Unit) {
                                app.pwhs.core.receiver.TvReceiver.start(applicationContext)
                            }
                            TvApp()
                        }

                        null -> Unit // splash covers the screen while prefs load
                    }

                    // Single splash instance; AnimatedVisibility owns the only fade.
                    AnimatedVisibility(visible = showSplash, enter = fadeIn(), exit = fadeOut()) {
                        SplashScreen()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.pwhs.core.receiver.TvReceiver.stop()
    }
}
