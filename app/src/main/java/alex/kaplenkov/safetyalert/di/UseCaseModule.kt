package alex.kaplenkov.safetyalert.di

import alex.kaplenkov.safetyalert.domain.repository.ViolationRepository
import alex.kaplenkov.safetyalert.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideGetAllViolationsUseCase(
        violationRepository: ViolationRepository
    ): GetAllViolationsUseCase {
        return GetAllViolationsUseCase(violationRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSaveViolationUseCase(
        violationRepository: ViolationRepository
    ): SaveViolationUseCase {
        return SaveViolationUseCase(violationRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideDeleteViolationUseCase(
        violationRepository: ViolationRepository
    ): DeleteViolationUseCase {
        return DeleteViolationUseCase(violationRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetViolationByIdUseCase(
        violationRepository: ViolationRepository
    ): GetViolationByIdUseCase {
        return GetViolationByIdUseCase(violationRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetViolationsForSessionUseCase(
        violationRepository: ViolationRepository
    ): GetViolationsForSessionUseCase {
        return GetViolationsForSessionUseCase(violationRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideCalculateViolationStatisticsUseCase(
        violationRepository: ViolationRepository
    ): CalculateViolationStatisticsUseCase {
        return CalculateViolationStatisticsUseCase(violationRepository)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    @Provides
    @Singleton
    fun provideSessionManagementUseCase(): SessionManagementUseCase {
        return SessionManagementUseCase()
    }
}