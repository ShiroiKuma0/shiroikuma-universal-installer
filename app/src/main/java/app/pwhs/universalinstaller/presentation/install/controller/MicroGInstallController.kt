package app.pwhs.universalinstaller.presentation.install.controller

import android.app.Application
import android.content.Context
import android.net.Uri
import app.pwhs.universalinstaller.data.local.InstallHistoryDao
import app.pwhs.universalinstaller.domain.model.SessionData
import app.pwhs.universalinstaller.domain.repository.SessionDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.session.ProgressSession
import java.util.UUID

class MicroGInstallController(
    private val application: Application,
    packageInstaller: PackageInstaller,
    sessionDataRepository: SessionDataRepository,
    historyDao: InstallHistoryDao,
) : BaseInstallController(application, packageInstaller, sessionDataRepository, historyDao) {

    override val telemetryMethod = "microg"

    override fun install(
        uris: List<Uri>,
        sessionData: SessionData,
        scope: CoroutineScope,
        context: Context?,
        originalUri: Uri?,
        deleteAfterInstall: Boolean,
        allowDowngrade: Boolean,
        onSuccess: (suspend () -> Unit)?,
        onSessionCreated: ((UUID) -> Unit)?,
    ) {
        val sessionId = UUID.randomUUID()
        val data = sessionData.copy(
            id = sessionId,
            uris = uris,
            originalUri = originalUri,
            deleteAfterInstall = deleteAfterInstall,
            allowDowngrade = allowDowngrade,
        )
        scope.launch {
            sessionDataRepository.addSessionData(data)
            onSessionCreated?.invoke(sessionId)
            reportInstallStarted(uris.size, method = telemetryMethod, id = sessionId)
            MicroGInstallActivity.start(
                context = context ?: application,
                uris = uris,
                packageName = sessionData.packageName,
                sessionId = sessionId.toString(),
            )
        }
    }

    override suspend fun createSession(
        uris: List<Uri>,
        name: String,
        packageName: String,
        allowDowngrade: Boolean,
        targetUserId: Int?,
    ): ProgressSession<InstallFailure> {
        throw UnsupportedOperationException("MicroG controller delegates to MicroGInstallActivity")
    }
}
