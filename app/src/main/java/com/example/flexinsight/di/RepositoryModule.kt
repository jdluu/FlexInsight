package com.example.flexinsight.di

import android.content.Context
import com.example.flexinsight.core.dispatchers.DefaultDispatcherProvider
import com.example.flexinsight.core.dispatchers.DispatcherProvider
import com.example.flexinsight.core.network.NetworkMonitor
import com.example.flexinsight.data.api.FlexApiClient
import com.example.flexinsight.data.cache.CacheManager
import com.example.flexinsight.data.cache.CacheStrategy
import com.example.flexinsight.data.local.FlexDatabase
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.data.repository.*
import com.example.flexinsight.data.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }

    @Provides
    @Singleton
    fun provideCacheManager(): CacheManager {
        return CacheManager()
    }

    @Provides
    @Singleton
    fun provideExerciseRepository(
        exerciseDao: ExerciseDao,
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        apiClient: FlexApiClient,
        cacheManager: CacheManager
    ): ExerciseRepository {
        return ExerciseRepositoryImpl(
            exerciseDao = exerciseDao,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager
        )
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDao: WorkoutDao,
        exerciseDao: ExerciseDao,
        setDao: SetDao,
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        apiClient: FlexApiClient,
        cacheManager: CacheManager,
        syncManager: SyncManager
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager,
            syncManager = syncManager
        )
    }

    @Provides
    @Singleton
    fun provideRoutineRepository(
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        apiClient: FlexApiClient,
        cacheManager: CacheManager,
        exerciseRepository: ExerciseRepository
    ): RoutineRepository {
        return RoutineRepositoryImpl(
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager,
            exerciseRepository = exerciseRepository
        )
    }

    @Provides
    @Singleton
    fun provideStatsRepository(
        workoutDao: WorkoutDao,
        exerciseDao: ExerciseDao,
        setDao: SetDao,
        exerciseRepository: ExerciseRepository,
        cacheManager: CacheManager,
        dispatcherProvider: DispatcherProvider,
        cacheStrategy: CacheStrategy,
        getWorkoutStatsUseCase: com.example.flexinsight.domain.usecase.GetWorkoutStatsUseCase,
        getPRDetailsUseCase: com.example.flexinsight.domain.usecase.GetPRDetailsUseCase,
        getMuscleGroupProgressUseCase: com.example.flexinsight.domain.usecase.GetMuscleGroupProgressUseCase,
        getWeeklyProgressUseCase: com.example.flexinsight.domain.usecase.GetWeeklyProgressUseCase,
        getMuscleRecoveryUseCase: com.example.flexinsight.domain.usecase.GetMuscleRecoveryUseCase
    ): StatsRepository {
        return StatsRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            exerciseRepository = exerciseRepository,
            cacheManager = cacheManager,
            dispatcherProvider = dispatcherProvider,
            cacheStrategy = cacheStrategy,
            getWorkoutStatsUseCase = getWorkoutStatsUseCase,
            getPRDetailsUseCase = getPRDetailsUseCase,
            getMuscleGroupProgressUseCase = getMuscleGroupProgressUseCase,
            getWeeklyProgressUseCase = getWeeklyProgressUseCase,
            getMuscleRecoveryUseCase = getMuscleRecoveryUseCase
        )
    }

    @Provides
    @Singleton
    fun provideFlexRepository(
        database: FlexDatabase,
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        cacheManager: CacheManager,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
        routineRepository: RoutineRepository,
        statsRepository: StatsRepository
    ): FlexRepository {
        return FlexRepositoryImpl(
            database = database,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            cacheManager = cacheManager,
            exerciseRepository = exerciseRepository,
            workoutRepository = workoutRepository,
            routineRepository = routineRepository,
            statsRepository = statsRepository
        )
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context
    ): SyncManager {
        return SyncManager(context)
    }
}
