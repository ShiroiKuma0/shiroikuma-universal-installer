@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package app.pwhs.universalinstaller.presentation.manage.logs


import android.os.Bundle
import app.pwhs.universalinstaller.base.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class UninstallLogsActivity : BaseActivity() {

    private val viewModel: UninstallLogsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme {
            UninstallLogsScreen(viewModel = viewModel)
        }
    }
}
