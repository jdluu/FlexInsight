package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import javax.inject.Inject

/**
 * Builds the system prompt context for AI coaching from Hevy data.
 *
 * @param userQuery When set, triggers live Hevy API exercise-history lookups for relevant lifts.
 */
class BuildAiContextUseCase @Inject constructor(
    private val aiContextProvider: AiContextProvider
) {
    suspend operator fun invoke(userQuery: String? = null): HevyAiDataAccessor.ContextSnapshot {
        return aiContextProvider.buildContext(userQuery)
    }
}
