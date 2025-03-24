package alex.kaplenkov.safetyalert.domain.usecase.report

import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import javax.inject.Inject

class DeleteReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(reportId: String): Boolean {
        return reportRepository.deleteReport(reportId)
    }
}