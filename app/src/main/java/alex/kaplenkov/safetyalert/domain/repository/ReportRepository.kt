package alex.kaplenkov.safetyalert.domain.repository

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    suspend fun startNewReport(): DetectionReport
    suspend fun endReport(reportId: String): DetectionReport?
    suspend fun getActiveReport(): DetectionReport?
    suspend fun getAllReports(): Flow<List<DetectionReport>>
    suspend fun getReportById(reportId: String): DetectionReport?
    suspend fun saveReport(report: DetectionReport): Boolean
    suspend fun deleteReport(reportId: String): Boolean
}