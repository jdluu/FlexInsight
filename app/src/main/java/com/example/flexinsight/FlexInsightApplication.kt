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
    
    val repository: FlexRepository by lazy {
        FlexRepository(
            database = database,
            apiKeyManager = apiKeyManager,
            networkMonitor = networkMonitor,
            cacheManager = cacheManager
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

