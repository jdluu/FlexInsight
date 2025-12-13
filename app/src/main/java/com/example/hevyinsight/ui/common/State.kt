package com.example.hevyinsight.ui.common

import com.example.hevyinsight.core.errors.ApiError

/**
 * Common UI state sealed classes for consistent state management across ViewModels
 */

/**
 * Sealed class for loading states
 */
sealed class LoadingState {
    data object Idle : LoadingState()
    data object Loading : LoadingState()
    data object Success : LoadingState()
    data class Error(val error: ApiError) : LoadingState()
}

/**
 * Sealed class for UI error states
 */
sealed class UiError {
    abstract val message: String
    
    data class Network(override val message: String) : UiError()
    data class Auth(override val message: String) : UiError()
    data class Server(override val message: String) : UiError()
    data class Unknown(override val message: String) : UiError()
    
    companion object {
        fun fromApiError(error: ApiError): UiError {
            return when (error) {
                is ApiError.NetworkError -> Network(error.message)
                is ApiError.AuthError -> Auth(error.message)
                is ApiError.ServerError -> Server(error.message)
                is ApiError.ClientError -> Unknown(error.message)
                is ApiError.Unknown -> Unknown(error.message)
            }
        }
    }
}
