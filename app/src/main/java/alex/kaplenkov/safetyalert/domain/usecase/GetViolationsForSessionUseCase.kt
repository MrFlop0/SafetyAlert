package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetViolationsForSessionUseCase @Inject constructor(
    private val violationRepository: ViolationRepository
) {
    operator fun invoke(sessionId: String): Flow<List<Violation>> {
        return violationRepository.getAllViolations()
            .map { violations -> violations.filter { it.sessionId == sessionId } }
    }
}