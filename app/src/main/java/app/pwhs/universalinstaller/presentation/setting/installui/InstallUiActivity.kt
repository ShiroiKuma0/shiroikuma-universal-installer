package app.pwhs.universalinstaller.presentation.setting.installui

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.pwhs.universalinstaller.base.BaseActivity

/** Host for [InstallUiScreen]. Same shape as the theme screen's activity, deliberately. */
class InstallUiActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                InstallUiScreen()
            }
        }
    }
}
