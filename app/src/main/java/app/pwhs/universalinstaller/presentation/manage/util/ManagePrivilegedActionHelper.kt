package app.pwhs.universalinstaller.presentation.manage.util

import android.content.Context
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.provider.PrivilegedExecutor
import app.pwhs.universalinstaller.domain.provider.PrivilegedProvider
import app.pwhs.universalinstaller.presentation.install.controller.InstallerBackendFactory
import app.pwhs.universalinstaller.presentation.install.controller.ShizukuShellExecutor
import app.pwhs.universalinstaller.presentation.manage.PrivilegedActionResult

object ManagePrivilegedActionHelper {

    suspend fun openAppPrivileged(
        context: Context,
        packageName: String,
        appName: String,
        privilegedProvider: PrivilegedProvider,
        backendFactory: InstallerBackendFactory,
    ): PrivilegedActionResult {
        val executor = privilegedProvider.resolveExecutor()
            ?: return PrivilegedActionResult.Failure(context.getString(R.string.manage_privileged_unavailable))

        val result = when (executor) {
            PrivilegedExecutor.Root -> backendFactory.launchAppViaRoot(packageName)
            PrivilegedExecutor.Shizuku -> ShizukuShellExecutor.launchApp(packageName)
        }
        return if (!result.isSuccess) {
            PrivilegedActionResult.Failure("Launch failed: ${result.exceptionOrNull()?.message ?: "unknown error"}")
        } else {
            PrivilegedActionResult.Success(context.getString(R.string.manage_action_open_app, appName))
        }
    }

    suspend fun forceStop(
        context: Context,
        packageName: String,
        appName: String,
        privilegedProvider: PrivilegedProvider,
        backendFactory: InstallerBackendFactory,
    ): PrivilegedActionResult {
        if (packageName == context.packageName) {
            return PrivilegedActionResult.Failure(context.getString(R.string.manage_action_force_stop_self_blocked))
        }
        val executor = privilegedProvider.resolveExecutor()
            ?: return PrivilegedActionResult.Failure(context.getString(R.string.manage_privileged_unavailable))

        val result = when (executor) {
            PrivilegedExecutor.Root -> backendFactory.forceStopViaRoot(packageName)
            PrivilegedExecutor.Shizuku -> ShizukuShellExecutor.forceStop(packageName)
        }
        return if (result.isSuccess) {
            PrivilegedActionResult.Success(context.getString(R.string.manage_action_force_stop_done, appName))
        } else {
            PrivilegedActionResult.Failure(
                context.getString(
                    R.string.manage_action_force_stop_failed,
                    result.exceptionOrNull()?.message ?: "unknown error",
                )
            )
        }
    }

    suspend fun setEnabled(
        context: Context,
        packageName: String,
        appName: String,
        enabled: Boolean,
        privilegedProvider: PrivilegedProvider,
        backendFactory: InstallerBackendFactory,
    ): PrivilegedActionResult {
        if (packageName == context.packageName) {
            return PrivilegedActionResult.Failure(context.getString(R.string.manage_action_disable_self_blocked))
        }
        val executor = privilegedProvider.resolveExecutor()
            ?: return PrivilegedActionResult.Failure(context.getString(R.string.manage_privileged_unavailable))

        val result = when (executor) {
            PrivilegedExecutor.Root -> backendFactory.setEnabledViaRoot(packageName, enabled)
            PrivilegedExecutor.Shizuku -> ShizukuShellExecutor.setEnabled(packageName, enabled)
        }
        return if (result.isSuccess) {
            PrivilegedActionResult.Success(
                context.getString(
                    if (enabled) R.string.manage_action_enable_done else R.string.manage_action_disable_done,
                    appName,
                )
            )
        } else {
            PrivilegedActionResult.Failure(
                context.getString(
                    if (enabled) R.string.manage_action_enable_failed else R.string.manage_action_disable_failed,
                    result.exceptionOrNull()?.message ?: "unknown error",
                )
            )
        }
    }

    suspend fun clearAllData(
        context: Context,
        packageName: String,
        appName: String,
        privilegedProvider: PrivilegedProvider,
        backendFactory: InstallerBackendFactory,
    ): PrivilegedActionResult {
        if (packageName == context.packageName) {
            return PrivilegedActionResult.Failure(context.getString(R.string.manage_action_clear_data_self_blocked))
        }
        val executor = privilegedProvider.resolveExecutor()
            ?: return PrivilegedActionResult.Failure(context.getString(R.string.manage_privileged_unavailable))

        val result = when (executor) {
            PrivilegedExecutor.Root -> backendFactory.clearAppDataViaRoot(packageName)
            PrivilegedExecutor.Shizuku -> ShizukuShellExecutor.clearAppData(packageName)
        }
        return if (result.isSuccess) {
            PrivilegedActionResult.Success(context.getString(R.string.manage_action_clear_data_done, appName))
        } else {
            PrivilegedActionResult.Failure(
                context.getString(
                    R.string.manage_action_clear_data_failed,
                    result.exceptionOrNull()?.message ?: "unknown error",
                )
            )
        }
    }

    suspend fun runPrivilegedBatch(
        context: Context,
        packages: List<String>,
        actionLabelRes: Int,
        privilegedProvider: PrivilegedProvider,
        op: suspend (PrivilegedExecutor, String) -> Result<*>,
    ): PrivilegedActionResult {
        val executor = privilegedProvider.resolveExecutor()
            ?: return PrivilegedActionResult.Failure(context.getString(R.string.manage_privileged_unavailable))

        var success = 0
        var failed = 0
        for (pkg in packages) {
            if (pkg == context.packageName) continue
            if (op(executor, pkg).isSuccess) success++ else failed++
        }
        val label = context.getString(actionLabelRes)
        return if (failed == 0) {
            PrivilegedActionResult.Success(
                context.getString(R.string.manage_batch_result_success, label, success)
            )
        } else {
            PrivilegedActionResult.Failure(
                context.getString(R.string.manage_batch_result_partial, label, success, failed)
            )
        }
    }
}
