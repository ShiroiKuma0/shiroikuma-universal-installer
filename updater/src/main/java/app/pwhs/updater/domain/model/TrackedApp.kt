package app.pwhs.updater.domain.model

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
    val eTag: String? = null,
) {
    val hasUpdate: Boolean
        get() = latestVersionName != null &&
            latestVersionName.isNotBlank() &&
            latestVersionName != currentVersionName
}
