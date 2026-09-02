package app.pwhs.universalinstaller.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "install_history",
    indices = [Index(value = ["sessionId"], unique = true)],
)
data class InstallHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val fileName: String,
    val versionName: String = "",
    val oldVersionName: String? = null,
    val fileSizeBytes: Long = 0,
    val iconPath: String? = null,
    val success: Boolean,
    val errorMessage: String? = null,
    val installedAt: Long = System.currentTimeMillis(),
    /** One Ackpine/privileged install session may produce at most one history row. */
    val sessionId: String? = null,
    val installerMode: String? = null,
    val operationType: String? = null,
    val filePath: String? = null,
)
