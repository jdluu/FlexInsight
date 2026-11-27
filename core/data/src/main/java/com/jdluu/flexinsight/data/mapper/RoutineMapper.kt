package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.CreateRoutineBody
import com.jdluu.flexinsight.data.model.CreateRoutineExercise
import com.jdluu.flexinsight.data.model.CreateRoutineRequest
import com.jdluu.flexinsight.data.model.CreateRoutineSet
import com.jdluu.flexinsight.data.model.RoutineExerciseResponse
import com.jdluu.flexinsight.data.model.RoutineFolderResponse
import com.jdluu.flexinsight.data.model.RoutineResponse
import com.jdluu.flexinsight.domain.model.CreateRoutineDraft
import com.jdluu.flexinsight.domain.model.Routine
import com.jdluu.flexinsight.domain.model.RoutineExercise
import com.jdluu.flexinsight.domain.model.RoutineFolder

/**
 * Pure mapper for routine-related API responses and requests to/from domain models.
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
            name = response.title ?: exerciseName
        )
    }

    fun toRoutineFolder(response: RoutineFolderResponse): RoutineFolder {
        return RoutineFolder(
            id = response.id,
            title = response.title,
            index = response.index
        )
    }

    fun toCreateRoutineRequest(draft: CreateRoutineDraft): CreateRoutineRequest {
        return CreateRoutineRequest(
            routine = CreateRoutineBody(
                title = draft.title,
                folderId = draft.folderId,
                exercises = draft.exercises.map { ex ->
                    CreateRoutineExercise(
                        exerciseTemplateId = ex.exerciseTemplateId,
                        sets = ex.sets.map { set ->
                            CreateRoutineSet(
                                type = set.type,
                                weightKg = set.weightKg,
                                reps = set.reps
                            )
                        }
                    )
                }
            )
        )
    }
}
