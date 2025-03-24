package alex.kaplenkov.safetyalert.domain.usecase.detection

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.repository.DetectionRepository
import android.graphics.Bitmap
import javax.inject.Inject

class SaveDetectionUseCase @Inject constructor(
    private val detectionRepository: DetectionRepository
) {
    suspend operator fun invoke(
        reportId: String,
        detectionResult: DetectionResult,
        image: Bitmap
    ): DetectionEntry? {
        return detectionRepository.saveDetection(reportId, detectionResult, image)
    }
}