package app.pwhs.universalinstaller.wearos

import android.app.Application
import app.pwhs.universalinstaller.wearos.di.wearModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WearApp)
            modules(wearModule)
        }
    }
}
