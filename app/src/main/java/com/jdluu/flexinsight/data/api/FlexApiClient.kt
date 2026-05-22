package com.jdluu.flexinsight.data.api

import com.jdluu.flexinsight.BuildConfig
import com.jdluu.flexinsight.core.logger.AppLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating [FlexApiService] instances with API key auth and retry logic.
 */
@Singleton
class FlexApiClient @Inject constructor() {

    /**
     * Creates an OkHttpClient with API key interceptor and retry logic.
     */
    private fun createOkHttpClient(apiKey: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        builder.addInterceptor(apiKeyInterceptor(apiKey))
        builder.addInterceptor(RetryInterceptor(maxRetries = 3, baseDelayMillis = 1000L))

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }

    private fun apiKeyInterceptor(apiKey: String): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("api-key", apiKey)
            .header("Content-Type", "application/json")
            .build()

        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            AppLogger.e("API request failed: ${response.code} ${response.message}", tag = TAG)
            AppLogger.e("Request URL: ${request.url}", tag = TAG)
            if (response.code == 401) {
                AppLogger.e("Invalid API key - check your API key in settings", tag = TAG)
            }
        }

        response
    }

    /**
     * Creates a Retrofit instance with the provided API key.
     */
    fun createApiService(apiKey: String): FlexApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(apiKey))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlexApiService::class.java)
    }

    companion object {
        private const val BASE_URL = "https://api.hevyapp.com/"
        private const val TIMEOUT_SECONDS = 30L
        private const val TAG = "FlexApiClient"
    }
}
