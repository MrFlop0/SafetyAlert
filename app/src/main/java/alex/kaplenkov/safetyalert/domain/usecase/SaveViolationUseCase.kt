package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import android.graphics.Bitmap
import javax.inject.Inject

class SaveViolationUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    suspend operator fun invoke(violation: Violation, bitmap: Bitmap): Long {
        return violationRepository.saveViolation(violation, bitmap)
    }
}