package app.pwhs.universalinstaller.presentation.install.controller

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.data.local.InstallHistoryDao
import app.pwhs.universalinstaller.presentation.install.InstallErrorHelper
import app.pwhs.universalinstaller.data.local.InstallHistoryEntity
import app.pwhs.universalinstaller.domain.model.SessionData
import app.pwhs.universalinstaller.domain.repository.SessionDataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.progress
import ru.solrudev.ackpine.session.state
import timber.log.Timber
import java.io.File
import java.util.UUID

abstract class BaseInstallController(
    protected val context: Context,
    protected val packageInstaller: PackageInstaller,
    protected val sessionDataRepository: SessionDataRepository,
    protected val historyDao: InstallHistoryDao,
) {
    private val activeSessions = mutableMapOf<UUID, ProgressSession<InstallFailure>>()
    private val sessionUris = mutableMapOf<UUID, List<Uri>>()
    private val originalFileUris = mutableMapOf<UUID, Uri>()
    private val deleteFlags = mutableMapOf<UUID, Boolean>()
    private val successHooks = mutableMapOf<UUID, suspend () -> Unit>()

    protected abstract suspend fun createSession(
        uris: List<Uri>,
        name: String,
        packageName: String,
    ): ProgressSession<InstallFailure>

    open fun install(
        uris: List<Uri>,
        sessionData: SessionData,
        scope: CoroutineScope,
        context: Context? = null,
        originalUri: Uri? = null,
        deleteAfterInstall: Boolean = false,
        onSuccess: (suspend () -> Unit)? = null,
        onSessionCreated: ((UUID) -> Unit)? = null,
    ) {
        scope.launch {
            val session = createSession(uris, sessionData.name, sessionData.packageName)
            activeSessions[session.id] = session
            sessionUris[session.id] = uris
            if (originalUri != null) originalFileUris[session.id] = originalUri
            deleteFlags[session.id] = deleteAfterInstall
            if (onSuccess != null) successHooks[session.id] = onSuccess
            val data = sessionData.copy(id = session.id)
            sessionDataRepository.addSessionData(data)
            // Hand the real ackpine session ID back to the caller. The dialog flow keys its
            // Installing/Success/Failed watchers off this — using the caller-passed id won't
            // match because addSessionData stores the data under session.id, not sessionData.id.
            onSessionCreated?.invoke(session.id)
            awaitSession(session, scope, context)
        }
    }

    fun cancel(id: UUID, scope: CoroutineScope) {
        scope.launch {
            try {
                activeSessions[id]?.cancel()
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling session")
            } finally {
                activeSessions.remove(id)
                sessionUris.remove(id)
                originalFileUris.remove(id)
                deleteFlags.remove(id)
                successHooks.remove(id)
                sessionDataRepository.removeSessionData(id)
            }
        }
    }

    fun retry(id: UUID, scope: CoroutineScope) {
        val uris = sessionUris[id] ?: return
        val oldSession = sessionDataRepository.sessions.value.find { it.id == id } ?: return

        activeSessions.remove(id)
        sessionUris.remove(id)
        sessionDataRepository.removeSessionData(id)

        install(
            uris = uris,
            sessionData = SessionData(
                id = UUID.randomUUID(),
                name = oldSession.name,
                appName = oldSession.appName,
                iconPath = oldSession.iconPath,
            ),
            scope = scope,
        )
    }

    fun restoreSessionsFromSavedState(scope: CoroutineScope) {
        scope.launch {
            val sessions = sessionDataRepository.sessions.value
            for (data in sessions) {
                if (activeSessions.containsKey(data.id)) continue
                val session = packageInstaller.getSession(data.id) ?: continue
                activeSessions[session.id] = session
                awaitSession(session, scope)
            }
        }
    }

    private fun awaitSession(session: ProgressSession<InstallFailure>, scope: CoroutineScope, context: Context? = null) {
        scope.launch {
            session.progress
                .onEach { progress ->
                    sessionDataRepository.updateSessionProgress(session.id, progress)
                }
                .launchIn(this)
            session.state
                .filterIsInstance<Session.State.Committed>()
                .onEach {
                    sessionDataRepository.updateSessionIsCancellable(session.id, isCancellable = false)
                }
                .launchIn(this)
            try {
                val sessionData = sessionDataRepository.sessions.value.find { it.id == session.id }
                when (val result = session.await()) {
                    Session.State.Succeeded -> {
                        saveHistory(sessionData, success = true)
                        // Hook runs BEFORE source deletion so the hook can still read the original
                        // zip (e.g. to extract OBB entries). Errors are caller-reported; we don't
                        // roll back the APK install here.
                        val hook = successHooks.remove(session.id)
                        if (hook != null) {
                            runCatching { hook() }.onFailure {
                                Timber.e(it, "Install success hook failed")
                            }
                        }
                        deleteSourceFileIfNeeded(session.id, context)
                        sessionDataRepository.removeSessionData(session.id)
                        activeSessions.remove(session.id)
                        sessionUris.remove(session.id)
                        originalFileUris.remove(session.id)
                        deleteFlags.remove(session.id)
                    }
                    is Session.State.Failed -> {
                        if (context == null) return@launch
                        val errorInfo = InstallErrorHelper.getErrorInfo(context, result.failure)
                        saveHistory(
                            sessionData,
                            success = false,
                            errorMessage = "${errorInfo.title}\n${errorInfo.guidance}",
                        )
                        // Device-specific workarounds go to the live error only — history keeps
                        // the plain diagnosis.
                        val shown = InstallErrorHelper.withDeviceHint(context, errorInfo, result.failure)
                        handleError("${shown.title}\n${shown.guidance}", session.id)
                    }
                }
            } catch (e: CancellationException) {
                sessionDataRepository.removeSessionData(session.id)
                activeSessions.remove(session.id)
                sessionUris.remove(session.id)
                originalFileUris.remove(session.id)
                deleteFlags.remove(session.id)
                successHooks.remove(session.id)
                throw e
            } catch (e: Exception) {
                handleError(e.message, session.id)
                Timber.e(e, "Session error")
            }
        }
    }

    protected suspend fun saveHistory(
        sessionData: SessionData?,
        success: Boolean,
        errorMessage: String? = null,
    ) {
        if (sessionData == null) return
        try {
            historyDao.insert(
                InstallHistoryEntity(
                    appName = sessionData.appName.ifEmpty { sessionData.name },
                    packageName = sessionData.packageName,
                    fileName = sessionData.name,
                    iconPath = sessionData.iconPath,
                    success = success,
                    errorMessage = errorMessage,
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to save install history")
        }
    }

    /**
     * Delete the file the user installed from. Shared with controllers that bypass the ackpine
     * session path (e.g. the root shell installer) and so don't go through the per-session
     * [deleteFlags] map.
     *
     * Returns false when the file is still on disk afterwards — [deleteSourceFileAndWarn] turns
     * that into something the user can see. Silently swallowing it is what made issue #100 look
     * like the setting did nothing at all.
     */
    protected fun deleteSourceFile(context: Context, uri: Uri): Boolean {
        // `file://` never reaches a DocumentsProvider, so deleteDocument can't touch it —
        // DialogInstallActivity.collectIncomingUris accepts this scheme from external intents.
        // MANAGE_EXTERNAL_STORAGE (declared in the manifest) is what makes the plain delete work.
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path
            if (path == null) {
                Timber.e("No path on file uri: $uri")
                return false
            }
            return runCatching { File(path).delete() }
                .onFailure { Timber.e(it, "Failed to delete source file: $uri") }
                .getOrDefault(false)
        }

        runCatching {
            // Write access granted by our own OpenDocument picker. Absent for URIs handed to us
            // by another app, which typically grant read only.
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        // Only documents from a DocumentsProvider can be deleted this way. A third-party
        // FileProvider URI (a file manager sharing us an APK) has no delete method to call, and
        // there is no supported way to remove it — we report that rather than guessing at a path.
        return runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            .onFailure { Timber.e(it, "Failed to delete source file: $uri") }
            .getOrDefault(false)
    }

    /** [deleteSourceFile], plus a toast when the file survived so the user isn't left guessing. */
    protected suspend fun deleteSourceFileAndWarn(context: Context, uri: Uri) {
        if (deleteSourceFile(context, uri)) {
            Timber.d("Deleted source file: $uri")
            return
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.install_delete_source_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private suspend fun deleteSourceFileIfNeeded(sessionId: UUID, context: Context?) {
        if (deleteFlags[sessionId] != true || context == null) return
        val uri = originalFileUris[sessionId] ?: return
        deleteSourceFileAndWarn(context, uri)
    }

    private fun handleError(message: String?, sessionId: UUID) {
        val err = ResolvableString.raw(message ?: "Installation failed")
        sessionDataRepository.setError(sessionId, err)
    }
}
