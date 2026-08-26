package com.jdluu.flexinsight.domain.ai

import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor

/**
 * Source of AI prompt context built from user workout data.
 *
 * Extracted from [HevyAiDataAccessor] so domain code depends on an abstraction
 * rather than the Hevy-specific implementation.
 */
interface AiContextProvider {
    /**
     * Builds the system prompt context snapshot.
     *
     * @param userQuery When set, triggers live Hevy API exercise-history lookups for relevant lifts.
     */
    suspend fun buildContext(userQuery: String? = null): HevyAiDataAccessor.ContextSnapshot
}
