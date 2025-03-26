package alex.kaplenkov.safetyalert.domain.usecase.report

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetReportsUseCaseTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var useCase: GetReportsUseCase
    private lateinit var mockReportFlow: Flow<List<DetectionReport>>

    @Before
    fun setUp() {
        reportRepository = mock()
        useCase = GetReportsUseCase(reportRepository)
        mockReportFlow = flowOf(listOf<DetectionReport>(mock(), mock()))
    }

    @Test
    fun `invoke should return flow of reports from repository`(): Unit = runBlocking {
        // Arrange
        whenever(reportRepository.getAllReports()).thenReturn(mockReportFlow)

        // Act
        val result = useCase()

        // Assert
        assertEquals(mockReportFlow, result)
        verify(reportRepository).getAllReports()
    }
}