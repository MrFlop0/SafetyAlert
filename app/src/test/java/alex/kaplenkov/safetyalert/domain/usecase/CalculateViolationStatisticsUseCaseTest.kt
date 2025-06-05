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

class CalculateViolationStatisticsUseCaseTest {

    private lateinit var repository: ViolationRepository
    private lateinit var useCase: CalculateViolationStatisticsUseCase

    @Before
    fun setup() {
        repository = Mockito.mock(ViolationRepository::class.java)
        useCase = CalculateViolationStatisticsUseCase(repository)
    }

    @Test
    fun `invoke calculates statistics correctly`() = runTest {
        val violations = listOf(
            Violation(1, "Speed", 1f, null, "2024-06-01T12:00:00", "desc", "loc", "s1"),
            Violation(2, "Speed", 1f, null, "2024-06-03T12:00:00", "desc", "loc", "s2"),
            Violation(3, "Seatbelt", 1f, null, "2024-06-01T12:00:00", "desc", "loc", "s1"),
        )
        Mockito.`when`(repository.getAllViolations()).thenReturn(flowOf(violations))

        val result = useCase().toList().first()
        assertEquals(3, result.totalCount)
        assertEquals(mapOf("Speed" to 2, "Seatbelt" to 1), result.violationsByType)
        assertEquals("Speed", result.mostCommonViolationType)
    }
}