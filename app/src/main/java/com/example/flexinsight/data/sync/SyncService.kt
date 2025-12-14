package com.example.flexinsight.data.sync

import com.example.flexinsight.data.repository.FlexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service for handling synchronization between local database, Hevy API, and cloud database
 */
class SyncService(
    private val repository: FlexRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    /**
     * Sync with Hevy API on app launch
     * Explicitly syncs all data from the API
     */
    fun syncWithApi() {
        scope.launch {
            try {
                repository.syncAllData()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Sync with cloud database (structure ready, implementation TBD)
     * This will be implemented when a cloud database service is chosen
     */
    fun syncWithCloud() {
        scope.launch {
            try {
                repository.syncWithCloud()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Full sync - sync with both API and cloud
     */
    fun fullSync() {
        syncWithApi()
        syncWithCloud()
    }
}

