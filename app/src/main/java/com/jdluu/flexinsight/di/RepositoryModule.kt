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
    fun provideWorkoutMutationRepository(
        workoutDao: WorkoutDao,
        exerciseDao: ExerciseDao,
        setDao: SetDao,
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        apiClient: FlexApiClient,
        cacheManager: CacheManager,
        syncManager: SyncManager,
        syncPreferencesManager: com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
    ): WorkoutMutationRepository {
        return WorkoutMutationRepositoryImpl(
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
    fun provideWorkoutQueryRepository(
        workoutDao: WorkoutDao,
        exerciseDao: ExerciseDao,
        setDao: SetDao,
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        apiClient: FlexApiClient,
        mutationRepository: WorkoutMutationRepository
    ): WorkoutQueryRepository {
        return WorkoutQueryRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            mutationRepository = mutationRepository
        )
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        queryRepository: WorkoutQueryRepository,
        mutationRepository: WorkoutMutationRepository
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(
            queryRepository = queryRepository,
            mutationRepository = mutationRepository
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
    fun provideStatsMutationRepository(cacheManager: CacheManager): StatsMutationRepository {
        return StatsMutationRepositoryImpl(cacheManager = cacheManager)
    }

    @Provides
    @Singleton
    fun provideStatsQueryRepository(
        workoutDao: WorkoutDao,
        exerciseDao: ExerciseDao,
        setDao: SetDao,
        exerciseRepository: ExerciseRepository,
        dispatcherProvider: DispatcherProvider,
        cacheStrategy: CacheStrategy
    ): StatsQueryRepository {
        return StatsQueryRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            exerciseRepository = exerciseRepository,
            dispatcherProvider = dispatcherProvider,
            cacheStrategy = cacheStrategy
        )
    }

    @Provides
    @Singleton
    fun provideStatsRepository(
        queryRepository: StatsQueryRepository,
        mutationRepository: StatsMutationRepository
    ): StatsRepository {
        return StatsRepositoryImpl(
            queryRepository = queryRepository,
            mutationRepository = mutationRepository
        )
    }

    @Provides
    @Singleton
    fun provideFlexRepositoryImpl(
        apiKeyManager: ApiKeyManager,
        networkMonitor: NetworkMonitor,
        cacheManager: CacheManager,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
        routineRepository: RoutineRepository,
        statsRepository: StatsRepository
    ): FlexRepositoryImpl {
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
    fun provideFlexRepository(impl: FlexRepositoryImpl): FlexRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideHevySyncSource(impl: FlexRepositoryImpl): com.jdluu.flexinsight.data.sync.HevySyncSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context
    ): SyncManager {
        return SyncManager(context)
    }
}
