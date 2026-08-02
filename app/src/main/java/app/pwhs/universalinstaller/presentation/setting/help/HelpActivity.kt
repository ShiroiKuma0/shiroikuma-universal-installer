package app.pwhs.universalinstaller.presentation.setting.help

import android.os.Bundle
import app.pwhs.universalinstaller.base.BaseActivity

class HelpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme {
            HelpScreen()
        }
    }
}
