package com.example.hevyinsight

import android.app.Application
import androidx.room.Room
import com.example.hevyinsight.data.local.HevyDatabase
import com.example.hevyinsight.data.preferences.ApiKeyManager
import com.example.hevyinsight.data.preferences.UserPreferencesManager
import com.example.hevyinsight.data.repository.HevyRepository

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
    
    val repository: HevyRepository by lazy {
        HevyRepository(database, apiKeyManager)
    }
}

