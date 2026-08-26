package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.Routine
import com.jdluu.flexinsight.data.model.RoutineExercise
import com.jdluu.flexinsight.data.model.RoutineExerciseResponse
import com.jdluu.flexinsight.data.model.RoutineFolder
import com.jdluu.flexinsight.data.model.RoutineFolderResponse
import com.jdluu.flexinsight.data.model.RoutineResponse

/**
 * Pure mapper for routine-related API responses to domain models.
 * Mapping must stay identical to the previous inline implementations.
 */
object RoutineMapper {

    fun toRoutine(
        response: RoutineResponse,
        exerciseTemplateMapping: Map<String, String> = emptyMap()
    ): Routine {
        val routineExercises = response.exercises?.map { exerciseResponse ->
            val exerciseName = exerciseTemplateMapping[exerciseResponse.templateId]
            toRoutineExercise(exerciseResponse, exerciseName)
        }

        return Routine(
            id = response.id,
            name = response.name,
            exerciseCount = response.exerciseCount,
            exercises = routineExercises
        )
    }

    fun toRoutineExercise(
        response: RoutineExerciseResponse,
        exerciseName: String? = null
    ): RoutineExercise {
        return RoutineExercise(
            templateId = response.templateId,
            name = response.title ?: exerciseName // Prefer API title, fallback to mapping
        )
    }

    fun toRoutineFolder(response: RoutineFolderResponse): RoutineFolder {
        return RoutineFolder(
            id = response.id,
            title = response.title,
            index = response.index
        )
    }
}
