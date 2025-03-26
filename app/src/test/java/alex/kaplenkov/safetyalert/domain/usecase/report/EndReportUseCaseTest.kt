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

class EndReportUseCaseTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var useCase: EndReportUseCase
    private lateinit var mockReport: DetectionReport

    @Before
    fun setUp() {
        reportRepository = mock()
        useCase = EndReportUseCase(reportRepository)
        mockReport = mock()
    }

    @Test
    fun `invoke should return report when repository ends report successfully`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(reportRepository.endReport(reportId)).thenReturn(mockReport)

        // Act
        val result = useCase(reportId)

        // Assert
        assertEquals(mockReport, result)
        verify(reportRepository).endReport(reportId)
    }

    @Test
    fun `invoke should return null when repository fails to end report`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(reportRepository.endReport(reportId)).thenReturn(null)

        // Act
        val result = useCase(reportId)

        // Assert
        assertEquals(null, result)
        verify(reportRepository).endReport(reportId)
    }
}