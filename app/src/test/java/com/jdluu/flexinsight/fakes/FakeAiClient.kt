package com.jdluu.flexinsight.fakes

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.AiFeatureStatus
import com.jdluu.flexinsight.data.ai.FlexAIClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Scriptable [FlexAIClient] double for AI Trainer view-model tests.
 * Unlike [FakeFlexAIClient], supports streaming chunks and configurable readiness.
 */
class FakeAiClient : FlexAIClient {

    var featureStatus: AiFeatureStatus = AiFeatureStatus.Ready
    var prepareResult: Result<Unit> = Result.Success(Unit)
    var generateResponseResult: Result<String> = Result.Success("Hello!")

    /** Chunks emitted by [generateResponseStream]; a chunk starting with "Error:" mimics failures. */
    var streamChunks: List<String> = listOf("Hi", " there")

    /** When set, the stream throws instead of emitting chunks (exercises VM catch block). */
    var streamError: Exception? = null

    val prompts = mutableListOf<String>()
    val streamPrompts = mutableListOf<String>()
    val histories = mutableListOf<List<Pair<String, String>>>()

    override suspend fun isAvailable(): Boolean =
        featureStatus == AiFeatureStatus.Ready || featureStatus == AiFeatureStatus.Downloadable

    override suspend fun getFeatureStatus(): AiFeatureStatus = featureStatus

    override suspend fun prepareModel(): Result<Unit> = prepareResult

    override suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>>
    ): Result<String> {
        prompts += prompt
        histories += history
        return generateResponseResult
    }

    override suspend fun generateWorkoutPlan(prompt: String): Result<String> = generateResponseResult

    override fun generateResponseStream(
        prompt: String,
        history: List<Pair<String, String>>
    ): Flow<String> = flow {
        streamPrompts += prompt
        histories += history
        streamError?.let { throw it }
        streamChunks.forEach { emit(it) }
    }
}
