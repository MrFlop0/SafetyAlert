package alex.kaplenkov.safetyalert.domain.usecase.detection

import alex.kaplenkov.safetyalert.data.model.DetectionResult
import android.graphics.RectF
import alex.kaplenkov.safetyalert.data.model.PersonDetection
import alex.kaplenkov.safetyalert.domain.model.DetectionEntry
import alex.kaplenkov.safetyalert.domain.repository.DetectionRepository
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SaveDetectionUseCaseTest {

    private lateinit var detectionRepository: DetectionRepository
    private lateinit var useCase: SaveDetectionUseCase
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockDetectionResult: DetectionResult
    private lateinit var mockDetectionEntry: DetectionEntry

    @Before
    fun setUp() {
        detectionRepository = mock()
        useCase = SaveDetectionUseCase(detectionRepository)
        mockBitmap = mock()
        mockDetectionResult = DetectionResult(
            personDetections = listOf(
                PersonDetection(
                    confidence = 0.95f, hasHelmet = true,
                    boundingBox = RectF(),
                    keypoints = emptyList(),
                )
            ),
            processingTimeMs = 100,
            helmetDetections = emptyList(),
            imageWidth = 1090,
            imageHeight = 1820,
        )
        mockDetectionEntry = mock()
    }

    @Test
    fun `invoke should return detection entry from repository when save is successful`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(detectionRepository.saveDetection(reportId, mockDetectionResult, mockBitmap))
            .thenReturn(mockDetectionEntry)

        // Act
        val result = useCase(reportId, mockDetectionResult, mockBitmap)

        // Assert
        assertEquals(mockDetectionEntry, result)
        verify(detectionRepository).saveDetection(reportId, mockDetectionResult, mockBitmap)
    }

    @Test
    fun `invoke should return null when save fails`(): Unit = runBlocking {
        // Arrange
        val reportId = "report123"
        whenever(detectionRepository.saveDetection(reportId, mockDetectionResult, mockBitmap))
            .thenReturn(null)

        // Act
        val result = useCase(reportId, mockDetectionResult, mockBitmap)

        // Assert
        assertEquals(null, result)
        verify(detectionRepository).saveDetection(reportId, mockDetectionResult, mockBitmap)
    }
}