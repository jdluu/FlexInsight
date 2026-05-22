@file:OptIn(ExperimentalCoroutinesApi::class)

package com.jdluu.flexinsight.data.ai

import android.content.Context
import android.util.Log
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.BuildConfig
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device AI via ML Kit GenAI Prompt API (Gemini Nano through Android AICore).
 *
 * @see <a href="https://developers.google.com/ml-kit/genai/prompt/android/get-started">ML Kit Prompt API</a>
 */
@Singleton
class GeminiNanoClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) : FlexAIClient {

    private val generativeModel: GenerativeModel by lazy {
        Generation.getClient()
    }

    private val prepareMutex = Mutex()
    private var isWarmedUp = false

    /**
     * Debug-only: surfaces AI UI with stub responses when Nano/AICore is unavailable.
     * Real Gemini Nano behavior must be validated on a supported physical device.
     */
    private suspend fun isForceAiEnabled(): Boolean =
        BuildConfig.DEBUG && userPreferencesManager.getForceAiEnable()

    override suspend fun isAvailable(): Boolean {
        if (isForceAiEnabled()) return true
        return resolveFeatureStatus() in READY_STATUSES
    }

    override suspend fun getFeatureStatus(): AiFeatureStatus {
        if (isForceAiEnabled()) return AiFeatureStatus.Ready
        return when (resolveFeatureStatus()) {
            FeatureStatus.AVAILABLE -> AiFeatureStatus.Ready
            FeatureStatus.DOWNLOADABLE -> AiFeatureStatus.Downloadable
            FeatureStatus.DOWNLOADING -> AiFeatureStatus.Downloading
            FeatureStatus.UNAVAILABLE -> AiFeatureStatus.Unavailable
            else -> AiFeatureStatus.Unavailable
        }
    }

