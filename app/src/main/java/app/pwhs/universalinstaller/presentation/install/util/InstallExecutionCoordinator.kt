package app.pwhs.universalinstaller.presentation.install.util

import android.app.Application
import android.net.Uri
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.manager.ProfileManager
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.SessionData
import app.pwhs.universalinstaller.presentation.install.AttachedObb
import app.pwhs.universalinstaller.presentation.install.BatchApkEntry
import app.pwhs.universalinstaller.presentation.install.DialogTarget
import app.pwhs.universalinstaller.presentation.install.ObbEntry
import app.pwhs.universalinstaller.presentation.install.controller.BaseInstallController
import app.pwhs.universalinstaller.presentation.install.controller.InstallerBackendFactory
import app.pwhs.universalinstaller.presentation.install.controller.ManualInstallController
import app.pwhs.universalinstaller.presentation.install.dialog.isDowngrade
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.telemetry.Telemetry
import app.pwhs.universalinstaller.telemetry.TelemetryEvents
import app.pwhs.universalinstaller.util.extension.getDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import java.util.UUID

object InstallExecutionCoordinator {

    suspend fun executeSingleInstall(
        application: Application,
        scope: CoroutineScope,
        appScope: CoroutineScope,
        trackDialogTarget: Boolean,
        apkInfo: ApkInfo?,
        fileName: String,
        originalUri: Uri?,
        uris: List<Uri>,
        obbEntries: List<ObbEntry>,
        attachedObbs: List<AttachedObb>,
        currentProfileId: String?,
        rootController: BaseInstallController?,
        backendFactory: InstallerBackendFactory,
        manualController: ManualInstallController,
        resolveActiveController: suspend (String?) -> BaseInstallController,
        onDialogTargetCreated: (DialogTarget) -> Unit,
        onCopyObbs: suspend (Uri?, List<ObbEntry>, List<AttachedObb>, String, String) -> Unit,
    ) {
        val prefs = try { application.dataStore.data.first() } catch (_: Exception) { null }
        val profiles = ProfileManager.parseProfiles(prefs?.get(PreferencesKeys.INSTALLER_PROFILES))
        val profile = profiles.find { it.id == currentProfileId }
        val iconPath = InstallSessionManager.cacheIcon(application, apkInfo)
        val deleteAfterInstall = InstallSessionManager.readDeleteApkPref(application)
        val sessionData = SessionData(
            id = UUID.randomUUID(),
            name = fileName,
            appName = apkInfo?.appName ?: "",
            packageName = apkInfo?.packageName ?: "",
            iconPath = iconPath,
        )
        val hasZipObbs = obbEntries.isNotEmpty() && originalUri != null
        val hasAttachedObbs = attachedObbs.isNotEmpty()
        val onSuccess: (suspend () -> Unit)? = if ((hasZipObbs || hasAttachedObbs) && apkInfo != null) {
            val pkg = apkInfo.packageName
            val appName = apkInfo.appName.ifBlank { pkg }
            val callback: suspend () -> Unit = {
                onCopyObbs(originalUri, obbEntries, attachedObbs, pkg, appName)
            }
            callback
        } else {
            null
        }

        val pkgForTarget = apkInfo?.packageName.orEmpty()
        val nameForTarget = apkInfo?.appName.orEmpty().ifBlank { fileName }
        val controller = resolveActiveController(currentProfileId)
        val targetedUserId = profile?.targetUserId ?: prefs?.get(PreferencesKeys.INSTALL_USER_ID)

        if (targetedUserId != null) {
            val targetedBackend = InstallSessionManager.resolveTargetedBackend(profile?.preferredBackend, rootController, backendFactory)
            if (targetedBackend == null) {
                android.widget.Toast.makeText(application, application.getString(R.string.install_targeted_no_backend), android.widget.Toast.LENGTH_LONG).show()
                return
            }
            manualController.installTargeted(
                uris = uris,
                sessionData = sessionData,
                userId = targetedUserId,
                backend = targetedBackend,
                scope = if (trackDialogTarget) appScope else scope,
                originalUri = originalUri,
                deleteAfterInstall = deleteAfterInstall,
                onSessionCreated = if (trackDialogTarget) {
                    { realId -> onDialogTargetCreated(DialogTarget(realId, pkgForTarget, nameForTarget, iconPath, originalUri)) }
                } else null,
            )
        } else {
            InstallSessionManager.writeProfileFlags(application, profile)
            controller.install(
                uris = uris,
                sessionData = sessionData,
                scope = if (trackDialogTarget) appScope else scope,
                context = application,
                originalUri = originalUri,
                deleteAfterInstall = deleteAfterInstall,
                allowDowngrade = apkInfo != null && isDowngrade(apkInfo),
                onSuccess = onSuccess,
                onSessionCreated = if (trackDialogTarget) {
                    { realId -> onDialogTargetCreated(DialogTarget(realId, pkgForTarget, nameForTarget, iconPath, originalUri)) }
                } else null,
            )
        }
    }

