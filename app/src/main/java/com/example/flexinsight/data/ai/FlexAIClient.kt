package com.example.flexinsight.data.ai

import com.example.flexinsight.core.errors.Result

/**
 * Interface definition for the AI Client that abstracts the underlying ML implementation.
 * This allows swapping between on-device (Gemini Nano) and other implementations.
 */
interface FlexAIClient {
    /**
     * Checks if the on-device AI model is available and ready to use.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Generates a response for a chat interaction.
     * @param prompt The user's input or system prompt.
     * @param history Optional list of previous messages for context (format implementation dependent).
     */
    suspend fun generateResponse(prompt: String, history: List<Pair<String, String>> = emptyList()): Result<String>

    /**
     * specialized method for generating workout plans (to enforce structured prompts internaly)
     */
    suspend fun generateWorkoutPlan(prompt: String): Result<String>
}
