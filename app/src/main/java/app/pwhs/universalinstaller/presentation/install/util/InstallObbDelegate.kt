package app.pwhs.universalinstaller.presentation.install.util

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.work.WorkManager
import app.pwhs.universalinstaller.presentation.install.AttachedObb
import app.pwhs.universalinstaller.presentation.install.ObbCopyState
import app.pwhs.universalinstaller.presentation.install.ObbCopyWorker
import app.pwhs.universalinstaller.presentation.install.ObbEntry
import app.pwhs.universalinstaller.presentation.install.SafObbWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class InstallObbDelegate(
    private val application: Application,
    private val scope: CoroutineScope,
) {
    private val _obbCopyState = MutableStateFlow<ObbCopyState>(ObbCopyState.Idle)
    val obbCopyState: StateFlow<ObbCopyState> = _obbCopyState.asStateFlow()

    private val _attachedObbFiles = MutableStateFlow<List<AttachedObb>>(emptyList())
    val attachedObbFiles: StateFlow<List<AttachedObb>> = _attachedObbFiles.asStateFlow()

    var pendingObbEntries: List<ObbEntry> = emptyList()
        private set

    var pendingObbCopyJob: ObbCopyJob? = null
        private set

    private var obbWorkerObserverJob: Job? = null

    init {
        reattachRunningWorker()
    }

    private fun reattachRunningWorker() {
        scope.launch {
            try {
                val wm = WorkManager.getInstance(application)
                val active = wm.getWorkInfosByTag(ObbCopyWorker.WORK_TAG).get().firstOrNull { !it.state.isFinished }
                if (active != null) {
                    observeObbWorker(active.id, "", "")
                }
            } catch (_: Throwable) {
                // Best-effort re-attach
            }
        }
    }

    fun setPendingEntries(entries: List<ObbEntry>) {
        pendingObbEntries = entries
    }

    fun clear() {
        pendingObbEntries = emptyList()
        _attachedObbFiles.value = emptyList()
    }

    fun restore(entries: List<ObbEntry>, attached: List<AttachedObb>) {
        pendingObbEntries = entries
        _attachedObbFiles.value = attached
    }

    suspend fun copyObbFiles(
        src: Uri?,
        obbs: List<ObbEntry>,
        attached: List<AttachedObb>,
        pkg: String,
        name: String,
    ) {
        InstallObbHelper.copyObbFiles(
            context = application,
            sourceUri = src,
            entries = obbs,
            attached = attached,
            packageName = pkg,
            appName = name,
            onStateChanged = { _obbCopyState.value = it },
            onJobCreated = { pendingObbCopyJob = it },
            onObserveWork = { id, app, p -> observeObbWorker(id, app, p) },
        )
    }

    private fun observeObbWorker(workId: UUID, appName: String, packageName: String) {
        obbWorkerObserverJob?.cancel()
        obbWorkerObserverJob = scope.launch {
            InstallObbHelper.observeObbWorker(application, workId, appName, packageName) { state ->
                _obbCopyState.value = state
                if (state !is ObbCopyState.Running) {
                    pendingObbCopyJob = null
                }
            }
        }
    }

    fun onObbTreeGranted(uri: Uri?) {
        val job = pendingObbCopyJob ?: return
        if (uri == null) {
            pendingObbCopyJob = null
            _obbCopyState.value = ObbCopyState.Error(job.appName, "OBB folder access not granted")
            return
        }
        if (!SafObbWriter.isTreeForObbOf(uri, job.packageName)) {
            pendingObbCopyJob = null
            _obbCopyState.value = ObbCopyState.Error(job.appName, "Wrong folder picked — expected Android/obb/${job.packageName}/")
            return
        }
        try {
            application.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // Best-effort
        }
        scope.launch {
            InstallObbHelper.saveObbTreeGrant(application, job.packageName, uri)
            copyObbFiles(job.sourceUri, job.entries, job.attached, job.packageName, job.appName)
        }
    }

    fun obbTreeHintUri(): Uri? {
        val job = pendingObbCopyJob ?: return null
        return SafObbWriter.buildObbTreeHintUri(job.packageName)
    }

    fun dismissObbCopy() {
        _obbCopyState.value = ObbCopyState.Idle
    }

    fun attachObbFile(context: Context, uri: Uri) {
        _attachedObbFiles.value = InstallObbHelper.attachObbFile(context, uri, _attachedObbFiles.value)
    }

    fun removeAttachedObb(uri: Uri) {
        _attachedObbFiles.value = _attachedObbFiles.value.filterNot { it.uri == uri }
    }
}
