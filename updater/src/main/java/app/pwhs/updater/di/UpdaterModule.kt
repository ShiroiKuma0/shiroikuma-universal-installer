package app.pwhs.updater.di

import app.pwhs.updater.data.local.UpdaterDatabase
import app.pwhs.updater.data.remote.AppDownloader
import app.pwhs.updater.data.repo.AppUpdateRepository
import app.pwhs.updater.data.repo.AppUpdateRepositoryImpl
import app.pwhs.updater.domain.provider.CodebergReleaseProvider
import app.pwhs.updater.domain.provider.DirectApkProvider
import app.pwhs.updater.domain.provider.FDroidReleaseProvider
import app.pwhs.updater.domain.provider.GitHubReleaseProvider
import app.pwhs.updater.domain.provider.GitLabReleaseProvider
import app.pwhs.updater.presentation.UpdatesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val updaterModule = module {
    single { UpdaterDatabase.getInstance(get()) }
    single { get<UpdaterDatabase>().trackedAppDao() }
    single { GitHubReleaseProvider() }
    single { GitLabReleaseProvider() }
    single { CodebergReleaseProvider() }
    single { FDroidReleaseProvider() }
    single { DirectApkProvider() }
    single { AppDownloader(get()) }
    single<AppUpdateRepository> {
        AppUpdateRepositoryImpl(
            dao = get(),
            context = get(),
            providers = listOf(
                get<GitHubReleaseProvider>(),
                get<GitLabReleaseProvider>(),
                get<CodebergReleaseProvider>(),
                get<FDroidReleaseProvider>(),
                get<DirectApkProvider>(),
            ),
        )
    }
    viewModel {
        UpdatesViewModel(
            repository = get(),
            downloader = get(),
            providers = listOf(
                get<GitHubReleaseProvider>(),
                get<GitLabReleaseProvider>(),
                get<CodebergReleaseProvider>(),
                get<FDroidReleaseProvider>(),
                get<DirectApkProvider>(),
            ),
        )
    }
}
