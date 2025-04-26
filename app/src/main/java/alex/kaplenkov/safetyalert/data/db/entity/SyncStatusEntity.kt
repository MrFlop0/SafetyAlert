package alex.kaplenkov.safetyalert.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_status")
data class SyncStatusEntity(
    @PrimaryKey
    val violationId: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val serverId: String? = null,
    val lastSyncAttempt: Long = System.currentTimeMillis(),
    val syncAttempts: Int = 0,
    val errorMessage: String? = null
)

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}