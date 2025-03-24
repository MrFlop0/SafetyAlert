package alex.kaplenkov.safetyalert.domain.usecase.detection

import alex.kaplenkov.safetyalert.domain.repository.DetectionRepository
import android.graphics.Bitmap
import javax.inject.Inject

class GetDetectionImageUseCase @Inject constructor(
    private val detectionRepository: DetectionRepository
) {
    suspend operator fun invoke(imagePath: String): Bitmap? {
        return detectionRepository.getDetectionImage(imagePath)
    }
}