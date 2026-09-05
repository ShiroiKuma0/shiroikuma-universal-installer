package app.pwhs.universalinstaller.domain.backup

interface TrackedAppsBackupDataSource {
    fun isAvailable(): Boolean
    suspend fun getTrackedApps(): List<TrackedAppBackupDto>
    suspend fun restoreTrackedApps(apps: List<TrackedAppBackupDto>): Int
}
