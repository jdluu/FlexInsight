package com.example.flexinsight.ui.common

import com.example.flexinsight.core.errors.ApiError

/**
 * Common UI state sealed classes for consistent state management across ViewModels
 */

/**
 * Sealed class for loading states
 * Replaces boolean isLoading flags with type-safe states
 */
sealed class LoadingState {
    /**
     * Initial state, no operation in progress
     */
    data object Idle : LoadingState()

    /**
     * Operation is currently in progress
     */
    data object Loading : LoadingState()

    /**
     * Operation completed successfully
     */
    data object Success : LoadingState()

    /**
     * Operation failed with an error
     */
    data class Error(val error: ApiError) : LoadingState()

    /**
     * Returns true if currently loading
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns true if operation succeeded
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if operation failed
     */
    val isError: Boolean
        get() = this is Error
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
