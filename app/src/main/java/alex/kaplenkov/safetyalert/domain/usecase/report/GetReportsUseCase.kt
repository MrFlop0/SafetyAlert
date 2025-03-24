package alex.kaplenkov.safetyalert.domain.usecase.report

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReportsUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(): Flow<List<DetectionReport>> {
        return reportRepository.getAllReports()
    }
}