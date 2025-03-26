package alex.kaplenkov.safetyalert.domain.usecase.report

import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeleteReportUseCaseTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var useCase: DeleteReportUseCase

    @Before
    fun setUp() {
        reportRepository = mock()
        useCase = DeleteReportUseCase(reportRepository)
    }

    @Test
    fun `invoke should return true when repository deletion succeeds`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(reportRepository.deleteReport(reportId)).thenReturn(true)

        // Act
        val result = useCase(reportId)

        // Assert
        assertEquals(true, result)
        verify(reportRepository).deleteReport(reportId)
    }

    @Test
    fun `invoke should return false when repository deletion fails`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(reportRepository.deleteReport(reportId)).thenReturn(false)

        // Act
        val result = useCase(reportId)

        // Assert
        assertEquals(false, result)
        verify(reportRepository).deleteReport(reportId)
    }
}