package alex.kaplenkov.safetyalert.data.repository

import alex.kaplenkov.safetyalert.data.datasource.local.ReportLocalDataSource
import alex.kaplenkov.safetyalert.data.mapper.toDomain
import alex.kaplenkov.safetyalert.data.model.DetectionEntryDto
import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.repository.DetectionRepository
import android.graphics.Bitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectionRepositoryImpl @Inject constructor(
    private val localDataSource: ReportLocalDataSource
) : DetectionRepository {

    override suspend fun saveDetection(
        reportId: String,
        detectionResult: DetectionResult,
        image: Bitmap
    ): DetectionEntry? {
         
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timeString = dateFormat.format(Date(timestamp))
        val filename = "detection_${timeString}_${timestamp}.jpg"

         
        val imagePath = localDataSource.saveImage(image, filename)
        if (imagePath.isEmpty()) return null

         
        val entry = DetectionEntryDto(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            timestamp = timestamp,
            imagePath = imagePath,
            personCount = detectionResult.personDetections.size,
            peopleWithHelmets = detectionResult.personDetections.count { it.hasHelmet },
            peopleWithoutHelmets = detectionResult.personDetections.count { !it.hasHelmet },
            processingTimeMs = detectionResult.processingTimeMs
        )

         
        val saved = localDataSource.saveDetectionEntry(entry)
        if (!saved) return null

         
        val report = localDataSource.getReport(reportId) ?: return null
        val updatedReport = report.copy(entryIds = report.entryIds + entry.id)
        localDataSource.saveReport(updatedReport)

        return entry.toDomain()
    }

    override suspend fun getDetectionEntries(reportId: String): List<DetectionEntry> {
        return localDataSource.getDetectionEntries(reportId).map { it.toDomain() }
    }

    override suspend fun getDetectionImage(imagePath: String): Bitmap? {
        return localDataSource.getImage(imagePath)
    }

    override suspend fun deleteDetection(detectionId: String): Boolean {
        return false
    }
}