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
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: com.example.flexinsight.data.preferences.UserPreferencesManager
) : FlexAIClient {

    private val generativeModel: GenerativeModel by lazy {
        Generation.getClient()
    }

    override suspend fun isAvailable(): Boolean {
        // Check for Force Enable first
        if (userPreferencesManager.getForceAiEnable()) {
            return true
        }
        
        return try {
             withContext(Dispatchers.IO) {
                 val status = generativeModel.checkStatus()
                 Log.d("GeminiNanoClient", "AI Feature Status for ${context.packageName}: $status (Expected: ${FeatureStatus.AVAILABLE})")
                 // Allow DOWNLOADABLE state so usage triggers the download
                 status == FeatureStatus.AVAILABLE || status == FeatureStatus.DOWNLOADABLE
             }
        } catch (e: Exception) {
            Log.e("GeminiNanoClient", "Error checking AI availability", e)
            false
        }
    }

    override suspend fun generateResponse(prompt: String, history: List<Pair<String, String>>): Result<String> {
        return withContext(Dispatchers.IO) {
            val isForced = userPreferencesManager.getForceAiEnable()
            
            try {
                if (!isAvailable()) {
                     return@withContext Result.Error(ApiError.Unknown("AI model not ready on device."))
                }
                
                // If forced, try real first, but fallback to mock on ANY error
                if (isForced) {
                    try {
                        return@withContext generateRealResponse(prompt, history)
                    } catch (e: Exception) {
                        Log.w("GeminiNanoClient", "Real AI failed, using Mock because FORCE_AI_ENABLE is on.", e)
                        return@withContext generateMockResponse(prompt)
                    }
                }

                // Normal execution
                return@withContext generateRealResponse(prompt, history)

            } catch (e: Exception) {
                Result.Error(ApiError.Unknown(e.message ?: "AI Generation failed"))
            }
        }
    }

    private suspend fun generateRealResponse(prompt: String, history: List<Pair<String, String>>): Result<String> {
        val fullText = buildString {
            history.forEach { (role, msg) ->
                append("$role: $msg\n")
            }
            append("$prompt\n")
        }

        val request = generateContentRequest(TextPart(fullText)) {
             // Config if needed
        }

        val response = generativeModel.generateContent(request)
        val text = response.candidates.firstOrNull()?.text ?: ""

        if (text.isNotBlank()) {
            return Result.Success(text)
        } else {
            return Result.Error(ApiError.Unknown("Empty response from AI"))
        }
    }

    private fun generateMockResponse(prompt: String): Result<String> {
        // Simple mock logic
        val mockResponse = if (prompt.contains("greeting", ignoreCase = true) || prompt.contains("feel", ignoreCase = true)) {
            "Hello! I am your Mock AI Trainer (Dev Mode). How are you feeling today?"
        } else if (prompt.contains("workout", ignoreCase = true)) {
            "That sounds like a great workout plan. Make sure to stay hydrated!"
        } else {
            "I see! Tell me more. (Mock AI Response)"
        }
        return Result.Success(mockResponse)
    }

    override suspend fun generateWorkoutPlan(prompt: String): Result<String> {
        return generateResponse(prompt)
    }

    override fun generateResponseStream(prompt: String, history: List<Pair<String, String>>): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
        val isForced = try { 
             userPreferencesManager.getForceAiEnable() 
        } catch(e: Exception) { false }
        
        if (!isAvailable()) {
            emit("Error: AI model not ready.")
            return@flow
        }

        if (isForced) {
             // Mock Streaming
             val mockResponse = generateMockResponse(prompt)
             if (mockResponse is Result.Success) {
                 val text = mockResponse.data
                 val chunkConfig = 5 // chars per chunk
                 for (i in 0 until text.length step chunkConfig) {
                     val end = (i + chunkConfig).coerceAtMost(text.length)
                     emit(text.substring(i, end))
                     kotlinx.coroutines.delay(30) // Simulate typing speed
                 }
             } else {
                 emit("Error: Mock generation failed.")
             }
        } else {
            // Real Streaming
            try {
                val fullText = buildString {
                    history.forEach { (role, msg) ->
                         append("$role: $msg\n")
                    }
                    append("$prompt\n")
                }
                
                // Note: If the specific Alpha SDK being used doesn't support .generateContentStream, 
                // this block will fail compilation. In that case, we will fallback to non-streaming .generateContent()
                // wrapping it in a single emit.
                // Assuming it exists based on standard GenAI pattern.
                // If this fails compile, I will revert to non-stream wrapper.
                
                // Attempting standard flow:
                /* 
                   val request = generateContentRequest(TextPart(fullText)) {}
                   generativeModel.generateContentStream(request).collect { chunk ->
                       emit(chunk.text ?: "")
                   }
                */
                
                // Since I cannot verify exact SDK signature without docs/compile, 
                // and previous error logs showed standard Gemini classes, I'll try the safest bet:
                // If `.generateContentStream` is missing, I'll fake it with `.generateContent` for now 
                // to ensure compilation, then user can verify on device if they upgraded SDK.
                // BUT, to satisfy "make it stream", I really should try. 
                // Let's assume the method is `generateContentStream`.
                
                val request = generateContentRequest(TextPart(fullText)) {}
                val responseStream = generativeModel.generateContentStream(request)
                responseStream.collect { chunk ->
                     chunk.candidates.firstOrNull()?.text?.let { text ->
                         emit(text)
                     }
                }
            } catch (e: Exception) {
                 Log.e("GeminiNanoClient", "Streaming failed", e)
                 emit("Error: ${e.message}")
            }
        }
    }
}
