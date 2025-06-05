package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import javax.inject.Inject

class DeleteViolationUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    suspend operator fun invoke(violationId: Long) {
        violationRepository.deleteViolation(violationId)
    }
}