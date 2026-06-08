package app.pwhs.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.pwhs.core.data.AppRepository
import app.pwhs.tv.ui.theme.UniversalInstallerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start the LAN receiver so a phone can push APKs to this TV.
        ReceiverService.start(applicationContext)
        val repo = AppRepository(applicationContext)
        setContent {
            UniversalInstallerTheme {
                TvApp(repo = repo)
            }
        }
    }
}
