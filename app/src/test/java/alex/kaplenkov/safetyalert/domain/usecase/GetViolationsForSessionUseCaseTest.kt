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

class GetViolationsForSessionUseCaseTest {

    private lateinit var repository: ViolationRepository
    private lateinit var useCase: GetViolationsForSessionUseCase

    @Before
    fun setup() {
        repository = Mockito.mock(ViolationRepository::class.java)
        useCase = GetViolationsForSessionUseCase(repository)
    }

    @Test
    fun `invoke filters violations by session id`() = runTest {
        val violations = listOf(
            Violation(1, "type1", 1f, null, "2024-01-01T00:00:00", "desc", "loc", "s1"),
            Violation(2, "type2", 1f, null, "2024-01-01T00:00:00", "desc", "loc", "s2"),
            Violation(3, "type1", 1f, null, "2024-01-01T00:00:00", "desc", "loc", "s1"),
        )
        Mockito.`when`(repository.getAllViolations()).thenReturn(flowOf(violations))

        val result = useCase("s1").toList().first()
        assertEquals(listOf(violations[0], violations[2]), result)
    }
}