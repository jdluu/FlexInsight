@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.flexinsight.data.ai


import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.core.errors.ApiError
// Correct ML Kit Prompt API imports
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.common.FeatureStatus  // Common status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import javax.inject.Inject

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Implementation of FlexAIClient using Google ML Kit's Prompt API (Gemini Nano).
 */
class GeminiNanoClient @Inject constructor(
    @ApplicationContext private val context: Context
) : FlexAIClient {

    private val generativeModel: GenerativeModel by lazy {
        // Usage based on errors: getClient(Context) failed expecting GenerationConfig.
        // Reverting to getClient() no-arg as per GitHub sample and previous successful compile.
        Generation.getClient()
    }

    override suspend fun isAvailable(): Boolean {
        return try {
             withContext(Dispatchers.IO) {
                 // OpenPromptActivity source suggests strict usage inside coroutines/futures builders.
                 // Assuming they are suspend functions based on previous errors with futures.
                 val status = generativeModel.checkStatus()
                 Log.d("GeminiNanoClient", "AI Feature Status for ${context.packageName}: $status (Expected: ${FeatureStatus.AVAILABLE})")
                 status == FeatureStatus.AVAILABLE
             }
        } catch (e: Exception) {
            Log.e("GeminiNanoClient", "Error checking AI availability", e)
            false
        }
    }

    override suspend fun generateResponse(prompt: String, history: List<Pair<String, String>>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isAvailable()) {
                     // Fallback error since ServiceUnavailable might be missing
                     return@withContext Result.Error(ApiError.Unknown("AI model not ready on device."))
                }
                
                val fullText = buildString {
                    history.forEach { (role, msg) ->
                        append("$role: $msg\n")
                    }
                    append("$prompt\n")
                }

                val request = generateContentRequest(TextPart(fullText)) {
                     // Config if needed
                }

                // generateContent returns GenerateContentResponse directly or ListenableFuture.
                // Based on sample: checkNotNull(generativeModel).generateContent(genRequest).candidates
                // Sample wrapped it in runBlocking/future, suggesting it might be blocking or future.
                // If it IS a Future, .get() will solve it. 
                // If it is blocking, .get() will fail compilation (no such method).
                // However, ML Kit normally returns Task or ListenableFuture.
                // I will try .get() first as it shares pattern with checkStatus.
                // If generateContent is blocking, the previous error 'await() unresolved' implies it wasn't a future? 
                // No, 'await()' unresolved usually means the extension wasn't found for ListenableFuture.
                // So .get() is safest assumption.
                val response = generativeModel.generateContent(request)
                val text = response.candidates.firstOrNull()?.text ?: ""

                if (text.isNotBlank()) {
                    Result.Success(text)
                } else {
                    Result.Error(ApiError.Unknown("Empty response from AI"))
                }
            } catch (e: Exception) {
                Result.Error(ApiError.Unknown(e.message ?: "AI Generation failed"))
            }
        }
    }

    override suspend fun generateWorkoutPlan(prompt: String): Result<String> {
        return generateResponse(prompt)
    }
}
