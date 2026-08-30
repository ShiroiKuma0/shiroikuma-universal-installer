package app.pwhs.updater.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedAppEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class UpdaterDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao

    companion object {
        private const val DATABASE_NAME = "universal_updater.db"

        @Volatile
        private var INSTANCE: UpdaterDatabase? = null

        fun getInstance(context: Context): UpdaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UpdaterDatabase::class.java,
                    DATABASE_NAME,
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
