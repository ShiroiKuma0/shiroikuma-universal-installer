package app.pwhs.universalinstaller.di

import app.pwhs.universalinstaller.presentation.install.controller.FullInstallerBackendFactory
import app.pwhs.universalinstaller.presentation.install.controller.InstallerBackendFactory
import app.pwhs.updater.di.updaterModule
import app.pwhs.updater.worker.PeriodicUpdateCheckWorker
import org.koin.dsl.module

val flavorModule = module {
    single<InstallerBackendFactory> { FullInstallerBackendFactory() }
    includes(updaterModule)
    single {
        PeriodicUpdateCheckWorker.schedule(get())
        Unit
    }
}
