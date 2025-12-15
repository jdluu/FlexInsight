package com.example.flexinsight.di

import android.content.Context
import com.example.flexinsight.core.dispatchers.DefaultDispatcherProvider
import com.example.flexinsight.core.dispatchers.DispatcherProvider
import com.example.flexinsight.core.network.NetworkMonitor
import com.example.flexinsight.data.api.FlexApiClient
import com.example.flexinsight.data.cache.CacheManager
import com.example.flexinsight.data.local.FlexDatabase
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.repository.ExerciseRepository
import com.example.flexinsight.data.repository.ExerciseRepositoryImpl
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.data.repository.FlexRepositoryImpl
import com.example.flexinsight.data.repository.RoutineRepository
import com.example.flexinsight.data.repository.RoutineRepositoryImpl
import com.example.flexinsight.data.repository.StatsRepository
import com.example.flexinsight.data.repository.StatsRepositoryImpl
import com.example.flexinsight.data.repository.WorkoutRepository
import com.example.flexinsight.data.repository.WorkoutRepositoryImpl
import com.example.flexinsight.data.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }

    @Provides
    @Singleton
    fun provideApiKeyManager(@ApplicationContext context: Context): ApiKeyManager {
        return ApiKeyManager(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesManager(@ApplicationContext context: Context): UserPreferencesManager {
        return UserPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
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
        cacheManager: CacheManager
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager
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
        dispatcherProvider: DispatcherProvider
    ): StatsRepository {
        return StatsRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            exerciseRepository = exerciseRepository,
            cacheManager = cacheManager,
            dispatcherProvider = dispatcherProvider
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
        repository: FlexRepository,
        networkMonitor: NetworkMonitor
    ): SyncManager {
        return SyncManager(
            repository = repository,
            networkMonitor = networkMonitor
        )
    }

    @Provides
    @Singleton
    fun provideSyncScheduler(@ApplicationContext context: Context): com.example.flexinsight.data.sync.SyncScheduler {
        return com.example.flexinsight.data.sync.SyncScheduler(context)
    }
}
