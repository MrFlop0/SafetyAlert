package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class GetViolationByIdUseCaseTest {

    private lateinit var repository: ViolationRepository
    private lateinit var useCase: GetViolationByIdUseCase

    @Before
    fun setup() {
        repository = Mockito.mock(ViolationRepository::class.java)
        useCase = GetViolationByIdUseCase(repository)
    }

    @Test
    fun `invoke returns correct violation`() = runTest {
        val violation = Violation(7, "type", 0.8f, null, "2024-01-01T00:00:00", "desc", "loc", "session1")
        Mockito.`when`(repository.getViolationById(7)).thenReturn(flowOf(violation))

        val result = useCase(7).toList().first()
        assertEquals(violation, result)
    }
}