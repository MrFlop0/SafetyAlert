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

class GetAllViolationUseCaseTest {

    private lateinit var repository: ViolationRepository
    private lateinit var useCase: GetAllViolationsUseCase

    @Before
    fun setup() {
        repository = Mockito.mock(ViolationRepository::class.java)
        useCase = GetAllViolationsUseCase(repository)
    }

    @Test
    fun `invoke returns all violations from repository`() = runTest {
        val fakeViolations = listOf(
            Violation(1, "type1", 0.95f, null, "2024-01-01T00:00:00", "desc", "loc", "session1"),
            Violation(2, "type2", 0.90f, null, "2024-01-02T00:00:00", "desc", "loc", "session1")
        )
        Mockito.`when`(repository.getAllViolations()).thenReturn(flowOf(fakeViolations))

        val result = useCase().toList()
        assertEquals(fakeViolations, result[0])
    }
}