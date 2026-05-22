package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import javax.inject.Inject

/**
 * Builds the system prompt context for AI coaching from Hevy data.
 *
 * @param userQuery When set, triggers live Hevy API exercise-history lookups for relevant lifts.
 */
class BuildAiContextUseCase @Inject constructor(
    private val hevyAiDataAccessor: HevyAiDataAccessor
) {
    suspend operator fun invoke(userQuery: String? = null): HevyAiDataAccessor.ContextSnapshot {
        return hevyAiDataAccessor.buildContext(userQuery)
    }
}
