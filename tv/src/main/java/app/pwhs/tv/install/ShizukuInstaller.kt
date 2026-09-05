package app.pwhs.tv.install

import android.content.Context
import android.net.Uri
import app.pwhs.core.install.ApkInstaller
import app.pwhs.core.data.local.SharedPrefsKeys
import app.pwhs.core.data.local.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.progress
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.shizuku.shizuku
import ru.solrudev.ackpine.splits.ApkSplits.validate
import ru.solrudev.ackpine.splits.CloseableSequence
import ru.solrudev.ackpine.splits.SplitPackage.Companion.toSplitPackage
import ru.solrudev.ackpine.splits.ZippedApkSplits
import ru.solrudev.ackpine.splits.get

/** Uses the same privileged Ackpine backend as the phone app. */
class ShizukuInstaller(private val context: Context) {
    @OptIn(DelicateAckpineApi::class)
    suspend fun install(uri: Uri, isBundle: Boolean, onProgress: (Float) -> Unit): ApkInstaller.Result =
        coroutineScope {
            try {
                val prefs = context.dataStore.data.first()
                val uris = if (isBundle) compatibleSplits(uri) else listOf(uri)
                require(uris.isNotEmpty()) { "No APK found in bundle" }
                val session = PackageInstaller.getInstance(context).createSession(uris) {
                    confirmation = Confirmation.IMMEDIATE
                    shizuku {
                        replaceExisting = prefs[SharedPrefsKeys.TV_SHIZUKU_REPLACE] ?: true
                        requestDowngrade = prefs[SharedPrefsKeys.TV_SHIZUKU_DOWNGRADE] ?: false
                        grantAllRequestedPermissions = prefs[SharedPrefsKeys.TV_SHIZUKU_GRANT] ?: false
                        allowTest = prefs[SharedPrefsKeys.TV_SHIZUKU_TEST] ?: false
                        allUsers = prefs[SharedPrefsKeys.TV_SHIZUKU_ALL_USERS] ?: false
                    }
                }
                val progressJob = launch {
                    session.progress.collect {
                        if (it.max > 0) onProgress((it.progress.toFloat() / it.max).coerceIn(0f, 1f))
                    }
                }
                try {
                    when (val result = session.await()) {
                        Session.State.Succeeded -> ApkInstaller.Result.Success
                        is Session.State.Failed -> ApkInstaller.Result.Failure(result.failure.toString())
                    }
                } catch (e: CancellationException) {
                    session.cancel()
                    throw e
                } finally {
                    progressJob.cancel()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApkInstaller.Result.Failure(e.message ?: e.javaClass.simpleName)
            }
        }

    private suspend fun compatibleSplits(uri: Uri): List<Uri> {
        val sequence = ZippedApkSplits.getApksForUri(uri, context)
            .validate().toSplitPackage().filterCompatible(context).get()
        return try {
            sequence.toList().map { it.apk.uri }.toList()
        } finally {
            (sequence as? CloseableSequence<*>)?.close()
        }
    }
}
