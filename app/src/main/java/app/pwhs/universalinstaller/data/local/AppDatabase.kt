package app.pwhs.universalinstaller.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [InstallHistoryEntity::class, UninstallLogEntity::class, DownloadHistoryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun installHistoryDao(): InstallHistoryDao
    abstract fun uninstallLogDao(): UninstallLogDao
    abstract fun downloadHistoryDao(): DownloadHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `uninstall_logs` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `success` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        `uninstalledAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `download_history` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `url` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `downloadedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Nullable so old history stays intact; SQLite allows multiple NULLs in a unique
                // index, while every new install provides its concrete session ID.
                db.execSQL("ALTER TABLE `install_history` ADD COLUMN `sessionId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_install_history_sessionId` " +
                        "ON `install_history` (`sessionId`)"
                )
            }
        }
    }
}
