package app.pwhs.universalinstaller.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.core.data.local.dataStore
import app.pwhs.core.data.local.SharedPrefsKeys
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMain: () -> Unit,
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate in
        scale.animateTo(1f, animationSpec = tween(400))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(400))
    }

    LaunchedEffect(Unit) {
        delay(1000)
        val completed = context.dataStore.data
            .map { it[SharedPrefsKeys.ONBOARDING_COMPLETED] ?: false }
            .first()
        if (completed) onNavigateToMain() else onNavigateToOnboarding()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value)
                .scale(scale.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // The launcher icon is an adaptive icon (AdaptiveIconDrawable); painterResource
            // can't load that. Render its foreground vector (the yellow glyph) instead, sized
            // up to offset the adaptive safe-zone padding, on the black splash background.
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFF00),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFFF00),
            )
        }
    }
}
