package alex.kaplenkov.safetyalert.data

import alex.kaplenkov.safetyalert.data.datasource.local.ReportLocalDataSource
import alex.kaplenkov.safetyalert.data.model.DetectionEntryDto
import alex.kaplenkov.safetyalert.data.model.ReportDto
import alex.kaplenkov.safetyalert.data.repository.ReportRepositoryImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReportRepositoryTest {

    private lateinit var mockLocalDataSource: ReportLocalDataSource
    private lateinit var repository: ReportRepositoryImpl

    @Before
    fun setUp() {
        mockLocalDataSource = Mockito.mock(ReportLocalDataSource::class.java)
        repository = ReportRepositoryImpl(mockLocalDataSource)
    }

    @Test
    fun `startNewReport creates and saves new report`() = runTest {
        // Arrange
        whenever(mockLocalDataSource.saveReport(any())).thenReturn(true)

        // Act
        val result = repository.startNewReport()

        // Assert
        assertNotNull(result)
        assertNotNull(result.id)
        verify(mockLocalDataSource).saveReport(any())
    }

    @Test
    fun `endReport updates report with end time`() = runTest {
        // Arrange
        val reportId = "report123"
        val reportDto = ReportDto(id = reportId, startTime = 100L, endTime = null, entryIds = listOf("entry1"))
        val entries = listOf(
            DetectionEntryDto(
                id = "entry1",
                reportId = reportId,
                timestamp = 123456789,
                imagePath = "/path/1.jpg",
                personCount = 3,
                peopleWithHelmets = 2,
                peopleWithoutHelmets = 1,
                processingTimeMs = 100
            )
        )

        whenever(mockLocalDataSource.getReport(reportId)).thenReturn(reportDto)
        whenever(mockLocalDataSource.getDetectionEntries(reportId)).thenReturn(entries)
        whenever(mockLocalDataSource.saveReport(any())).thenReturn(true)

        // Act - first set this as active report
        repository.startNewReport()
        // Use reflection to set active report ID since it's private
        val field = ReportRepositoryImpl::class.java.getDeclaredField("activeReportId")
        field.isAccessible = true
        field.set(repository, reportId)

        val result = repository.endReport(reportId)

        // Assert
        assertNotNull(result)
        assertEquals(reportId, result?.id)
        assertNotNull(result?.endTime)

        // Active report should be null now
        assertNull(repository.getActiveReport())
    }

    @Test
    fun `endReport returns null when report not found`() = runTest {
        // Arrange
        val reportId = "nonexistent"
        whenever(mockLocalDataSource.getReport(reportId)).thenReturn(null)

        // Act
        val result = repository.endReport(reportId)

        // Assert
        assertNull(result)
        verify(mockLocalDataSource).getReport(reportId)
        verify(mockLocalDataSource, never()).saveReport(any())
    }

    @Test
    fun `getActiveReport returns report when active report exists`() = runTest {
        // Arrange
        val reportId = "report123"
        val reportDto = ReportDto(id = reportId, startTime = 100L, endTime = null, entryIds = emptyList())

        // Use reflection to set active report ID
        val field = ReportRepositoryImpl::class.java.getDeclaredField("activeReportId")
        field.isAccessible = true
        field.set(repository, reportId)

        whenever(mockLocalDataSource.getReport(reportId)).thenReturn(reportDto)
        whenever(mockLocalDataSource.getDetectionEntries(reportId)).thenReturn(emptyList())

        // Act
        val result = repository.getActiveReport()

        // Assert
        assertNotNull(result)
        assertEquals(reportId, result?.id)
    }

    @Test
    fun `getActiveReport returns null when no active report`() = runTest {
        // Act
        val result = repository.getActiveReport()

        // Assert
        assertNull(result)
        verify(mockLocalDataSource, never()).getReport(any())
    }

    @Test
    fun `getAllReports returns reports with entries`() = runTest {
        // Arrange
        val reportDto1 = ReportDto(id = "report1", startTime = 100L, endTime = 200L, entryIds = listOf("entry1"))
        val reportDto2 = ReportDto(id = "report2", startTime = 300L, endTime = 400L, entryIds = listOf("entry2"))
        val entries1 = listOf(
            DetectionEntryDto(
                id = "entry1", reportId = "report1", timestamp = 150L,
                imagePath = "/path/1.jpg", personCount = 2,
                peopleWithHelmets = 1, peopleWithoutHelmets = 1, processingTimeMs = 100
            )
        )
        val entries2 = listOf(
            DetectionEntryDto(
                id = "entry2", reportId = "report2", timestamp = 350L,
                imagePath = "/path/2.jpg", personCount = 3,
                peopleWithHelmets = 2, peopleWithoutHelmets = 1, processingTimeMs = 120
            )
        )

        whenever(mockLocalDataSource.getAllReports()).thenReturn(flowOf(listOf(reportDto1, reportDto2)))
        whenever(mockLocalDataSource.getDetectionEntries("report1")).thenReturn(entries1)
        whenever(mockLocalDataSource.getDetectionEntries("report2")).thenReturn(entries2)

        // Act
        val result = repository.getAllReports()

        // Assert
        result.collect { reports ->
            assertEquals(2, reports.size)
            assertEquals("report1", reports[0].id)
            assertEquals("report2", reports[1].id)
            assertEquals(1, reports[0].entries.size)
            assertEquals(1, reports[1].entries.size)
        }
    }

    @Test
    fun `deleteReport removes report and clears active report if matching`() = runTest {
        // Arrange
        val reportId = "report123"
        whenever(mockLocalDataSource.deleteReport(reportId)).thenReturn(true)

        // Set active report ID
        val field = ReportRepositoryImpl::class.java.getDeclaredField("activeReportId")
        field.isAccessible = true
        field.set(repository, reportId)

        // Act
        val result = repository.deleteReport(reportId)

        // Assert
        assertTrue(result)
        verify(mockLocalDataSource).deleteReport(reportId)
        assertNull(repository.getActiveReport())
    }
}