package alex.kaplenkov.safetyalert.data


import alex.kaplenkov.safetyalert.data.datasource.local.ReportLocalDataSource
import alex.kaplenkov.safetyalert.data.model.DetectionEntryDto
import alex.kaplenkov.safetyalert.data.model.DetectionResult
import alex.kaplenkov.safetyalert.data.model.PersonDetection
import alex.kaplenkov.safetyalert.data.repository.DetectionRepositoryImpl
import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DetectionRepositoryTest {

    private lateinit var repository: DetectionRepositoryImpl
    private lateinit var mockLocalDataSource: ReportLocalDataSource
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockDetectionResult: DetectionResult

    @Before
    fun setUp() {
        mockLocalDataSource = mock(ReportLocalDataSource::class.java)
        repository = DetectionRepositoryImpl(mockLocalDataSource)
        mockBitmap = mock(Bitmap::class.java)

        // Configure mock detection result
        val personDetections = listOf(
            mock(PersonDetection::class.java),
            mock(PersonDetection::class.java),
            mock(PersonDetection::class.java)
        )
        whenever(personDetections[0].hasHelmet).thenReturn(true)
        whenever(personDetections[1].hasHelmet).thenReturn(false)
        whenever(personDetections[2].hasHelmet).thenReturn(true)

        mockDetectionResult = DetectionResult(
            personDetections = personDetections,
            helmetDetections = emptyList(),
            processingTimeMs = 100,
            imageWidth = 1024,
            imageHeight = 768
        )
    }

    @Test
    fun `saveDetection returns null when image save fails`() = runTest {
        // Arrange
        val reportId = "report123"
        whenever(mockLocalDataSource.saveImage(any(), any())).thenReturn("")

        // Act
        val result = repository.saveDetection(reportId, mockDetectionResult, mockBitmap)

        // Assert
        assertNull(result)
        verify(mockLocalDataSource).saveImage(eq(mockBitmap), any())
        verify(mockLocalDataSource, never()).saveDetectionEntry(any())
    }

    @Test
    fun `saveDetection returns null when entry save fails`() = runTest {
        // Arrange
        val reportId = "report123"
        whenever(mockLocalDataSource.saveImage(any(), any())).thenReturn("/path/to/image.jpg")
        whenever(mockLocalDataSource.saveDetectionEntry(any())).thenReturn(false)

        // Act
        val result = repository.saveDetection(reportId, mockDetectionResult, mockBitmap)

        // Assert
        assertNull(result)
        verify(mockLocalDataSource).saveDetectionEntry(any())
        verify(mockLocalDataSource, never()).getReport(any())
    }

    @Test
    fun `saveDetection returns null when report retrieval fails`() = runTest {
        // Arrange
        val reportId = "report123"
        whenever(mockLocalDataSource.saveImage(any(), any())).thenReturn("/path/to/image.jpg")
        whenever(mockLocalDataSource.saveDetectionEntry(any())).thenReturn(true)
        whenever(mockLocalDataSource.getReport(reportId)).thenReturn(null)

        // Act
        val result = repository.saveDetection(reportId, mockDetectionResult, mockBitmap)

        // Assert
        assertNull(result)
        verify(mockLocalDataSource).getReport(reportId)
        verify(mockLocalDataSource, never()).saveReport(any())
    }

    @Test
    fun `getDetectionEntries retrieves entries from data source`() = runTest {
        // Arrange
        val reportId = "report123"
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
            ),
            DetectionEntryDto(
                id = "entry2",
                reportId = reportId,
                timestamp = 123456790,
                imagePath = "/path/2.jpg",
                personCount = 2,
                peopleWithHelmets = 1,
                peopleWithoutHelmets = 1,
                processingTimeMs = 120
            )
        )
        whenever(mockLocalDataSource.getDetectionEntries(reportId)).thenReturn(entries)

        // Act
        val result = repository.getDetectionEntries(reportId)

        // Assert
        assertEquals(2, result.size)
        assertEquals("entry1", result[0].id)
        assertEquals("entry2", result[1].id)
    }

    @Test
    fun `getDetectionEntries returns empty list when no entries found`() = runTest {
        // Arrange
        val reportId = "report123"
        whenever(mockLocalDataSource.getDetectionEntries(reportId)).thenReturn(emptyList())

        // Act
        val result = repository.getDetectionEntries(reportId)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getDetectionImage retrieves image from data source`() = runTest {
        // Arrange
        val imagePath = "/path/to/image.jpg"
        whenever(mockLocalDataSource.getImage(imagePath)).thenReturn(mockBitmap)

        // Act
        val result = repository.getDetectionImage(imagePath)

        // Assert
        assertSame(mockBitmap, result)
    }
}