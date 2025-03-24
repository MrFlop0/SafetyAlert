package alex.kaplenkov.safetyalert.data.repository

import alex.kaplenkov.safetyalert.data.datasource.local.ReportLocalDataSource
import alex.kaplenkov.safetyalert.data.mapper.toDomain
import alex.kaplenkov.safetyalert.data.mapper.toDto
import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val localDataSource: ReportLocalDataSource
) : ReportRepository {

    private var activeReportId: String? = null

    override suspend fun startNewReport(): DetectionReport {
        val report = DetectionReport()
        activeReportId = report.id
        saveReport(report)
        return report
    }

    override suspend fun endReport(reportId: String): DetectionReport? {
        if (activeReportId == reportId) {
            activeReportId = null
        }

        val reportDto = localDataSource.getReport(reportId) ?: return null
        val entries = localDataSource.getDetectionEntries(reportId).map { it.toDomain() }

        val updatedReport = reportDto.copy(endTime = Date().time).toDomain(entries)
        saveReport(updatedReport)

        return updatedReport
    }

    override suspend fun getActiveReport(): DetectionReport? {
        val activeId = activeReportId ?: return null
        return getReportById(activeId)
    }

    override suspend fun getAllReports(): Flow<List<DetectionReport>> {
        return localDataSource.getAllReports().map { reports ->
            reports.map { reportDto ->
                val entries = localDataSource.getDetectionEntries(reportDto.id).map { it.toDomain() }
                reportDto.toDomain(entries)
            }
        }
    }

    override suspend fun getReportById(reportId: String): DetectionReport? {
        val reportDto = localDataSource.getReport(reportId) ?: return null
        val entries = localDataSource.getDetectionEntries(reportId).map { it.toDomain() }
        return reportDto.toDomain(entries)
    }

    override suspend fun saveReport(report: DetectionReport): Boolean {
        return localDataSource.saveReport(report.toDto())
    }

    override suspend fun deleteReport(reportId: String): Boolean {
        if (activeReportId == reportId) {
            activeReportId = null
        }
        return localDataSource.deleteReport(reportId)
    }
}