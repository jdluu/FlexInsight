package com.jdluu.flexinsight.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

/**
 * OkHttp interceptor that implements exponential backoff with jitter and Retry-After support
 * for transient errors.
 *
 * Retry strategy:
 * - Retries transient errors (network errors, 5xx server errors, 429 rate limits) up to [maxRetries] times
 * - Honors HTTP `Retry-After` headers (delay-seconds or HTTP-date) when present on 429/503 responses
 * - Falls back to exponential backoff with bounded jitter: `min(maxDelayMillis, baseDelay * 2^attempt + jitter)`
 * - Does NOT retry authentication errors (401, 403) - fails immediately
 * - Does NOT retry other client errors (4xx) except 429
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMillis: Long = 1000L,
    private val maxDelayMillis: Long = 30_000L,
    private val jitterRatio: Double = 0.25,
    private val randomFraction: () -> Double = { ThreadLocalRandom.current().nextDouble() },
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        // Try the request up to maxRetries + 1 times (initial attempt + retries)
        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)

                // Check if we should retry based on response code
                if (shouldRetry(response, attempt)) {
                    val delay = resolveResponseDelayMillis(response, attempt)
                    response.close()

                    sleepBeforeRetry(delay)
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
                sleepBeforeRetry(delay)
            }
        }

        // If we get here, all retries failed
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    private fun sleepBeforeRetry(delayMillis: Long) {
        if (delayMillis <= 0L) return
        try {
            sleeper(delayMillis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted during retry delay", e)
        }
    }

    /**
     * Determines if a response should be retried
     */
    private fun shouldRetry(response: Response, attempt: Int): Boolean {
        if (attempt >= maxRetries) {
            return false
        }
        if (response.isSuccessful) {
            return false
        }
        val code = response.code
        if (code == 401 || code == 403) {
            return false
        }
        if (code in 500..599) {
            return true
        }
        if (code == 429) {
            return true
        }
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

    private fun resolveResponseDelayMillis(response: Response, attempt: Int): Long {
        if (response.code == 429 || response.code == 503) {
            parseRetryAfterMillis(response.header("Retry-After"))?.let { return it }
        }
        return calculateBackoffDelay(attempt)
    }

    internal fun parseRetryAfterMillis(headerValue: String?): Long? {
        val trimmed = headerValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        trimmed.toLongOrNull()?.let { seconds ->
            return (seconds * 1000L).coerceIn(0L, maxDelayMillis)
        }
        return try {
            val targetMillis = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(trimmed)).toEpochMilli()
            (targetMillis - clockMillis()).coerceIn(0L, maxDelayMillis)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /**
     * Calculates exponential backoff delay in milliseconds with bounded positive jitter.
     */
    internal fun calculateBackoffDelay(attempt: Int): Long {
        val multiplier = 2.0.pow(attempt.toDouble())
        val base = (baseDelayMillis * multiplier).toLong().coerceAtMost(maxDelayMillis)
        val jitter = (base * jitterRatio.coerceAtLeast(0.0) * randomFraction().coerceIn(0.0, 1.0)).toLong()
        return (base + jitter).coerceAtMost(maxDelayMillis)
    }
}
