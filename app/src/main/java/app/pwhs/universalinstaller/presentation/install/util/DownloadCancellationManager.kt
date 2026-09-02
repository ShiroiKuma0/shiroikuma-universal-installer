package app.pwhs.universalinstaller.presentation.install.util

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry to allow external components (like [app.pwhs.universalinstaller.presentation.install.InstallActionReceiver])
 * to cancel any in-progress download jobs.
 */
object DownloadCancellationManager {
    private val cancelHandlers = CopyOnWriteArrayList<() -> Unit>()

    fun register(onCancel: () -> Unit): () -> Unit {
        cancelHandlers.add(onCancel)
        return { cancelHandlers.remove(onCancel) }
    }

    fun cancelActiveDownloads() {
        for (handler in cancelHandlers) {
            runCatching { handler.invoke() }
        }
        cancelHandlers.clear()
    }
}
