package com.example.hevyinsight.data.sync

import com.example.hevyinsight.core.errors.ApiError

/**
 * Sealed class representing the current sync state
 */
sealed class SyncState {
    /**
     * No sync in progress
     */
    data object Idle : SyncState()
    
    /**
     * Sync is currently in progress
     */
    data object Syncing : SyncState()
    
    /**
     * Last sync completed successfully
     */
    data class Success(
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncState()
    
    /**
     * Last sync failed with an error
     */
    data class Error(
        val error: ApiError,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncState()
    
    /**
     * Returns true if sync is in progress
     */
    val isSyncing: Boolean
        get() = this is Syncing
    
    /**
     * Returns true if last sync was successful
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if last sync failed
     */
    val isError: Boolean
        get() = this is Error
}
