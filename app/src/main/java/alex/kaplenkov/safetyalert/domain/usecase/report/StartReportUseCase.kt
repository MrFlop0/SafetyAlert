package alex.kaplenkov.safetyalert.domain.usecase.report

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import javax.inject.Inject

class StartReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(): DetectionReport {
        return reportRepository.startNewReport()
    }
}