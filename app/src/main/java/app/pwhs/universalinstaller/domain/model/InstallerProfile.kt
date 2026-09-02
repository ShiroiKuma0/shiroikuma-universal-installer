package app.pwhs.universalinstaller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InstallerProfile(
    val id: String,
    val name: String,
    val installerPackageName: String? = null,
    val preferredBackend: String? = null, // "Root", "Shizuku", "Default"
    val replaceExisting: Boolean? = null,
    val allowTest: Boolean? = null,
    val requestDowngrade: Boolean? = null,
    val grantAllPermissions: Boolean? = null,
    val bypassLowTargetSdk: Boolean? = null,
    val allUsers: Boolean? = null,
    val allowRestrictedPermissions: Boolean? = null,
    val dontKillApp: Boolean? = null,
    val disableVerification: Boolean? = null,
    val enableRollback: Boolean? = null,
    val requestUpdateOwnership: Boolean? = null,
    val targetUserId: Int? = null,
)
