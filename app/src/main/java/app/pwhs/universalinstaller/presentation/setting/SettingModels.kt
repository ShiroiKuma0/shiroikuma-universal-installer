package app.pwhs.universalinstaller.presentation.setting

import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.domain.ThemeMode
import app.pwhs.universalinstaller.domain.model.InstallerProfile
import app.pwhs.universalinstaller.presentation.install.controller.RootState

const val DEFAULT_INSTALLER_PACKAGE_NAME = "com.android.vending"

data class SettingThemeState(
    val mode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val amoledMode: Boolean = false,
    val themePreset: AppThemePreset = AppThemePreset.Orange,
)

data class SyncOptions(
    val requirePin: Boolean = true,
    val pinCode: String = "",
    val serverPort: String = "8080",
)

data class CommonInstallOptions(
    val replaceExisting: Boolean,
    val requestDowngrade: Boolean,
    val grantAllPermissions: Boolean,
    val allowTest: Boolean,
    val bypassLowTargetSdk: Boolean,
    val allUsers: Boolean,
    val setInstallSource: Boolean,
    val installerPackageName: String,
    val allowRestrictedPermissions: Boolean = false,
    val dontKillApp: Boolean = false,
    val disableVerification: Boolean = false,
    val enableRollback: Boolean = false,
    val requestUpdateOwnership: Boolean = false,
)

fun ShizukuOptions.asCommon() = CommonInstallOptions(
    replaceExisting = replaceExisting,
    requestDowngrade = requestDowngrade,
    grantAllPermissions = grantAllPermissions,
    allowTest = allowTest,
    bypassLowTargetSdk = bypassLowTargetSdk,
    allUsers = allUsers,
    setInstallSource = setInstallSource,
    installerPackageName = installerPackageName,
    allowRestrictedPermissions = allowRestrictedPermissions,
    dontKillApp = dontKillApp,
    disableVerification = disableVerification,
    enableRollback = enableRollback,
    requestUpdateOwnership = requestUpdateOwnership,
)

fun RootOptions.asCommon() = CommonInstallOptions(
    replaceExisting = replaceExisting,
    requestDowngrade = requestDowngrade,
    grantAllPermissions = grantAllPermissions,
    allowTest = allowTest,
    bypassLowTargetSdk = bypassLowTargetSdk,
    allUsers = allUsers,
    setInstallSource = setInstallSource,
    installerPackageName = installerPackageName,
    allowRestrictedPermissions = allowRestrictedPermissions,
    dontKillApp = dontKillApp,
    disableVerification = disableVerification,
    enableRollback = enableRollback,
    requestUpdateOwnership = requestUpdateOwnership,
)

enum class SecurityLevel {
    Normal,
    Strict;

    companion object {
        fun from(stored: String?, legacyStrict: Boolean = false): SecurityLevel =
            entries.firstOrNull { it.name == stored }
                ?: if (legacyStrict) Strict else Normal
    }
}

enum class InstallMode {
    DEFAULT,
    SHIZUKU,
    ROOT;

    companion object {
        fun from(useShizuku: Boolean, useRoot: Boolean): InstallMode = when {
            useRoot -> ROOT
            useShizuku -> SHIZUKU
            else -> DEFAULT
        }
    }
}

enum class ShizukuState {
    NOT_INSTALLED,   // no Shizuku app and no Sui — nothing to talk to
    NOT_RUNNING,     // Shizuku app installed but service not started (binder dead)
    UNSUPPORTED,     // pre-v11 Shizuku — modern API calls unavailable
    NO_PERMISSION,   // binder alive, permission not granted
    READY,           // binder alive, permission granted
}

data class ShizukuOptions(
    val bypassLowTargetSdk: Boolean = false,
    val allowTest: Boolean = false,
    val replaceExisting: Boolean = true,
    val requestDowngrade: Boolean = false,
    val grantAllPermissions: Boolean = false,
    val allUsers: Boolean = false,
    val setInstallSource: Boolean = false,
    val installerPackageName: String = DEFAULT_INSTALLER_PACKAGE_NAME,
    val allowRestrictedPermissions: Boolean = false,
    val dontKillApp: Boolean = false,
    val disableVerification: Boolean = false,
    val enableRollback: Boolean = false,
    val requestUpdateOwnership: Boolean = false,
    val uninstallKeepData: Boolean = false,
    val uninstallAllUsers: Boolean = false,
)

data class RootOptions(
    val bypassLowTargetSdk: Boolean = false,
    val allowTest: Boolean = false,
    val replaceExisting: Boolean = true,
    val requestDowngrade: Boolean = false,
    val grantAllPermissions: Boolean = false,
    val allUsers: Boolean = false,
    val setInstallSource: Boolean = false,
    val installerPackageName: String = DEFAULT_INSTALLER_PACKAGE_NAME,
    val allowRestrictedPermissions: Boolean = false,
    val dontKillApp: Boolean = false,
    val disableVerification: Boolean = false,
    val enableRollback: Boolean = false,
    val requestUpdateOwnership: Boolean = false,
)

data class SettingUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val amoledMode: Boolean = false,
    val themePreset: AppThemePreset = AppThemePreset.Orange,
    val useShizuku: Boolean = false,
    val useRoot: Boolean = false,
    val virusTotalApiKey: String = "",
    val deleteApkAfterInstall: Boolean = false,
    val autoOpenAfterInstall: Boolean = false,
    val shizukuState: ShizukuState = ShizukuState.NOT_INSTALLED,
    val shizukuAvailable: Boolean = false,
    val shizukuOptions: ShizukuOptions = ShizukuOptions(),
    val rootSupported: Boolean = false,
    val rootState: RootState = RootState.UNAVAILABLE,
    val rootOptions: RootOptions = RootOptions(),
    val syncOptions: SyncOptions = SyncOptions(),
    val appVersion: String = "",
    val biometricLockInstall: Boolean = false,
    val biometricLockUninstall: Boolean = false,
    val dialogInstallMode: Boolean = true,
    val autoConfirmExternalInstall: Boolean = false,
    val showDownloadTab: Boolean = true,
    val extractorOutputPath: String = "",
    val extractorFilenameTemplate: String = "{name}-{version}",
    val installerProfiles: List<InstallerProfile> = emptyList(),
    val appProfileMapping: Map<String, String> = emptyMap(),
    val isDefaultInstaller: Boolean = false,
    val selectedLanguage: String = "",
    /**
     * True when the device has at least one biometric or device-credential enrolled.
     * Used to inform the user that the toggles will be no-ops until they
     * enrol a fingerprint or set a screen lock.
     */
    val biometricEnrolmentAvailable: Boolean = false,
)
