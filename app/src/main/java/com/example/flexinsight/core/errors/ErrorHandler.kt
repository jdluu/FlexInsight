package com.example.flexinsight.core.errors

import android.util.Log
import com.example.flexinsight.core.logger.AppLogger
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
                    AppLogger.e("JSON deserialization error - API response structure mismatch", throwable, TAG)
                    // ApiError.ServerError.Other(code) is the only subclass that takes a code?
                    // Or explicit subclass?
                    // ApiError.ServerError is sealed.
                    // We need a specific instance.
                    // Use ApiError.Unknown for format error if no specific ServerError fits,
                    // or define a new one. Or use Other(500).
                    // ApiError.Server doesn't exist. Use Unknown or ServerError.Other.
                    // Given the message "Server response format error", let's use Unknown for now as it captures the message best
                    ApiError.Unknown("Server response format error", throwable)
                } else {
                    AppLogger.e("Unknown error", throwable, TAG)
                    ApiError.Unknown(throwable.message ?: "Unknown error", throwable)
                }
            }
            is SocketTimeoutException -> ApiError.NetworkError.Timeout(30L)
            is UnknownHostException -> ApiError.NetworkError.NoConnection
            is IOException -> ApiError.NetworkError.ConnectionError(throwable)
            else -> {
                AppLogger.e("Unknown error", throwable, TAG)
                // ApiError.Unknown takes (errorMessage: String, errorCause: Throwable?)
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
                AppLogger.w("Unhandled HTTP code: $code", tag = TAG)
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
            is ApiError.NetworkError, is ApiError.NetworkError.Timeout, is ApiError.ServerError -> {
                // ServerError and ClientError have httpCode property, NetworkError does not (except indirectly?)
                // NetworkError does NOT have httpCode. ServerError DOES.
                // We need to split them or cast carefully.
                if (error is ApiError.ServerError) {
                    AppLogger.e("$prefix${error.message} (HTTP ${error.httpCode})", tag = TAG)
                } else {
                     AppLogger.e("$prefix${error.message}", tag = TAG)
                }
            }
            is ApiError.AuthError -> {
                AppLogger.w("$prefix${error.message} (HTTP ${error.httpCode})", tag = TAG)
            }
            is ApiError.ClientError -> {
                AppLogger.w("$prefix${error.message} (HTTP ${error.httpCode})", tag = TAG)
                // ClientError doesn't seem to have validationErrors in the definition I saw?
                // Checking ApiError.kt again... ClientError subclasses don't show validationErrors.
                // It was likely removed or never there. I will remove the validation loop.
            }
            is ApiError.Unknown -> {
                AppLogger.w("$prefix${error.message}", error.cause, TAG)
            }
        }
    }
}
