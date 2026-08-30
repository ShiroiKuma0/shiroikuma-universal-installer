package app.pwhs.universalinstaller.presentation.manage.util

import android.content.Context
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.data.local.UninstallLogDao
import app.pwhs.universalinstaller.data.local.UninstallLogEntity
import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.domain.provider.PrivilegedExecutor
import app.pwhs.universalinstaller.domain.provider.PrivilegedProvider
import app.pwhs.universalinstaller.presentation.install.controller.InstallerBackendFactory
import app.pwhs.universalinstaller.presentation.install.controller.ShizukuShellExecutor
import app.pwhs.universalinstaller.presentation.install.controller.SystemAppMethod
import app.pwhs.universalinstaller.presentation.manage.SystemAppPrompt
import app.pwhs.universalinstaller.presentation.manage.UninstallNotifier
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.telemetry.Telemetry
import app.pwhs.universalinstaller.telemetry.TelemetryEvents
import kotlinx.coroutines.flow.first
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.shizuku.shizuku
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.createSession
import timber.log.Timber

data class UninstallOptions(
    val useShizuku: Boolean,
    val keepData: Boolean,
    val allUsers: Boolean,
)

object ManageUninstallHelper {

    suspend fun readUninstallOptions(context: Context): UninstallOptions {
        return try {
            val prefs = context.dataStore.data.first()
            val useShizuku = prefs[PreferencesKeys.USE_SHIZUKU] ?: false
            UninstallOptions(
                useShizuku = useShizuku,
                keepData = useShizuku && (prefs[PreferencesKeys.SHIZUKU_UNINSTALL_KEEP_DATA] ?: false),
                allUsers = useShizuku && (prefs[PreferencesKeys.SHIZUKU_UNINSTALL_ALL_USERS] ?: false),
            )
        } catch (_: Exception) {
            UninstallOptions(useShizuku = false, keepData = false, allUsers = false)
        }
    }

    suspend fun uninstallSingle(
        context: Context,
        packageName: String,
        appName: String,
        packageUninstaller: PackageUninstaller,
        uninstallLogDao: UninstallLogDao,
        notifier: UninstallNotifier,
        onSuccess: () -> Unit,
    ): Boolean {
        val opts = readUninstallOptions(context)
        val notifId = notifier.notifySingleStart(appName)
        val ok = performUninstall(packageName, appName, opts, packageUninstaller, uninstallLogDao)
        if (ok) onSuccess()
        notifier.notifySingleResult(notifId, appName, success = ok)
        return ok
    }

    suspend fun runBatchUninstall(
        context: Context,
        packages: List<String>,
        apps: List<InstalledApp>,
        packageUninstaller: PackageUninstaller,
        uninstallLogDao: UninstallLogDao,
        notifier: UninstallNotifier,
        onAppRemoved: (String) -> Unit,
    ) {
        Telemetry.feature(TelemetryEvents.FEATURE_UNINSTALL)
        val opts = readUninstallOptions(context)
        val total = packages.size
        val notifId = notifier.notifyBatchStart(total)
        var successful = 0
        var failed = 0
        val lookup = apps.associateBy { it.packageName }
        packages.forEachIndexed { index, pkg ->
            val appName = lookup[pkg]?.appName ?: pkg
            notifier.notifyBatchProgress(notifId, completed = index, total = total, currentAppName = appName)
            val ok = performUninstall(pkg, appName, opts, packageUninstaller, uninstallLogDao)
            if (ok) {
                successful++
                onAppRemoved(pkg)
            } else {
                failed++
            }
        }
        notifier.notifyBatchDone(notifId, successful = successful, failed = failed)
    }