    override suspend fun prepareModel(): Result<Unit> {
        if (isForceAiEnabled()) {
            return Result.Success(Unit)
        }

        return prepareMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    when (resolveFeatureStatus()) {
                        FeatureStatus.AVAILABLE -> {
                            warmUpIfNeeded()
                            Result.Success(Unit)
                        }
                        FeatureStatus.DOWNLOADABLE -> {
                            var failed: GenAiException? = null
                            generativeModel.download().collect { status ->
                                when (status) {
                                    is DownloadStatus.DownloadFailed -> {
                                        failed = status.e
                                    }
                                    DownloadStatus.DownloadCompleted -> {
                                        warmUpIfNeeded()
                                    }
                                    else -> Unit
                                }
                            }
                            if (failed != null) {
                                Result.Error(ApiError.Unknown(failed?.message ?: "Model download failed"))
                            } else if (resolveFeatureStatus() == FeatureStatus.AVAILABLE) {
                                Result.Success(Unit)
                            } else {
                                Result.Error(ApiError.Unknown("Gemini Nano is still preparing on this device."))
                            }
                        }
                        FeatureStatus.DOWNLOADING -> {
                            Result.Error(ApiError.Unknown("Gemini Nano is downloading. Try again shortly."))
                        }
                        else -> {
                            Result.Error(ApiError.Unknown("On-device AI is not supported on this device."))
                        }
                    }
                } catch (e: GenAiException) {
                    Log.e(TAG, "GenAI prepare failed", e)
                    Result.Error(ApiError.Unknown(e.message ?: "AI preparation failed"))
                } catch (e: Exception) {
                    Log.e(TAG, "AI prepare failed", e)
                    Result.Error(ApiError.Unknown(e.message ?: "AI preparation failed"))
                }
            }
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>>
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            if (isForceAiEnabled()) {
                return@withContext try {
                    generateRealResponse(prompt, history)
                } catch (e: Exception) {
                    Log.w(TAG, "Force-AI dev fallback after real inference failed", e)
                    generateDevFallbackResponse(prompt)
                }
            }

            when (val prepared = prepareModel()) {
                is Result.Error -> return@withContext prepared
                is Result.Success -> Unit
            }

            try {
                generateRealResponse(prompt, history)
            } catch (e: GenAiException) {
                Log.e(TAG, "generateContent failed", e)
                Result.Error(ApiError.Unknown(e.message ?: "AI generation failed"))
            } catch (e: Exception) {
                Log.e(TAG, "generateContent failed", e)
                Result.Error(ApiError.Unknown(e.message ?: "AI generation failed"))
            }
        }
    }

    override suspend fun generateWorkoutPlan(prompt: String): Result<String> {
        return generateResponse(
            prompt = prompt,
            history = emptyList()
        )
    }

    override fun generateResponseStream(
        prompt: String,
        history: List<Pair<String, String>>
    ): Flow<String> = flow {
        if (isForceAiEnabled()) {
            try {
                val result = generateRealResponse(prompt, history)
                if (result is Result.Success) {
                    emit(result.data)
                } else if (result is Result.Error) {
                    emit("Error: ${result.error.message}")
                }
            } catch (e: Exception) {
                val fallback = generateDevFallbackResponse(prompt)
                if (fallback is Result.Success) emit(fallback.data)
                else emit("Error: ${(fallback as Result.Error).error.message}")
            }
            return@flow
        }

        when (val prepared = prepareModel()) {
            is Result.Error -> {
                emit("Error: ${prepared.error.message}")
                return@flow
            }
            is Result.Success -> Unit
        }

        try {
            val request = buildContentRequest(prompt, history)
            generativeModel.generateContentStream(request).collect { chunk ->
                chunk.candidates.firstOrNull()?.text?.let { text ->
                    if (text.isNotEmpty()) emit(text)
                }
            }
        } catch (e: GenAiException) {
            Log.e(TAG, "generateContentStream failed", e)
            emit("Error: ${e.message ?: "Streaming failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "generateContentStream failed", e)
            emit("Error: ${e.message ?: "Streaming failed"}")
        }
    }

    private suspend fun generateRealResponse(
        prompt: String,
        history: List<Pair<String, String>>
    ): Result<String> {
        val request = buildContentRequest(prompt, history)
        val response = generativeModel.generateContent(request)
        val text = response.candidates.firstOrNull()?.text.orEmpty()

        return if (text.isNotBlank()) {
            Result.Success(text)
        } else {
            Result.Error(ApiError.Unknown("Empty response from on-device AI"))
        }
    }

    private fun buildContentRequest(
        prompt: String,
        history: List<Pair<String, String>>
    ) = generateContentRequest(TextPart(prompt)) {
        val systemPrefix = history
            .filter { (role, content) -> role == "user" && content.startsWith("System Context:") }
            .joinToString("\n") { (_, content) -> content }
            .ifBlank { null }

        val conversation = history
            .filterNot { (role, content) -> role == "user" && content.startsWith("System Context:") }
            .joinToString("\n") { (role, content) -> "$role: $content" }

        val prefixText = when {
            !systemPrefix.isNullOrBlank() && conversation.isNotBlank() ->
                "$systemPrefix\n\n$conversation\n".trim()
            !systemPrefix.isNullOrBlank() -> systemPrefix.trim()
            conversation.isNotBlank() -> "$conversation\n"
            else -> null
        }
        if (prefixText != null) {
            promptPrefix = PromptPrefix(prefixText)
        }

        temperature = 0.7f
        maxOutputTokens = 512
    }

    private suspend fun warmUpIfNeeded() {
        if (!isWarmedUp) {
            generativeModel.warmup()
            isWarmedUp = true
            Log.d(TAG, "Gemini Nano warmup complete for ${context.packageName}")
        }
    }

    private suspend fun resolveFeatureStatus(): Int {
        return generativeModel.checkStatus()
    }

    private fun generateDevFallbackResponse(prompt: String): Result<String> {
        val response = when {
            prompt.contains("greeting", ignoreCase = true) ||
                prompt.contains("feel", ignoreCase = true) ->
                "Hello! I'm running in developer fallback mode. How are you feeling today?"

            prompt.contains("workout", ignoreCase = true) ->
                "That sounds like a solid plan. For best results, log the session in Hevy when you're done."

            else ->
                "Developer fallback mode is active. On-device Gemini Nano was unavailable for this response."
        }
        return Result.Success(response)
    }

    companion object {
        private const val TAG = "GeminiNanoClient"
        private val READY_STATUSES = setOf(FeatureStatus.AVAILABLE, FeatureStatus.DOWNLOADABLE)
    }
}
