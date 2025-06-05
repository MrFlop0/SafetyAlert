package alex.kaplenkov.safetyalert.domain.usecase

import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DeleteViolationUseCaseTest {

    private lateinit var repository: ViolationRepository
    private lateinit var useCase: DeleteViolationUseCase

    @Before
    fun setup() {
        repository = Mockito.mock(ViolationRepository::class.java)
        useCase = DeleteViolationUseCase(repository)
    }

    @Test
    fun `invoke calls repository deleteViolation`() = runTest {
        useCase(42L)
        Mockito.verify(repository).deleteViolation(42L)
    }
}