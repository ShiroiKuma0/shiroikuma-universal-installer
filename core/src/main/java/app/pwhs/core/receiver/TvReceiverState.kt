package app.pwhs.core.receiver

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** An APK that arrived over the LAN receiver, staged on disk and ready to install. */
data class ReceivedApk(
    val path: String,
    val fileName: String,
    val sizeBytes: Long,
)

sealed interface ReceiverStatus {
    data object Stopped : ReceiverStatus
    /** Server is up. [url] is what the QR encodes; [token] guards uploads. */
    data class Running(
        val ip: String,
        val port: Int,
        val token: String,
        val url: String,
    ) : ReceiverStatus
}

/**
 * Process-wide bridge between the receiver foreground service (which owns the HTTP server)
 * and the TV UI (which renders the QR/status and triggers installs). Mirrors the mobile
 * app's `SyncManager` singleton pattern so the service and Compose screens share state
 * without binding.
 */
object TvReceiverState {
    private val _status = MutableStateFlow<ReceiverStatus>(ReceiverStatus.Stopped)
    val status: StateFlow<ReceiverStatus> = _status.asStateFlow()

    // replay = 1 so a freshly-composed screen still sees the most recent arrival.
    private val _received = MutableSharedFlow<ReceivedApk>(replay = 1, extraBufferCapacity = 8)
    val received: SharedFlow<ReceivedApk> = _received.asSharedFlow()

    fun setStatus(status: ReceiverStatus) {
        _status.value = status
    }

    fun emitReceived(apk: ReceivedApk) {
        _received.tryEmit(apk)
    }
}
