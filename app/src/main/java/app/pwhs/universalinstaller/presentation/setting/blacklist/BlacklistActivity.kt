package app.pwhs.universalinstaller.presentation.setting.blacklist

import android.os.Bundle
import app.pwhs.universalinstaller.base.BaseActivity

class BlacklistActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme {
            BlacklistScreen()
        }
    }
}
