package app.pwhs.universalinstaller.data

import app.pwhs.universalinstaller.domain.backup.TrackedAppBackupDto
import app.pwhs.universalinstaller.domain.backup.TrackedAppsBackupDataSource

class NoOpTrackedAppsBackupDataSource : TrackedAppsBackupDataSource {
    override fun isAvailable(): Boolean = false
    override suspend fun getTrackedApps(): List<TrackedAppBackupDto> = emptyList()
    override suspend fun restoreTrackedApps(apps: List<TrackedAppBackupDto>): Int = 0
}
