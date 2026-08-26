package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.ExerciseTemplate
import com.jdluu.flexinsight.data.model.ExerciseTemplateResponse

/**
 * Pure mapper for exercise template API responses to domain models.
 * Mapping must stay identical to the previous inline implementations.
 */
object ExerciseMapper {

    fun toExerciseTemplate(response: ExerciseTemplateResponse): ExerciseTemplate {
        return ExerciseTemplate(
            id = response.id,
            name = response.title, // Map title to name for internal model
            muscleGroup = response.muscleGroup
        )
    }
}
