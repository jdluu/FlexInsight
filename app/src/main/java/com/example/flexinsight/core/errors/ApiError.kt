package com.example.flexinsight.core.errors

/**
 * Sealed class representing different types of API errors.
 * This allows for explicit error handling and type-safe error propagation.
 */
sealed class ApiError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /**
     * Network-related errors (no internet, timeout, connection issues)
     * These are transient and should be retried with exponential backoff
     */
    sealed class NetworkError(
        message: String,
        cause: Throwable? = null
    ) : ApiError(message, cause) {
        /**
         * No internet connection available
         */
        data object NoConnection : NetworkError("No internet connection available")

        /**
         * Request timed out
         */
        data class Timeout(
            val timeoutSeconds: Long
        ) : NetworkError("Request timed out after ${timeoutSeconds}s")

        /**
         * Connection error (unable to reach server)
         */
        data class ConnectionError(
            val errorCause: Throwable? = null
        ) : NetworkError("Unable to connect to server", errorCause) {
            override val cause: Throwable? = errorCause
        }

        /**
         * Unknown network error
         */
        data class Unknown(
            val errorCause: Throwable? = null
        ) : NetworkError("Unknown network error", errorCause) {
            override val cause: Throwable? = errorCause
        }
    }

    /**
     * Authentication/authorization errors
     * These should fail fast without retry
     */
    sealed class AuthError(
        message: String,
        val httpCode: Int
    ) : ApiError(message) {
        /**
         * Invalid API key (401)
         */
        data object InvalidApiKey : AuthError("Invalid API key", 401)

        /**
         * Forbidden access (403)
         */
        data object Forbidden : AuthError("Access forbidden", 403)

        /**
         * Unauthorized (401)
         */
        data object Unauthorized : AuthError("Unauthorized", 401)
    }

    /**
     * Server errors (5xx)
     * These are transient and may be retried
     */
    sealed class ServerError(
        message: String,
        val httpCode: Int
    ) : ApiError(message) {
        /**
         * Internal server error (500)
         */
        data object InternalServerError : ServerError("Internal server error", 500)

        /**
         * Bad gateway (502)
         */
        data object BadGateway : ServerError("Bad gateway", 502)

        /**
         * Service unavailable (503)
         */
        data object ServiceUnavailable : ServerError("Service unavailable", 503)

        /**
         * Gateway timeout (504)
         */
        data object GatewayTimeout : ServerError("Gateway timeout", 504)

        /**
         * Other server error
         */
        data class Other(
            val code: Int
        ) : ServerError("Server error: $code", code)
    }

    /**
     * Client errors (4xx, excluding auth)
     * These should fail fast without retry
     */
    sealed class ClientError(
        message: String,
        val httpCode: Int
    ) : ApiError(message) {
        /**
         * Bad request (400)
         */
        data object BadRequest : ClientError("Bad request", 400)

        /**
         * Not found (404)
         */
        data object NotFound : ClientError("Resource not found", 404)

        /**
         * Rate limited (429)
         */
        data object RateLimited : ClientError("Rate limit exceeded", 429)

        /**
         * Other client error
         */
        data class Other(
            val code: Int
        ) : ClientError("Client error: $code", code)
    }

    /**
     * Unknown or unexpected errors
     */
    data class Unknown(
        val errorMessage: String,
        val errorCause: Throwable? = null
    ) : ApiError(errorMessage, errorCause) {
        override val message: String = errorMessage
        override val cause: Throwable? = errorCause
    }

    /**
     * Returns true if this error should be retried (transient errors)
     */
    val isRetryable: Boolean
        get() = when (this) {
            is NetworkError -> true
            is ServerError -> true
            is AuthError -> false
            is ClientError -> when (this) {
                is ClientError.RateLimited -> true // Rate limits can be retried after delay
                else -> false
            }
            is Unknown -> false
        }

    /**
     * Returns true if this is an authentication error
     */
    val isAuthError: Boolean
        get() = this is AuthError
}
