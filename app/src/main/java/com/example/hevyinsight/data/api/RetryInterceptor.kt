package com.example.hevyinsight.data.api

import com.example.hevyinsight.core.errors.ApiError
import com.example.hevyinsight.core.errors.ErrorHandler
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.math.pow

/**
 * OkHttp interceptor that implements exponential backoff retry logic for transient errors.
 * 
 * Retry strategy:
 * - Retries transient errors (network errors, server errors) up to maxRetries times
 * - Uses exponential backoff: delay = baseDelay * 2^attemptNumber
 * - Does NOT retry authentication errors (401, 403) - fails immediately
 * - Does NOT retry client errors (4xx) except rate limits (429)
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMillis: Long = 1000L
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        
        // Try the request up to maxRetries + 1 times (initial attempt + retries)
        for (attempt in 0..maxRetries) {
            try {
                response = chain.proceed(request)
                
                // Check if we should retry based on response code
                if (shouldRetry(response, attempt)) {
                    response.close()
                    val delay = calculateBackoffDelay(attempt)
                    
                    // Wait before retrying
                    try {
                        Thread.sleep(delay)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Interrupted during retry delay", e)
                    }
                    
                    // Continue to next retry attempt
                    continue
                }
                
                // Success or non-retryable error - return response
                return response
                
            } catch (e: IOException) {
                lastException = e
                
                // Check if this is a retryable exception
                if (!isRetryableException(e) || attempt >= maxRetries) {
                    throw e
                }
                
                // Calculate delay and wait
                val delay = calculateBackoffDelay(attempt)
                try {
                    Thread.sleep(delay)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Interrupted during retry delay", ie)
                }
            }
        }
        
        // If we get here, all retries failed
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
    
    /**
     * Determines if a response should be retried
     */
    private fun shouldRetry(response: Response, attempt: Int): Boolean {
        // Don't retry if we've exceeded max retries
        if (attempt >= maxRetries) {
            return false
        }
        
        // Don't retry successful responses
        if (response.isSuccessful) {
            return false
        }
        
        val code = response.code
        
        // Never retry authentication errors - fail immediately
        if (code == 401 || code == 403) {
            return false
        }
        
        // Retry server errors (5xx)
        if (code in 500..599) {
            return true
        }
        
        // Retry rate limit errors (429) - but with longer delay
        if (code == 429) {
            return true
        }
        
        // Don't retry other client errors (4xx)
        if (code in 400..499) {
            return false
        }
        
        // Don't retry unknown status codes
        return false
    }
    
    /**
     * Determines if an exception is retryable
     */
    private fun isRetryableException(exception: IOException): Boolean {
        return when (exception) {
            is SocketTimeoutException -> true
            is java.net.ConnectException -> true
            is java.net.UnknownHostException -> true
            else -> false
        }
    }
    
    /**
     * Calculates exponential backoff delay in milliseconds
     * Formula: baseDelay * 2^attemptNumber
     * 
     * Example with baseDelay=1000ms:
     * - Attempt 0: 1000ms
     * - Attempt 1: 2000ms
     * - Attempt 2: 4000ms
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val multiplier = 2.0.pow(attempt.toDouble())
        return (baseDelayMillis * multiplier).toLong().coerceAtMost(30000L) // Max 30 seconds
    }
}
