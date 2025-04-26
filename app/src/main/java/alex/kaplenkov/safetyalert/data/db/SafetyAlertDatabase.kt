package alex.kaplenkov.safetyalert.data.db

import alex.kaplenkov.safetyalert.data.db.entity.SyncStatusEntity
import alex.kaplenkov.safetyalert.data.db.entity.ViolationEntity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ViolationEntity::class, SyncStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SafetyAlertDatabase : RoomDatabase() {
    abstract fun violationDao(): ViolationDao
    abstract fun syncStatusDao(): SyncStatusDao

    companion object {
        @Volatile
        private var INSTANCE: SafetyAlertDatabase? = null

        fun getDatabase(context: Context): SafetyAlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafetyAlertDatabase::class.java,
                    "safety_alert_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}