package com.jdluu.flexinsight.data.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RetryInterceptorTest {

    private fun request(): Request = Request.Builder().url("https://api.hevyapp.com/v1/workouts").build()

    private fun response(
        req: Request,
        code: Int,
        retryAfter: String? = null
    ): Response {
        val builder = Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("msg")
            .body("{}".toResponseBody("application/json".toMediaType()))
        if (retryAfter != null) {
            builder.header("Retry-After", retryAfter)
        }
        return builder.build()
    }

    @Test
    fun `429 with integer Retry-After uses exact header seconds capped by maxDelay`() {
        val sleeps = mutableListOf<Long>()
        val interceptor = RetryInterceptor(
            maxRetries = 2,
            baseDelayMillis = 1000L,
            maxDelayMillis = 30_000L,
            sleeper = { sleeps.add(it) }
        )
        val req = request()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(req) } returnsMany listOf(
            response(req, 429, retryAfter = "5"),
            response(req, 200)
        )

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(listOf(5000L), sleeps)
        verify(exactly = 2) { chain.proceed(req) }
    }

    @Test
    fun `503 with HTTP-date Retry-After uses difference from clock`() {
        val nowMillis = 1_700_000_000_000L
        val targetMillis = nowMillis + 4_000L
        val httpDate = DateTimeFormatter.RFC_1123_DATE_TIME
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(targetMillis))

        val sleeps = mutableListOf<Long>()
        val interceptor = RetryInterceptor(
            maxRetries = 2,
            clockMillis = { nowMillis },
            sleeper = { sleeps.add(it) }
        )
        val req = request()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(req) } returnsMany listOf(
            response(req, 503, retryAfter = httpDate),
            response(req, 200)
        )

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(listOf(4000L), sleeps)
    }

    @Test
    fun `invalid Retry-After falls back to exponential backoff with jitter`() {
        val sleeps = mutableListOf<Long>()
        val interceptor = RetryInterceptor(
            maxRetries = 2,
            baseDelayMillis = 1000L,
            jitterRatio = 0.25,
            randomFraction = { 0.8 },
            sleeper = { sleeps.add(it) }
        )
        val req = request()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(req) } returnsMany listOf(
            response(req, 429, retryAfter = "invalid-header"),
            response(req, 500),
            response(req, 200)
        )

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        // Attempt 0: 1000 + 1000 * 0.25 * 0.8 = 1200
        // Attempt 1: 2000 + 2000 * 0.25 * 0.8 = 2400
        assertEquals(listOf(1200L, 2400L), sleeps)
    }

    @Test
    fun `auth errors 401 and 403 fail immediately without retrying`() {
        val sleeps = mutableListOf<Long>()
        val interceptor = RetryInterceptor(maxRetries = 3, sleeper = { sleeps.add(it) })
        val req = request()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(req) } returns response(req, 401)

        val result = interceptor.intercept(chain)

        assertEquals(401, result.code)
        assertTrue(sleeps.isEmpty())
        verify(exactly = 1) { chain.proceed(req) }
    }

    @Test
    fun `socket timeout retries with exponential backoff and returns on recovery`() {
        val sleeps = mutableListOf<Long>()
        val interceptor = RetryInterceptor(
            maxRetries = 2,
            baseDelayMillis = 500L,
            jitterRatio = 0.5,
            randomFraction = { 0.0 },
            sleeper = { sleeps.add(it) }
        )
        val req = request()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(req) } throws SocketTimeoutException("timeout") andThen response(req, 200)

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(listOf(500L), sleeps)
    }

    @Test
    fun `parseRetryAfterMillis clamps huge delays and ignores blanks`() {
        val interceptor = RetryInterceptor(maxDelayMillis = 10_000L)
        assertNull(interceptor.parseRetryAfterMillis(null))
        assertNull(interceptor.parseRetryAfterMillis("   "))
        assertEquals(10_000L, interceptor.parseRetryAfterMillis("120"))
    }
}
