package alex.kaplenkov.safetyalert.data.db

import alex.kaplenkov.safetyalert.data.db.entity.ViolationEntity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolation(violation: ViolationEntity): Long

    @Query("SELECT * FROM violations ORDER BY timestamp DESC")
    fun getAllViolations(): Flow<List<ViolationEntity>>

    @Query("DELETE FROM violations WHERE id = :violationId")
    suspend fun deleteViolation(violationId: Long)

    @Query("SELECT * FROM violations WHERE id = :violationId")
    suspend fun getViolationById(violationId: Long): ViolationEntity?
}