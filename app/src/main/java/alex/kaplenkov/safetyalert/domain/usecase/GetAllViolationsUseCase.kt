package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllViolationsUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    operator fun invoke(): Flow<List<Violation>> {
        return violationRepository.getAllViolations()
    }
}