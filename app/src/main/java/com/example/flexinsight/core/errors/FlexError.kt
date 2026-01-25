package com.example.flexinsight.core.errors

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Sealed class representing all possible error types in the application.
 * Designed to provide meaningful data for UI display and analytics.
 */
sealed class FlexError(val message: String, val cause: Throwable? = null) {
    
    sealed class Network(message: String, cause: Throwable? = null) : FlexError(message, cause) {
        object NoConnection : Network("No internet connection available")
        object Timeout : Network("The request timed out")
        data class ServerError(val code: Int, val rawMessage: String) : Network("Server error ($code): $rawMessage")
        object Unknown : Network("An unexpected network error occurred")
    }

    sealed class Auth(message: String) : FlexError(message) {
        object InvalidApiKey : Auth("API key is invalid or expired")
        object MissingApiKey : Auth("API key is missing")
    }

    sealed class Domain(message: String) : FlexError(message) {
        object WorkoutNotFound : Domain("Requested workout could not be found")
        object SyncFailed : Domain("Failed to synchronize data with server")
        data class Generic(val error: String) : Domain(error)
    }

    data class Unexpected(val error: Throwable) : FlexError(error.message ?: "An unexpected error occurred", error)

    /**
     * Helper to map any Throwable to a FlexError
     */
    companion object {
        fun from(throwable: Throwable): FlexError {
            return when (throwable) {
                is IOException -> Network.NoConnection
                is SocketTimeoutException -> Network.Timeout
                is HttpException -> {
                    when (throwable.code()) {
                        401, 403 -> Auth.InvalidApiKey
                        500, 502, 503, 504 -> Network.ServerError(throwable.code(), "Internal Server Error")
                        else -> Network.ServerError(throwable.code(), throwable.message())
                    }
                }
                else -> Unexpected(throwable)
            }
        }
    }
}

/**
 * Extension to convert any Throwable into a FlexError
 */
fun Throwable.toFlexError(): FlexError = FlexError.from(this)
