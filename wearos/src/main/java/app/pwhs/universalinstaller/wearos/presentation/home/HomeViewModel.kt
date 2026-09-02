package app.pwhs.universalinstaller.wearos.presentation.home

import androidx.lifecycle.ViewModel
import app.pwhs.universalinstaller.wearos.data.WearApkInfo
import app.pwhs.universalinstaller.wearos.data.WearApkRepository
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(
    private val repository: WearApkRepository,
) : ViewModel() {

    val apks: StateFlow<List<WearApkInfo>> = repository.apks
}
