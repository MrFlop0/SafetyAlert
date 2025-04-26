package alex.kaplenkov.safetyalert.data.repository

import alex.kaplenkov.safetyalert.data.api.ApiService
import alex.kaplenkov.safetyalert.data.api.model.request.CreateOrderRequest
import alex.kaplenkov.safetyalert.data.api.model.request.UploadPhotoRequest
import alex.kaplenkov.safetyalert.data.db.SyncStatusDao
import alex.kaplenkov.safetyalert.data.db.ViolationDao
import alex.kaplenkov.safetyalert.data.db.entity.SyncStatus
import alex.kaplenkov.safetyalert.data.db.entity.SyncStatusEntity
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val violationDao: ViolationDao,
    private val syncStatusDao: SyncStatusDao,
    private val apiService: ApiService
) {
    suspend fun registerViolationForSync(violationId: Long) {
        val syncStatus = SyncStatusEntity(violationId = violationId)
        syncStatusDao.insert(syncStatus)
    }

    suspend fun deleteSyncStatusForViolation(violationId: Long) {
        syncStatusDao.deleteSyncStatusForViolation(violationId)
    }

    suspend fun syncPendingViolations() {
        val pendingViolations = syncStatusDao.getViolationsWithStatus(SyncStatus.PENDING)
        val failedViolations = syncStatusDao.getViolationsWithStatus(SyncStatus.FAILED)
            .filter { it.syncAttempts < MAX_RETRY_ATTEMPTS &&
                    (System.currentTimeMillis() - it.lastSyncAttempt) > RETRY_DELAY_MS }

        (pendingViolations + failedViolations).forEach { syncStatus ->
            Log.d("SyncAAA", "Attempting to sync violation ${syncStatus.violationId}")
            syncViolation(syncStatus.violationId)
        }
    }

    fun getSyncStatusForViolation(violationId: Long): Flow<SyncStatus?> {
        return syncStatusDao.getSyncStatusForViolationFlow(violationId)
            .map { it?.syncStatus }
    }

    // Синхронизирует конкретное нарушение
    suspend fun syncViolation(violationId: Long) {
        try {
            syncStatusDao.updateSyncStatus(violationId, SyncStatus.IN_PROGRESS, null)

            val violation = violationDao.getViolationById(violationId) ?: run {
                syncStatusDao.updateSyncFailure(violationId, SyncStatus.FAILED, "Violation not found")
                return
            }

            // Создаем заявку
            val orderRequest = CreateOrderRequest(
                name = "Нарушение: ${violation.type}",
                description = violation.description ?: "Нарушение ${violation.timestamp}",
                priority_id = "df22a3db-df94-4728-b6e7-c1c210fca945" // Высокий приоритет
            )

            val response = apiService.createOrder(orderRequest)

            if (response.isSuccessful && response.body() != null) {
                val orderId = response.body()!!.id

                if (violation.imagePath.isNotBlank()) {
                    val imageFile = File(violation.imagePath)
                    if (imageFile.exists()) {
                        val encodedImage = encodeImageToBase64(imageFile)

                        val photoRequest = UploadPhotoRequest(
                            name = "Фото нарушения ${violation.type}",
                            orderId = orderId,
                            file = encodedImage
                        )

                        val a = apiService.uploadPhoto(photoRequest)
                        Log.d("SyncAAA", "Photo upload response: ${a.code()}")
                    }
                }

                syncStatusDao.updateSyncStatus(
                    violationId = violationId,
                    status = SyncStatus.COMPLETED,
                    serverId = orderId
                )
            } else {
                syncStatusDao.updateSyncFailure(
                    violationId = violationId,
                    status = SyncStatus.FAILED,
                    errorMessage = "API error: ${response.code()}"
                )
            }

        } catch (e: Exception) {
            syncStatusDao.updateSyncFailure(
                violationId = violationId,
                status = SyncStatus.FAILED,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    private suspend fun encodeImageToBase64(file: File): String {
        return withContext(Dispatchers.IO) {
            val bytes = file.readBytes()
            "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.DEFAULT)}"
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val RETRY_DELAY_MS = 30 * 60 * 1000L // 30 минут
    }
}