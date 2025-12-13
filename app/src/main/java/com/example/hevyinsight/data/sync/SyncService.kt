package com.example.hevyinsight.data.sync

import com.example.hevyinsight.data.repository.HevyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service for handling synchronization between local database, Hevy API, and cloud database
 */
class SyncService(
    private val repository: HevyRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    /**
     * Sync with Hevy API on app launch
     */
    fun syncWithApi() {
        scope.launch {
            try {
                // Repository handles the sync internally when data is requested
                // This method can be called explicitly for background sync
                repository.getWorkoutCount().collect { } // Trigger sync
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

