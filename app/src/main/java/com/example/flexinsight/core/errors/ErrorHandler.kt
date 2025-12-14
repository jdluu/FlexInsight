package com.example.flexinsight.core.errors

import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * Centralized error handling and conversion utility.
 * Converts exceptions and HTTP errors to ApiError types.
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Converts a Throwable to an ApiError
     */
    fun handleError(throwable: Throwable): ApiError {
        return when (throwable) {
            is HttpException -> handleHttpException(throwable)
            is IllegalStateException -> {
                // Gson deserialization errors often throw IllegalStateException
                if (throwable.message?.contains("Expected BEGIN_ARRAY") == true ||
                    throwable.message?.contains("Expected BEGIN_OBJECT") == true) {
                    Log.e(TAG, "JSON deserialization error - API response structure mismatch", throwable)
                    ApiError.Unknown("API response format error: ${throwable.message}", throwable)
                } else {
                    Log.e(TAG, "Unknown error", throwable)
                    ApiError.Unknown(throwable.message ?: "Unknown error", throwable)
                }
            }
            is SocketTimeoutException -> ApiError.NetworkError.Timeout(30L)
            is UnknownHostException -> ApiError.NetworkError.NoConnection
            is IOException -> ApiError.NetworkError.ConnectionError(throwable)
            else -> {
                Log.e(TAG, "Unknown error", throwable)
                ApiError.Unknown(throwable.message ?: "Unknown error", throwable)
            }
        }
    }
    
    /**
     * Converts an HTTP exception to an ApiError
     */
    fun handleHttpException(exception: HttpException): ApiError {
        val code = exception.code()
        val message = exception.message()
        
        return when (code) {
            // Authentication errors
            401 -> ApiError.AuthError.InvalidApiKey
            403 -> ApiError.AuthError.Forbidden
            
            // Client errors
            400 -> ApiError.ClientError.BadRequest
            404 -> ApiError.ClientError.NotFound
            429 -> ApiError.ClientError.RateLimited
            in 400..499 -> ApiError.ClientError.Other(code)
            
            // Server errors
            500 -> ApiError.ServerError.InternalServerError
            502 -> ApiError.ServerError.BadGateway
            503 -> ApiError.ServerError.ServiceUnavailable
            504 -> ApiError.ServerError.GatewayTimeout
            in 500..599 -> ApiError.ServerError.Other(code)
            
            else -> {
                Log.w(TAG, "Unhandled HTTP code: $code")
                ApiError.Unknown("HTTP $code: $message", exception)
            }
        }
    }
    
    /**
     * Logs an error with appropriate level based on error type
     */
    fun logError(error: ApiError, context: String = "") {
        val prefix = if (context.isNotEmpty()) "[$context] " else ""
        
        when (error) {
            is ApiError.AuthError -> {
                Log.e(TAG, "$prefix${error.message} (HTTP ${error.httpCode})")
            }
            is ApiError.NetworkError -> {
                Log.w(TAG, "$prefix${error.message}", error.cause)
            }
            is ApiError.ServerError -> {
                Log.w(TAG, "$prefix${error.message} (HTTP ${error.httpCode})")
            }
            is ApiError.ClientError -> {
                Log.w(TAG, "$prefix${error.message} (HTTP ${error.httpCode})")
            }
            is ApiError.Unknown -> {
                Log.e(TAG, "$prefix${error.message}", error.cause)
            }
        }
    }
}
