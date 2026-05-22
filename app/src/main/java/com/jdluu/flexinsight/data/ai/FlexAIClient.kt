package com.jdluu.flexinsight.data.ai

import com.jdluu.flexinsight.core.errors.Result
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for on-device AI (Gemini Nano via ML Kit GenAI Prompt API / AICore).
 */
interface FlexAIClient {
    suspend fun isAvailable(): Boolean

    /** Detailed readiness for UI messaging (download prompts, unsupported device, etc.). */
    suspend fun getFeatureStatus(): AiFeatureStatus

    /** Ensures the model is downloaded and warmed up before inference. */
    suspend fun prepareModel(): Result<Unit>

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): Result<String>

    suspend fun generateWorkoutPlan(prompt: String): Result<String>

    fun generateResponseStream(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): Flow<String>
}
