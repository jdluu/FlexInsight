package com.jdluu.flexinsight.domain.ai

/**
 * Source of AI prompt context built from user workout data.
 */
interface AiContextProvider {
    /**
     * Builds the system prompt context snapshot.
     *
     * @param userQuery When set, triggers live Hevy API exercise-history lookups for relevant lifts.
     */
    suspend fun buildContext(userQuery: String? = null): AiContextSnapshot
}
