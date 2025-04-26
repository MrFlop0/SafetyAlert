package alex.kaplenkov.safetyalert.data.repository

import alex.kaplenkov.safetyalert.data.db.ViolationDao
import alex.kaplenkov.safetyalert.data.db.entity.SyncStatus
import alex.kaplenkov.safetyalert.data.db.entity.ViolationEntity
import alex.kaplenkov.safetyalert.domain.model.Violation
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalViolationRepository(
    private val violationDao: ViolationDao,
    private val syncRepository: SyncRepository,
    private val context: Context
) {

    fun getAllViolations(): Flow<List<Violation>> {
        return violationDao.getAllViolations()
            .map { entities ->
                entities.map { it.toViolation() }
            }
            .flowOn(Dispatchers.IO)
    }


    suspend fun saveViolation(violation: Violation, bitmap: Bitmap): Long {
        return withContext(Dispatchers.IO) {
            val imagePath = saveImageToStorage(bitmap)


            val entity = ViolationEntity(
                type = violation.type,
                confidence = violation.confidence,
                imagePath = imagePath,
                description = violation.description,
                location = violation.location,
                sessionId = violation.sessionId
            )

            val violationId = violationDao.insertViolation(entity)
            syncRepository.registerViolationForSync(violationId)
            violationId
        }
    }


    suspend fun deleteViolation(violationId: Long) {
        withContext(Dispatchers.IO) {
            val violation = violationDao.getViolationById(violationId)
            violation?.let {

                File(it.imagePath).delete()

                syncRepository.deleteSyncStatusForViolation(violationId)
                violationDao.deleteViolation(violationId)
            }
        }
    }

    fun getViolationById(id: Long): Flow<Violation?> {
        return flow {
            val entity = violationDao.getViolationById(id)
            emit(entity?.toViolation())
        }.flowOn(Dispatchers.IO)
    }

    fun getSyncStatusForViolation(violationId: Long): Flow<SyncStatus?> {
        return syncRepository.getSyncStatusForViolation(violationId)
    }

    suspend fun forceSyncViolation(violationId: Long) {
        syncRepository.syncViolation(violationId)
    }


    private suspend fun saveImageToStorage(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}_${UUID.randomUUID()}.jpg"
            val directory = File(context.filesDir, "violations")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            file.absolutePath
        }
    }


    private fun ViolationEntity.toViolation(): Violation {
        return Violation(
            id = id,
            type = type,
            confidence = confidence,
            imageUri = Uri.fromFile(File(imagePath)),
            timestamp = timestamp,
            description = description,
            location = location,
            sessionId = sessionId
        )
    }
}