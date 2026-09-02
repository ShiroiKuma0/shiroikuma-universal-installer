package app.pwhs.universalinstaller.wearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import app.pwhs.universalinstaller.wearos.presentation.detail.ApkDetailScreen
import app.pwhs.universalinstaller.wearos.presentation.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearNavGraph()
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{apkId}"
    fun detail(apkId: String) = "detail/$apkId"
}

@Composable
fun WearNavGraph() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onApkClick = { apkId ->
                    navController.navigate(Routes.detail(apkId))
                },
            )
        }
        composable(Routes.DETAIL) { backStackEntry ->
            val apkId = backStackEntry.arguments?.getString("apkId") ?: return@composable
            ApkDetailScreen(
                apkId = apkId,
                onInstallSuccess = { navController.popBackStack() },
                onDelete = { navController.popBackStack() },
            )
        }
    }
}