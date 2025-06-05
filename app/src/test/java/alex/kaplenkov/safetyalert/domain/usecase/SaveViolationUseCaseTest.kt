package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import android.graphics.Bitmap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SaveViolationUseCaseTest {

    private lateinit var repository: ViolationRepository
    private lateinit var useCase: SaveViolationUseCase

    @Before
    fun setup() {
        repository = Mockito.mock(ViolationRepository::class.java)
        useCase = SaveViolationUseCase(repository)
    }

    @Test
    fun `invoke calls repository and returns violation id`() = runTest {
        val violation = Violation(0, "type", 0.9f, null, "2024-01-01T00:00:00", "desc", "loc", "session1")
        val bitmap = Mockito.mock(Bitmap::class.java)
        Mockito.`when`(repository.saveViolation(violation, bitmap)).thenReturn(123L)

        val result = useCase(violation, bitmap)
        assertEquals(123L, result)
    }
}