    suspend fun handleSystemAppPromptConfirmation(
        context: Context,
        prompt: SystemAppPrompt,
        systemMethod: SystemAppMethod?,
        privilegedProvider: PrivilegedProvider,
        packageUninstaller: PackageUninstaller,
        uninstallLogDao: UninstallLogDao,
        backendFactory: InstallerBackendFactory,
        notifier: UninstallNotifier,
        apps: List<InstalledApp>,
        onAppRemoved: (String) -> Unit,
    ) {
        val executor = privilegedProvider.resolveExecutor()
        when (prompt) {
            is SystemAppPrompt.Single -> {
                if (systemMethod == null || executor == null) return
                val notifId = notifier.notifySingleStart(prompt.appName)
                val ok = performSystemUninstall(
                    prompt.pkg, prompt.appName, systemMethod, executor, backendFactory, uninstallLogDao
                )
                if (ok) onAppRemoved(prompt.pkg)
                notifier.notifySingleResult(notifId, prompt.appName, success = ok)
            }
            is SystemAppPrompt.Batch -> {
                val userPkgs = prompt.userApps.map { it.first }
                val systemPkgs = prompt.systemApps
                val runSystem = systemMethod != null && executor != null
                val totalToRun = userPkgs.size + (if (runSystem) systemPkgs.size else 0)
                if (totalToRun == 0) return
                val notifId = notifier.notifyBatchStart(totalToRun)
                var successful = 0
                var failed = 0
                var processed = 0

                val opts = readUninstallOptions(context)
                val lookup = apps.associateBy { it.packageName }
                for (pkg in userPkgs) {
                    val name = lookup[pkg]?.appName ?: pkg
                    notifier.notifyBatchProgress(notifId, completed = processed, total = totalToRun, currentAppName = name)
                    val ok = performUninstall(pkg, name, opts, packageUninstaller, uninstallLogDao)
                    if (ok) {
                        successful++
                        onAppRemoved(pkg)
                    } else {
                        failed++
                    }
                    processed++
                }
                if (runSystem) {
                    for ((pkg, name) in systemPkgs) {
                        notifier.notifyBatchProgress(notifId, completed = processed, total = totalToRun, currentAppName = name)
                        val ok = performSystemUninstall(pkg, name, systemMethod, executor, backendFactory, uninstallLogDao)
                        if (ok) {
                            successful++
                            onAppRemoved(pkg)
                        } else {
                            failed++
                        }
                        processed++
                    }
                }
                notifier.notifyBatchDone(notifId, successful = successful, failed = failed)
            }
            is SystemAppPrompt.PrivilegedRequired -> {
                if (prompt.userAppsAvailable.isNotEmpty()) {
                    runBatchUninstall(
                        context, prompt.userAppsAvailable, apps, packageUninstaller, uninstallLogDao, notifier, onAppRemoved
                    )
                }
            }
        }
    }

    suspend fun performUninstall(
        packageName: String,
        appName: String,
        opts: UninstallOptions,
        packageUninstaller: PackageUninstaller,
        uninstallLogDao: UninstallLogDao,
    ): Boolean {
        return try {
            val session = packageUninstaller.createSession(packageName) {
                confirmation = Confirmation.IMMEDIATE
                if (opts.useShizuku) {
                    shizuku {
                        keepData = opts.keepData
                        allUsers = opts.allUsers
                    }
                }
            }
            when (val result = session.await()) {
                Session.State.Succeeded -> {
                    Timber.d("Uninstalled $packageName successfully")
                    saveLog(uninstallLogDao, packageName, appName, success = true, errorMessage = null)
                    true
                }
                is Session.State.Failed -> {
                    val reason = result.failure.message?.takeIf { it.isNotBlank() }
                        ?: "Uninstall failed (no reason reported)"
                    Timber.e("Failed to uninstall $packageName — $reason")
                    saveLog(uninstallLogDao, packageName, appName, success = false, errorMessage = reason)
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error uninstalling $packageName")
            saveLog(uninstallLogDao, packageName, appName, success = false, errorMessage = e.message ?: e::class.java.simpleName)
            false
        }
    }

    suspend fun performSystemUninstall(
        packageName: String,
        appName: String,
        method: SystemAppMethod,
        executor: PrivilegedExecutor,
        backendFactory: InstallerBackendFactory,
        uninstallLogDao: UninstallLogDao,
    ): Boolean {
        val result = when (executor) {
            PrivilegedExecutor.Root -> backendFactory.uninstallSystemAppViaRoot(packageName, method)
            PrivilegedExecutor.Shizuku -> ShizukuShellExecutor.uninstallSystemApp(packageName, method)
        }
        return if (result.isSuccess) {
            Timber.d("System app removed via $executor/$method: $packageName")
            saveLog(uninstallLogDao, packageName, appName, success = true, errorMessage = null)
            true
        } else {
            val err = result.exceptionOrNull()?.message ?: "Privileged shell command failed"
            Timber.e("System app removal failed ($executor/$method) for $packageName: $err")
            saveLog(uninstallLogDao, packageName, appName, success = false, errorMessage = err)
            false
        }
    }

    fun partitionByKind(
        packages: List<String>,
        apps: List<InstalledApp>,
    ): Pair<List<Pair<String, String>>, List<Pair<String, String>>> {
        val lookup = apps.associateBy { it.packageName }
        val system = mutableListOf<Pair<String, String>>()
        val user = mutableListOf<Pair<String, String>>()
        for (pkg in packages) {
            val app = lookup[pkg]
            val entry = pkg to (app?.appName ?: pkg)
            if (app?.isSystemApp == true) system += entry else user += entry
        }
        return system to user
    }

    private suspend fun saveLog(
        dao: UninstallLogDao,
        packageName: String,
        appName: String,
        success: Boolean,
        errorMessage: String?,
    ) {
        try {
            dao.insert(
                UninstallLogEntity(
                    packageName = packageName,
                    appName = appName,
                    success = success,
                    errorMessage = errorMessage,
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist uninstall log")
        }
    }
}
