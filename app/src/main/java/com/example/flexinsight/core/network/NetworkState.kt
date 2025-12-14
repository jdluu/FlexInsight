package com.example.flexinsight.core.network

/**
 * Sealed class representing the current network state
 */
sealed class NetworkState {
    /**
     * Network is available and connected
     */
    data object Available : NetworkState()
    
    /**
     * Network is unavailable (no internet connection)
     */
    data object Unavailable : NetworkState()
    
    /**
     * Network state is unknown (checking or initializing)
     */
    data object Unknown : NetworkState()
    
    /**
     * Returns true if network is available
     */
    val isAvailable: Boolean
        get() = this is Available
}