    suspend fun executeBatchInstall(
        application: Application,
        scope: CoroutineScope,
        picked: List<BatchApkEntry>,
        currentProfileId: String?,
        resolveActiveController: suspend (String?) -> BaseInstallController,
    ) {
        if (picked.isEmpty()) return
        Telemetry.feature(TelemetryEvents.FEATURE_BATCH_INSTALL)
        val deleteAfterInstall = InstallSessionManager.readDeleteApkPref(application)
        val prefs = try { application.dataStore.data.first() } catch (_: Exception) { null }
        val profile = ProfileManager.parseProfiles(prefs?.get(PreferencesKeys.INSTALLER_PROFILES)).find { it.id == currentProfileId }
        InstallSessionManager.writeProfileFlags(application, profile)
        val controller = resolveActiveController(currentProfileId)
        for (entry in picked) {
            val iconPath = InstallSessionManager.cacheIcon(application, entry.apkInfo)
            val sessionData = SessionData(
                id = UUID.randomUUID(),
                name = entry.fileName,
                appName = entry.apkInfo.appName,
                packageName = entry.apkInfo.packageName,
                iconPath = iconPath,
            )
            controller.install(
                uris = entry.splitUris,
                sessionData = sessionData,
                scope = scope,
                context = application,
                originalUri = entry.uri,
                deleteAfterInstall = deleteAfterInstall,
                allowDowngrade = isDowngrade(entry.apkInfo),
                onSuccess = null,
            )
        }
    }

    suspend fun executeSkipSingle(
        application: Application,
        scope: CoroutineScope,
        uri: Uri,
        fileName: String,
        sessionId: UUID,
        resolveActiveController: suspend () -> BaseInstallController,
        onSuccess: () -> Unit,
    ) {
        val deleteAfterInstall = InstallSessionManager.readDeleteApkPref(application)
        val controller = resolveActiveController()
        val sessionData = SessionData(sessionId, fileName, fileName, "", null)
        controller.install(
            uris = listOf(uri),
            sessionData = sessionData,
            scope = scope,
            context = application,
            originalUri = uri,
            deleteAfterInstall = deleteAfterInstall,
            onSuccess = onSuccess,
        )
    }

    suspend fun executeSkipBatch(
        application: Application,
        scope: CoroutineScope,
        uris: List<Uri>,
        resolveActiveController: suspend () -> BaseInstallController,
    ) {
        val deleteAfterInstall = InstallSessionManager.readDeleteApkPref(application)
        val controller = resolveActiveController()
        for (uri in uris) {
            val fileName = application.contentResolver.getDisplayName(uri)
            val sessionData = SessionData(UUID.randomUUID(), fileName, fileName, "", null)
            controller.install(
                uris = listOf(uri),
                sessionData = sessionData,
                scope = scope,
                context = application,
                originalUri = uri,
                deleteAfterInstall = deleteAfterInstall,
                onSuccess = null,
            )
        }
    }
}
