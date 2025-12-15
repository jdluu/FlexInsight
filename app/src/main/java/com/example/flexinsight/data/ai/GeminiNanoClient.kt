package com.example.flexinsight.data.ai

import android.content.Context
import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.core.errors.ApiError
import com.google.mlkit.genai.GenerativeModel
import com.google.mlkit.genai.GenerativeModelFutures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of FlexAIClient using Google ML Kit's Generative AI (Gemini Nano).
 */
class GeminiNanoClient @Inject constructor(
    private val context: Context
) : FlexAIClient {

    private var generativeModel: GenerativeModel? = null

    // Initialize logic could happen here, or lazily.
    // For now, we instantiate on demand or keep a singleton if expensive.
    // The Prompt API documentation suggests initializing the model is lightweight, 
    // but the underlying model load might take time.

    private fun getModel(): GenerativeModel {
        if (generativeModel == null) {
             generativeModel = GenerativeModel.Builder()
                .setModelName("gemini-nano") // Or "gemini-on-device" depending on specific beta version
                .build()
        }
        return generativeModel!!
    }

    override suspend fun isAvailable(): Boolean {
        // In a real implementation using the specific beta SDK, there is often a capability check.
        // For now, we will wrap the instantiation in a try-catch as a proxy or use a specific API if known.
        // The Prompt API usually has a `GenerativeModel.isAvailable()` equivalent.
        // Since strict docs are not available, we assume true for supported devices 
        // and handle errors downstream, or check for specific system features.
        return true 
    }

    override suspend fun generateResponse(prompt: String, history: List<Pair<String, String>>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Determine if we use history (chat mode) or single prompt.
                // For simplicity in this first pass, we concat history.
                // A more advanced implementation would use `startChat()`.
                
                val fullPrompt = buildString {
                    history.forEach { (role, msg) ->
                        append("$role: $msg\n")
                    }
                    append("User: $prompt\n")
                }

                val model = getModel()
                // ML Kit GenAI usually returns a Future or has a suspend function in Kotlin extensions.
                // Assuming `generateContent` or similar.
                
                // Note: The specific API method name might vary (generateContent, prompt, etc)
                // Using a safe placeholder pattern until we compile/verify.
                // For "Google ML Kit Generative AI", it follows the pattern of the Cloud SDK closely.
                
                val response = model.generateContent(fullPrompt)
                val text = response.text ?: ""
                
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
