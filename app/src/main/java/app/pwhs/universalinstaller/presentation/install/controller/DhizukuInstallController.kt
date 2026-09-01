package app.pwhs.universalinstaller.presentation.install.controller

import android.app.Application
import android.net.Uri
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.data.local.InstallHistoryDao
import app.pwhs.universalinstaller.domain.repository.SessionDataRepository
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.coroutines.flow.first
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.dhizuku.dhizuku
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.parameters.Confirmation

/**
 * Installs through Dhizuku, which delegates *device owner* privileges to this app.
 *
 * Worth knowing how little it can do compared to [ShizukuInstallController]: the plugin exposes
 * exactly one install option, `requestDowngrade`. There is no replace-existing, no grant-all,
 * no allow-test, no installer-package spoofing and no all-users — device owner operations only
 * target the current Android user. Rather than silently dropping the other flags, the settings
 * screen tells the user which ones this backend ignores.
 */
class DhizukuInstallController(
    private val application: Application,
    packageInstaller: PackageInstaller,
    sessionDataRepository: SessionDataRepository,
    historyDao: InstallHistoryDao,
) : BaseInstallController(application, packageInstaller, sessionDataRepository, historyDao) {

    override val telemetryMethod = "dhizuku"

    @OptIn(DelicateAckpineApi::class)
    override suspend fun createSession(
        uris: List<Uri>,
        name: String,
        packageName: String,
        allowDowngrade: Boolean,
        targetUserId: Int?,
    ): ProgressSession<InstallFailure> {
        val prefs = application.dataStore.data.first()
        return packageInstaller.createSession(uris) {
            this.name = name
            confirmation = Confirmation.IMMEDIATE
            dhizuku {
                // Same shape as the other privileged backends: the per-install consent from the
                // downgrade dialog is ORed with the standing setting, never written back to it.
                requestDowngrade =
                    allowDowngrade || (prefs[PreferencesKeys.DHIZUKU_REQUEST_DOWNGRADE] ?: false)
            }
        }
    }
}
