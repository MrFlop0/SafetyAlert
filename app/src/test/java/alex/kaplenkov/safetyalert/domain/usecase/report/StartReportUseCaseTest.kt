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

class StartReportUseCaseTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var useCase: StartReportUseCase
    private lateinit var mockReport: DetectionReport

    @Before
    fun setUp() {
        reportRepository = mock()
        useCase = StartReportUseCase(reportRepository)
        mockReport = mock()
    }

    @Test
    fun `invoke should return new report from repository`(): Unit = runBlocking {
        // Arrange
        whenever(reportRepository.startNewReport()).thenReturn(mockReport)

        // Act
        val result = useCase()

        // Assert
        assertEquals(mockReport, result)
        verify(reportRepository).startNewReport()
    }
}