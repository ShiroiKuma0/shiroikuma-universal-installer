package app.pwhs.updater.domain.model

import app.pwhs.updater.domain.matcher.SemVerComparator

data class TrackedApp(
    val packageName: String,
    val appName: String,
    val iconUrl: String? = null,
    val sourceType: UpdateSourceType,
    val sourceUrl: String,
    val currentVersionName: String,
    val currentVersionCode: Long,
    val latestVersionName: String? = null,
    val latestVersionCode: Long? = null,
    val latestReleaseTag: String? = null,
    val latestDownloadUrl: String? = null,
    val releaseNotes: String? = null,
    val publishedAt: Long? = null,
    val lastCheckedAt: Long = 0L,
    val includePrereleases: Boolean = false,
    val customRegexFilter: String? = null,
    val category: String? = null,
    val ignoredVersion: String? = null,
    val eTag: String? = null,
) {
    val isInstalled: Boolean
        get() = currentVersionName.isNotBlank() && !currentVersionName.equals("Not Installed", ignoreCase = true)

    val isVersionIgnored: Boolean
        get() = !ignoredVersion.isNullOrBlank() && ignoredVersion.equals(latestVersionName, ignoreCase = true)

    val hasUpdate: Boolean
        get() {
            if (latestVersionName.isNullOrBlank()) return false
            if (isVersionIgnored) return false
            if (!isInstalled) return true // App is tracked but not yet installed on device
            return SemVerComparator.isNewer(currentVersionName, latestVersionName)
        }
}
