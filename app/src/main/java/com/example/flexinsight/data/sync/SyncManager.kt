package com.example.flexinsight.data.sync

import com.example.flexinsight.core.errors.ApiError
import com.example.flexinsight.core.network.NetworkMonitor
import com.example.flexinsight.core.network.NetworkState
import com.example.flexinsight.data.repository.FlexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages sync state and coordinates background sync operations.
 * Implements smart sync strategy: manual + background + on resume
 */
class SyncManager(
    private val repository: FlexRepository,
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var lastSyncTime: Long = 0L
    private val minSyncIntervalMillis = 15 * 60 * 1000L // 15 minutes
    
    /**
     * Performs a manual sync (user-triggered)
     * Always attempts sync regardless of network state or last sync time
     */
    suspend fun syncManually(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing
                
                // Check network before syncing
                if (!networkMonitor.hasNetworkConnection()) {
                    val error = ApiError.NetworkError.NoConnection
                    _syncState.value = SyncState.Error(error)
                    return@withContext Result.failure(Exception(error.message))
                }
                
                repository.syncAllData()
                
                lastSyncTime = System.currentTimeMillis()
                _syncState.value = SyncState.Success(lastSyncTime)
                Result.success(Unit)
            } catch (e: Exception) {
                val error = if (e is ApiError) {
                    e
                } else {
                    ApiError.Unknown(e.message ?: "Unknown sync error", e)
                }
                _syncState.value = SyncState.Error(error)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Performs a background sync if conditions are met
     * Only syncs if:
     * - Network is available
     * - Last sync was more than minSyncIntervalMillis ago
     */
    suspend fun syncIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            // Check if we should sync
            if (!shouldSync()) {
                return@withContext false
            }
            
            try {
                _syncState.value = SyncState.Syncing
                
                repository.syncAllData()
                
                lastSyncTime = System.currentTimeMillis()
                _syncState.value = SyncState.Success(lastSyncTime)
                true
            } catch (e: Exception) {
                // Don't update state for background sync failures
                // They're silent and won't interrupt the user
                false
            }
        }
    }
    
    /**
     * Syncs on app resume if conditions are met
     */
    fun syncOnResume() {
        scope.launch {
            syncIfNeeded()
        }
    }
    
    /**
     * Determines if a sync should be performed
     */
    private suspend fun shouldSync(): Boolean {
        // Check network availability
        if (!networkMonitor.hasNetworkConnection()) {
            return false
        }
        
        // Check if enough time has passed since last sync
        val timeSinceLastSync = System.currentTimeMillis() - lastSyncTime
        if (timeSinceLastSync < minSyncIntervalMillis) {
            return false
        }
        
        // Don't sync if already syncing
        if (_syncState.value is SyncState.Syncing) {
            return false
        }
        
        return true
    }
    
    /**
     * Gets the time since last successful sync in milliseconds
     */
    fun getTimeSinceLastSync(): Long {
        return if (lastSyncTime > 0) {
            System.currentTimeMillis() - lastSyncTime
        } else {
            Long.MAX_VALUE // Never synced
        }
    }
}
