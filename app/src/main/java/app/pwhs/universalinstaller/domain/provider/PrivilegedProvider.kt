package app.pwhs.universalinstaller.domain.provider

import android.content.Context
import android.content.pm.PackageManager
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.presentation.install.controller.InstallerBackendFactory
import app.pwhs.universalinstaller.presentation.install.controller.RootState
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import timber.log.Timber

enum class PrivilegedExecutor { Root, Shizuku }

interface PrivilegedProvider {
    suspend fun resolveExecutor(): PrivilegedExecutor?
    fun isShizukuReady(): Boolean
}

class PrivilegedProviderImpl(
    private val context: Context,
    private val backendFactory: InstallerBackendFactory
) : PrivilegedProvider {

    override suspend fun resolveExecutor(): PrivilegedExecutor? {
        val prefs = context.dataStore.data.first()
        val useRoot = prefs[PreferencesKeys.USE_ROOT] ?: false
        val useShizuku = prefs[PreferencesKeys.USE_SHIZUKU] ?: false

        if (useRoot) {
            val state = backendFactory.probeRootState()
            if (state == RootState.READY) return PrivilegedExecutor.Root
            
            if (state == RootState.UNKNOWN || state == RootState.DENIED) {
                val reqState = backendFactory.requestRoot()
                if (reqState == RootState.READY) {
                    return PrivilegedExecutor.Root
                }
            }
        }

        if (useShizuku) {
            if (isShizukuReady()) {
                return PrivilegedExecutor.Shizuku
            }
        }

        return null
    }

    override fun isShizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        Timber.w(e, "Shizuku readiness probe failed")
        false
    }
}
