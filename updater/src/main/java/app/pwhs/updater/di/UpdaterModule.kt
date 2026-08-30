package app.pwhs.updater.di

import app.pwhs.updater.data.local.UpdaterDatabase
import app.pwhs.updater.data.remote.AppDownloader
import app.pwhs.updater.data.repo.AppUpdateRepository
import app.pwhs.updater.data.repo.AppUpdateRepositoryImpl
import app.pwhs.updater.domain.provider.GitHubReleaseProvider
import app.pwhs.updater.presentation.UpdatesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val updaterModule = module {
    single { UpdaterDatabase.getInstance(get()) }
    single { get<UpdaterDatabase>().trackedAppDao() }
    single { GitHubReleaseProvider() }
    single { AppDownloader(get()) }
    single<AppUpdateRepository> {
        AppUpdateRepositoryImpl(
            dao = get(),
            providers = listOf(get<GitHubReleaseProvider>()),
        )
    }
    viewModel {
        UpdatesViewModel(
            repository = get(),
            downloader = get(),
            gitHubProvider = get(),
        )
    }
}
