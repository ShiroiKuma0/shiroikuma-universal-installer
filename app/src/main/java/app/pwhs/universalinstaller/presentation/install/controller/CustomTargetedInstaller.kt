package app.pwhs.universalinstaller.presentation.install.controller

import android.content.Context
import android.net.Uri
import android.os.Build
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.presentation.install.dialog.InstallerOverrides
import app.pwhs.universalinstaller.presentation.setting.DEFAULT_INSTALLER_PACKAGE_NAME
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.util.CustomShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID

/**
 * Installs APKs via custom shell/authorizer commands (`pm install-create/write/commit`).
 *
 * Counterpart of [RootTargetedInstaller] which routes through libsu Shell;
 * CustomTargetedInstaller routes through [CustomShellExecutor].
 */
object CustomTargetedInstaller {

    suspend fun install(
        context: Context,
        uris: List<Uri>,
        userId: Int,
        packageName: String = "",
        allowDowngrade: Boolean = false,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val createArgs = buildCreateArgs(context, packageName, allowDowngrade)
            val stagingDir = File(context.cacheDir, "custom_install_${UUID.randomUUID()}").apply {
                mkdirs()
            }
            val stagedFiles = try {
                uris.mapIndexed { idx, uri ->
                    val out = File(stagingDir, "split_$idx.apk")
                    context.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Failed to open URI $uri" }
                        out.outputStream().use { input.copyTo(it) }
                    }
                    out.setReadable(true, false)
                    out
                }.also {
                    stagingDir.setReadable(true, false)
                    stagingDir.setExecutable(true, false)
                }
            } catch (e: Exception) {
                stagingDir.deleteRecursively()
                throw e
            }

            try {
                val sessionId = createSession(context, userId, createArgs)
                onProgress(0.1f)

                stagedFiles.forEachIndexed { idx, file ->
                    writeSplit(context, sessionId, idx, file)
                    onProgress(0.1f + 0.8f * ((idx + 1).toFloat() / stagedFiles.size))
                }

                commitSession(context, sessionId)
                onProgress(1f)
            } finally {
                stagingDir.deleteRecursively()
            }
        }
    }

    private suspend fun buildCreateArgs(
        context: Context,
        packageName: String,
        allowDowngrade: Boolean,
    ): List<String> {
        val prefs = runCatching { context.dataStore.data.first() }.getOrNull()
        val args = mutableListOf<String>()
        if (prefs?.get(PreferencesKeys.ROOT_REPLACE_EXISTING) != false) args += "-r"
        if (allowDowngrade || prefs?.get(PreferencesKeys.ROOT_REQUEST_DOWNGRADE) == true) args += "-d"
        if (prefs?.get(PreferencesKeys.ROOT_ALLOW_TEST) == true) args += "-t"
        if (prefs?.get(PreferencesKeys.ROOT_GRANT_ALL_PERMISSIONS) == true) args += "-g"
        if (prefs?.get(PreferencesKeys.ROOT_BYPASS_LOW_TARGET_SDK) == true &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) args += "--bypass-low-target-sdk-block"
        if (prefs?.get(PreferencesKeys.ROOT_DONT_KILL_APP) == true) args += "--dont-kill"
        if (prefs?.get(PreferencesKeys.ROOT_DISABLE_VERIFICATION) == true &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) args += "--skip-verification"
        if (prefs?.get(PreferencesKeys.ROOT_ENABLE_ROLLBACK) == true &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) args += "--enable-rollback"
        if (prefs?.get(PreferencesKeys.ROOT_REQUEST_UPDATE_OWNERSHIP) == true &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) args += "--update-ownership"
        if (prefs?.get(PreferencesKeys.ROOT_SET_INSTALL_SOURCE) == true) {
            val override = if (packageName.isNotBlank()) {
                InstallerOverrides.get(prefs[PreferencesKeys.INSTALLER_OVERRIDES], packageName)
            } else null
            val installer = override
                ?: prefs[PreferencesKeys.ROOT_INSTALLER_PACKAGE_NAME]?.trim()?.ifBlank { DEFAULT_INSTALLER_PACKAGE_NAME }
                ?: DEFAULT_INSTALLER_PACKAGE_NAME
            if (installer.isNotBlank()) args += "-i $installer"
        }
        return args
    }

    private suspend fun createSession(context: Context, userId: Int, createArgs: List<String>): String {
        val userArg = if (userId >= 0) "--user $userId " else ""
        val flags = createArgs.joinToString(" ")
        val result = CustomShellExecutor.exec(context, "pm install-create $userArg$flags".trim())
        if (!result.isSuccess) {
            throw RuntimeException("pm install-create failed: ${joinErr(result)}")
        }
        val line = result.out.joinToString("\n")
        val sessionId = extractSessionId(line)
            ?: throw RuntimeException("Could not parse session id from: $line")
        Timber.d("CustomTargetedInstaller: created session=$sessionId user=$userId")
        return sessionId
    }

    private suspend fun writeSplit(context: Context, sessionId: String, index: Int, file: File) {
        val size = file.length()
        val splitName = "split_$index"
        val pathArg = file.absolutePath.replace(" ", "\\ ")
        val cmd = "pm install-write -S $size $sessionId $splitName $pathArg"
        var result = CustomShellExecutor.exec(context, cmd)
        if (!result.isSuccess) {
            val pipeCmd = "cat ${file.absolutePath.replace(" ", "\\ ")} | pm install-write -S $size $sessionId $splitName -"
            result = CustomShellExecutor.exec(context, pipeCmd)
            if (!result.isSuccess) {
                throw RuntimeException("pm install-write failed for split_$index: ${joinErr(result)}")
            }
        }
    }

    private suspend fun commitSession(context: Context, sessionId: String) {
        val result = CustomShellExecutor.exec(context, "pm install-commit $sessionId")
        val stdout = result.out.joinToString("\n")
        if (!result.isSuccess || !stdout.contains("Success", ignoreCase = true)) {
            throw RuntimeException(stdout.ifBlank { joinErr(result) }.ifBlank { "install-commit failed" })
        }
    }

    private fun extractSessionId(text: String): String? {
        val regex = Regex("""session\s*\[(\d+)]""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    private fun joinErr(result: CustomShellExecutor.Result): String =
        result.err.joinToString("\n").ifBlank { result.out.joinToString("\n") }
}
