package alex.kaplenkov.safetyalert.domain.usecase.report

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetReportByIdUseCaseTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var useCase: GetReportByIdUseCase
    private lateinit var mockReport: DetectionReport

    @Before
    fun setUp() {
        reportRepository = mock()
        useCase = GetReportByIdUseCase(reportRepository)
        mockReport = mock()
    }

    @Test
    fun `invoke should return report when repository finds report`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(reportRepository.getReportById(reportId)).thenReturn(mockReport)

        // Act
        val result = useCase(reportId)

        // Assert
        assertEquals(mockReport, result)
        verify(reportRepository).getReportById(reportId)
    }

    @Test
    fun `invoke should return null when repository doesn't find report`(): Unit = runBlocking {
        // Arrange
        val reportId = "nonexistent"
        whenever(reportRepository.getReportById(reportId)).thenReturn(null)

        // Act
        val result = useCase(reportId)

        // Assert
        assertEquals(null, result)
        verify(reportRepository).getReportById(reportId)
    }
}