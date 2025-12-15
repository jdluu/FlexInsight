package com.example.flexinsight

import android.app.Application
import androidx.room.Room
import com.example.flexinsight.core.network.NetworkMonitor
import com.example.flexinsight.data.cache.CacheManager
import com.example.flexinsight.data.local.FlexDatabase
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.repository.FlexRepository
import com.example.flexinsight.data.sync.SyncManager
import com.example.flexinsight.data.sync.SyncScheduler

class FlexInsightApplication : Application() {
    val database: FlexDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            FlexDatabase::class.java,
            FlexDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Allow schema changes during development
            .build()
    }

    val apiKeyManager: ApiKeyManager by lazy {
        ApiKeyManager(applicationContext)
    }

    val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(applicationContext)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(applicationContext)
    }

    val cacheManager: CacheManager by lazy {
        CacheManager()
    }


    // Core Dependencies
    private val apiClient by lazy { com.example.flexinsight.data.api.FlexApiClient() }

    // Repositories
    private val exerciseRepository: com.example.flexinsight.data.repository.ExerciseRepository by lazy {
        com.example.flexinsight.data.repository.ExerciseRepositoryImpl(
            exerciseDao = database.exerciseDao(),
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager
        )
    }

    private val workoutRepository: com.example.flexinsight.data.repository.WorkoutRepository by lazy {
        com.example.flexinsight.data.repository.WorkoutRepositoryImpl(
            workoutDao = database.workoutDao(),
            exerciseDao = database.exerciseDao(),
            setDao = database.setDao(),
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager
        )
    }

    private val routineRepository: com.example.flexinsight.data.repository.RoutineRepository by lazy {
        com.example.flexinsight.data.repository.RoutineRepositoryImpl(
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            apiClient = apiClient,
            cacheManager = cacheManager,
            exerciseRepository = exerciseRepository
        )
    }

    private val statsRepository: com.example.flexinsight.data.repository.StatsRepository by lazy {
        com.example.flexinsight.data.repository.StatsRepositoryImpl(
            workoutDao = database.workoutDao(),
            exerciseDao = database.exerciseDao(),
            setDao = database.setDao(),
            exerciseRepository = exerciseRepository,
            cacheManager = cacheManager,
            dispatcherProvider = com.example.flexinsight.core.dispatchers.DefaultDispatcherProvider()
        )
    }

    val repository: FlexRepository by lazy {
        com.example.flexinsight.data.repository.FlexRepositoryImpl(
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

    val syncManager: SyncManager by lazy {
        SyncManager(
            repository = repository,
            networkMonitor = networkMonitor
        )
    }

    val syncScheduler: SyncScheduler by lazy {
        SyncScheduler(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic background sync
        syncScheduler.schedulePeriodicSync()
    }
}

