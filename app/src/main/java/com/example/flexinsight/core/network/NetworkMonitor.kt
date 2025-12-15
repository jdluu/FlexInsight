package com.example.flexinsight.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitors network connectivity and provides the current network state as a Flow.
 * Caches the network state to avoid repeated checks.
 */
class NetworkMonitor(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Current cached network state
     */
    private var cachedState: NetworkState = NetworkState.Unknown

    /**
     * Flow of network state changes
     */
    val networkState: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cachedState = NetworkState.Available
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                cachedState = NetworkState.Unavailable
                trySend(NetworkState.Unavailable)
            }

            override fun onUnavailable() {
                cachedState = NetworkState.Unavailable
                trySend(NetworkState.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        cachedState = getCurrentNetworkState()
        trySend(cachedState)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Gets the current network state synchronously
     * Always checks connectivity to ensure accuracy
     */
    fun getCurrentNetworkState(): NetworkState {
        return if (isNetworkAvailable()) {
            NetworkState.Available.also { cachedState = it }
        } else {
            NetworkState.Unavailable.also { cachedState = it }
        }
    }

    /**
     * Checks if network is currently available
     */
    @Suppress("DEPRECATION")
    fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // Use deprecated API for Android versions below M (API 23)
            val networkInfo: android.net.NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.state == android.net.NetworkInfo.State.CONNECTED
        }
    }

    /**
     * Checks if network is available and connected (synchronous check)
     */
    fun hasNetworkConnection(): Boolean {
        return getCurrentNetworkState() is NetworkState.Available
    }
}
