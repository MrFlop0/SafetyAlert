package alex.kaplenkov.safetyalert.domain.repository

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import android.graphics.Bitmap

interface DetectionRepository {
    suspend fun saveDetection(
        reportId: String,
        detectionResult: DetectionResult,
        image: Bitmap
    ): DetectionEntry?

    suspend fun getDetectionEntries(reportId: String): List<DetectionEntry>
    suspend fun getDetectionImage(imagePath: String): Bitmap?
    suspend fun deleteDetection(detectionId: String): Boolean
}