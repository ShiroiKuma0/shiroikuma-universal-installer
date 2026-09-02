package app.pwhs.universalinstaller.presentation.install.util

import android.content.Context
import android.net.Uri
import app.pwhs.core.network.DownloadProgress
import app.pwhs.core.network.DownloadResult
import app.pwhs.core.network.NetworkApkDownloader
import app.pwhs.universalinstaller.presentation.install.DownloadNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Process-scoped background downloader so network downloads survive Activity / ViewModel disposal
 * when the user dismisses the dialog or places the app in the background.
 */
object BackgroundPackageDownloader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    private val _currentProgress = MutableStateFlow<DownloadProgress?>(null)
    val currentProgress: StateFlow<DownloadProgress?> = _currentProgress.asStateFlow()

    fun download(
        context: Context,
        uri: Uri,
        onProgress: (DownloadProgress) -> Unit = {},
        onSuccess: (File, String) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {},
    ) {
        cancel(context)
        val appContext = context.applicationContext
        val notifier = DownloadNotifier(appContext)
        val displayName = uri.lastPathSegment?.substringBefore('?')?.ifBlank { "download.apk" } ?: "download.apk"

        val unregister = DownloadCancellationManager.register {
            cancel(appContext)
        }

        activeJob = scope.launch {
            try {
                val downloader = NetworkApkDownloader(appContext)
                when (val result = downloader.download(uri.toString()) { progress ->
                    _currentProgress.value = progress
                    onProgress(progress)
                    notifier.notifyProgress(displayName, progress.bytesDownloaded, progress.totalBytes)
                }) {
                    is DownloadResult.Success -> {
                        _currentProgress.value = null
                        val fileUri = Uri.fromFile(result.file)
                        notifier.notifyDone(result.fileName, fileUri)
                        onSuccess(result.file, result.fileName)
                    }
                    is DownloadResult.Error -> {
                        _currentProgress.value = null
                        notifier.notifyFailed(result.message)
                        onError(result.message)
                    }
                    DownloadResult.Cancelled -> {
                        _currentProgress.value = null
                        notifier.cancel()
                    }
                }
            } catch (e: Throwable) {
                Timber.e(e, "Background download failed")
                _currentProgress.value = null
                notifier.notifyFailed(e.localizedMessage ?: "Download failed")
                onError(e.localizedMessage ?: "Download failed")
            } finally {
                unregister()
            }
        }
    }

    fun cancel(context: Context? = null) {
        activeJob?.cancel()
        activeJob = null
        _currentProgress.value = null
        context?.let { DownloadNotifier(it.applicationContext).cancel() }
    }
}
