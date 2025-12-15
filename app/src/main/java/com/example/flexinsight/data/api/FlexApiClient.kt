package com.example.flexinsight.data.api

import android.util.Log
import com.example.flexinsight.core.logger.AppLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory class for creating FlexApiService instances.
 * Converted from object to class to support dependency injection.
 */
class FlexApiClient {
    companion object {
    private const val BASE_URL = "https://api.hevyapp.com/"
    private const val TIMEOUT_SECONDS = 30L
    private const val TAG = "FlexApiClient"
    }

    /**
     * Creates an OkHttpClient with API key interceptor and retry logic
     */
    private fun createOkHttpClient(apiKey: String): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Change to NONE in production
        }

            val apiKeyInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                // Use 'api-key' header as specified in Hevy API CORS headers
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")

            val request = requestBuilder.build()
            val response = chain.proceed(request)

            // Log API errors for debugging (body is already logged by HttpLoggingInterceptor)
            if (!response.isSuccessful) {
                AppLogger.e("API request failed: ${response.code} ${response.message}", tag = TAG)
                AppLogger.e("Request URL: ${request.url}", tag = TAG)

                if (response.code == 401) {
                    AppLogger.e("Invalid API key - check your API key in settings", tag = TAG)
                }
            }

            response
        }

        // Create retry interceptor with exponential backoff
        val retryInterceptor = RetryInterceptor(
            maxRetries = 3,
            baseDelayMillis = 1000L
        )

        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(retryInterceptor) // Add retry logic before logging
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates a Retrofit instance with the provided API key
     */
    fun createApiService(apiKey: String): FlexApiService {
        val client = createOkHttpClient(apiKey)

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlexApiService::class.java)
    }
}

