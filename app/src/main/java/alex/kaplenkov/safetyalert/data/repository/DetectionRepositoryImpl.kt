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
        // Generate filename for image
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timeString = dateFormat.format(Date(timestamp))
        val filename = "detection_${timeString}_${timestamp}.jpg"

        // Save image
        val imagePath = localDataSource.saveImage(image, filename)
        if (imagePath.isEmpty()) return null

        // Create entry
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

        // Save entry
        val saved = localDataSource.saveDetectionEntry(entry)
        if (!saved) return null

        // Update report with the new entry
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
        // TODO(impl)
        // 1. Find which report this detection belongs to
        // 2. Remove the detection from the report's entry list
        // 3. Delete the detection file
        // 4. Delete the image file
        return false
    }
}