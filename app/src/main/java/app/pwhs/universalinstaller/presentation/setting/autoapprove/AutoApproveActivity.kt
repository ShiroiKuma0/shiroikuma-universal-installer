package app.pwhs.universalinstaller.presentation.setting.autoapprove

import android.os.Bundle
import app.pwhs.universalinstaller.base.BaseActivity

class AutoApproveActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme {
            AutoApproveScreen()
        }
    }
}
