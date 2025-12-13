package com.example.hevyinsight

import android.app.Application
import androidx.room.Room
import com.example.hevyinsight.core.network.NetworkMonitor
import com.example.hevyinsight.data.cache.CacheManager
import com.example.hevyinsight.data.local.HevyDatabase
import com.example.hevyinsight.data.preferences.ApiKeyManager
import com.example.hevyinsight.data.preferences.UserPreferencesManager
import com.example.hevyinsight.data.repository.HevyRepository
import com.example.hevyinsight.data.sync.SyncManager
import com.example.hevyinsight.data.sync.SyncScheduler

class HevyInsightApplication : Application() {
    val database: HevyDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            HevyDatabase::class.java,
            HevyDatabase.DATABASE_NAME
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
    
    val repository: HevyRepository by lazy {
        HevyRepository(
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

