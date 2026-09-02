package app.pwhs.universalinstaller.wearos.di

import app.pwhs.universalinstaller.wearos.data.WearApkRepository
import app.pwhs.universalinstaller.wearos.presentation.detail.DetailViewModel
import app.pwhs.universalinstaller.wearos.presentation.home.HomeViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearModule = module {
    singleOf(::WearApkRepository)
    viewModelOf(::HomeViewModel)
    viewModel { params ->
        DetailViewModel(apkId = params.get(), repository = get(), application = get())
    }
}
