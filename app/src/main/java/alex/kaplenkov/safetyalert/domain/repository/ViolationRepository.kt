package alex.kaplenkov.safetyalert.domain.repository

import alex.kaplenkov.safetyalert.domain.model.Violation
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface ViolationRepository {
    fun getAllViolations(): Flow<List<Violation>>
    suspend fun saveViolation(violation: Violation, bitmap: Bitmap): Long
    suspend fun deleteViolation(violationId: Long)
    fun getViolationById(id: Long): Flow<Violation?>
    suspend fun forceSyncViolation(violationId: Long)
}