package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetViolationByIdUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    operator fun invoke(id: Long): Flow<Violation?> {
        return violationRepository.getViolationById(id)
    }
}