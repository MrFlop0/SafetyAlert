package alex.kaplenkov.safetyalert.domain.usecase.detection

import alex.kaplenkov.safetyalert.domain.repository.DetectionRepository
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetDetectionImageUseCaseTest {

    private lateinit var detectionRepository: DetectionRepository
    private lateinit var useCase: GetDetectionImageUseCase
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setUp() {
        detectionRepository = mock()
        useCase = GetDetectionImageUseCase(detectionRepository)
        mockBitmap = mock()
    }

    @Test
    fun `invoke should return bitmap from repository when image exists`(): Unit = runBlocking {
        // Arrange
        val imagePath = "path/to/image.jpg"
        whenever(detectionRepository.getDetectionImage(imagePath)).thenReturn(mockBitmap)

        // Act
        val result = useCase(imagePath)

        // Assert
        assertEquals(mockBitmap, result)
        verify(detectionRepository).getDetectionImage(imagePath)
    }

    @Test
    fun `invoke should return null when image doesn't exist`(): Unit = runBlocking {
        // Arrange
        val imagePath = "invalid/path.jpg"
        whenever(detectionRepository.getDetectionImage(imagePath)).thenReturn(null)

        // Act
        val result = useCase(imagePath)

        // Assert
        assertEquals(null, result)
        verify(detectionRepository).getDetectionImage(imagePath)
    }
}