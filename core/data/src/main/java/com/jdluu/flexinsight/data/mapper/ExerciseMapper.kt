package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.ExerciseHistoryResponse
import com.jdluu.flexinsight.data.model.ExerciseTemplateResponse
import com.jdluu.flexinsight.domain.model.ExerciseHistory
import com.jdluu.flexinsight.domain.model.ExerciseHistoryEntry
import com.jdluu.flexinsight.domain.model.ExerciseHistorySet
import com.jdluu.flexinsight.domain.model.ExerciseTemplate

/**
 * Pure mapper for exercise template and history API responses to domain models.
 */
object ExerciseMapper {

    fun toExerciseTemplate(response: ExerciseTemplateResponse): ExerciseTemplate {
        return ExerciseTemplate(
            id = response.id,
            name = response.title,
            muscleGroup = response.muscleGroup
        )
    }

    fun toExerciseHistory(response: ExerciseHistoryResponse): ExerciseHistory {
        return ExerciseHistory(
            exerciseId = response.exerciseId,
            history = response.history.map { entry ->
                ExerciseHistoryEntry(
                    id = entry.id,
                    workoutId = entry.workoutId,
                    exerciseTemplateId = entry.exerciseTemplateId,
                    name = entry.name,
                    date = entry.date,
                    sets = entry.sets?.map { set ->
                        ExerciseHistorySet(
                            weightKg = set.weightKg,
                            reps = set.reps
                        )
                    },
                    volume = entry.volume,
                    oneRepMax = entry.oneRepMax
                )
            }
        )
    }
}
