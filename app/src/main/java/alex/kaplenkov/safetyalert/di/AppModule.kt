package alex.kaplenkov.safetyalert.di

import alex.kaplenkov.safetyalert.data.datasource.local.ReportLocalDataSource
import alex.kaplenkov.safetyalert.data.repository.DetectionRepositoryImpl
import alex.kaplenkov.safetyalert.data.repository.ReportRepositoryImpl
import alex.kaplenkov.safetyalert.domain.repository.DetectionRepository
import alex.kaplenkov.safetyalert.domain.repository.ReportRepository
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideReportLocalDataSource(@ApplicationContext context: Context): ReportLocalDataSource {
        return ReportLocalDataSource(context)
    }

    @Provides
    @Singleton
    fun provideReportRepository(
        localDataSource: ReportLocalDataSource
    ): ReportRepository {
        return ReportRepositoryImpl(localDataSource)
    }

    @Provides
    @Singleton
    fun provideDetectionRepository(
        localDataSource: ReportLocalDataSource
    ): DetectionRepository {
        return DetectionRepositoryImpl(localDataSource)
    }
}