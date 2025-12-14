package com.example.hevyinsight.core.errors

/**
 * Sealed class representing the result of an operation that can either succeed or fail.
 * This replaces nullable returns and exception-based error handling with explicit error types.
 *
 * @param T The type of the successful value
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with a value
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with an error
     */
    data class Error(val error: ApiError) : Result<Nothing>()
    
    /**
     * Returns true if the result is a success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if the result is an error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Gets the data if successful, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Gets the data if successful, or returns the default value
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }
    
    /**
     * Maps the success value to another type
     */
    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    /**
     * Maps the error to another error type
     */
    fun mapError(transform: (ApiError) -> ApiError): Result<T> = when (this) {
        is Success -> this
        is Error -> Error(transform(error))
    }
    
    /**
     * Executes an action if the result is a success
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * Executes an action if the result is an error
     */
    inline fun onError(action: (ApiError) -> Unit): Result<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }
    
    /**
     * Companion object with helper functions for creating Result instances
     */
    companion object {
        /**
         * Helper function to create a success result
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Helper function to create an error result
         */
        fun <T> error(error: ApiError): Result<T> = Error(error)
    }
}

/**
 * Helper function to wrap a nullable value in a Result
 */
fun <T> T?.toResult(error: ApiError): Result<T> = if (this != null) {
    Result.Success(this)
} else {
    Result.Error(error)
}
