package com.jdluu.flexinsight.di

import android.content.Context
import com.jdluu.flexinsight.core.dispatchers.DefaultDispatcherProvider
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.data.api.FlexApiClient
import com.jdluu.flexinsight.data.cache.CacheManager
import com.jdluu.flexinsight.data.cache.CacheStrategy
import com.jdluu.flexinsight.data.local.FlexDatabase
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.local.dao.SetDao
import com.jdluu.flexinsight.data.local.dao.WorkoutDao
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.data.repository.*
import com.jdluu.flexinsight.data.sync.SyncManager
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
        syncManager: SyncManager,
        syncPreferencesManager: com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager,
            syncManager = syncManager,
            syncPreferencesManager = syncPreferencesManager
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
        cacheStrategy: CacheStrategy
    ): StatsRepository {
        return StatsRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            exerciseRepository = exerciseRepository,
            cacheManager = cacheManager,
            dispatcherProvider = dispatcherProvider,
            cacheStrategy = cacheStrategy
        )
    }

    @Provides
    @Singleton
    fun provideFlexRepository(
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        cacheManager: CacheManager,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
        routineRepository: RoutineRepository,
        statsRepository: StatsRepository
    ): FlexRepository {
        return FlexRepositoryImpl(
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
