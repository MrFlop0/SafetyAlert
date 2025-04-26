package alex.kaplenkov.safetyalert.data.db

import alex.kaplenkov.safetyalert.data.db.entity.SyncStatus
import alex.kaplenkov.safetyalert.data.db.entity.SyncStatusEntity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncStatus: SyncStatusEntity): Long

    @Query("SELECT * FROM sync_status WHERE syncStatus = :status")
    suspend fun getViolationsWithStatus(status: SyncStatus): List<SyncStatusEntity>

    @Query("SELECT * FROM sync_status WHERE violationId = :violationId")
    suspend fun getSyncStatusForViolation(violationId: Long): SyncStatusEntity?

    @Query("UPDATE sync_status SET syncStatus = :status, serverId = :serverId, lastSyncAttempt = :timestamp WHERE violationId = :violationId")
    suspend fun updateSyncStatus(violationId: Long, status: SyncStatus, serverId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sync_status SET syncStatus = :status, syncAttempts = syncAttempts + 1, errorMessage = :errorMessage, lastSyncAttempt = :timestamp WHERE violationId = :violationId")
    suspend fun updateSyncFailure(violationId: Long, status: SyncStatus, errorMessage: String?, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM sync_status")
    fun getAllSyncStatuses(): Flow<List<SyncStatusEntity>>

    @Query("SELECT * FROM sync_status WHERE violationId = :violationId")
    fun getSyncStatusForViolationFlow(violationId: Long): Flow<SyncStatusEntity?>

    @Query("DELETE FROM sync_status WHERE violationId = :violationId")
    suspend fun deleteSyncStatusForViolation(violationId: Long)
}