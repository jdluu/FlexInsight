package com.example.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * Routine exercise within a routine
 */
data class RoutineExercise(
    val templateId: String,
    val name: String? // Can be looked up from exercise templates
)

/**
 * Routine exercise API response
 */
data class RoutineExerciseResponse(
    @SerializedName("template_id")
    val templateId: String
) {
    fun toRoutineExercise(exerciseName: String? = null): RoutineExercise {
        return RoutineExercise(
            templateId = templateId,
            name = exerciseName
        )
    }
}

/**
 * Routine - Local representation
 */
data class Routine(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val exercises: List<RoutineExercise>?
)

/**
 * Routine API response model
 */
data class RoutineResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("exercise_count")
    val exerciseCount: Int,
    @SerializedName("exercises")
    val exercises: List<RoutineExerciseResponse>?
) {
    fun toRoutine(exerciseTemplateMapping: Map<String, String> = emptyMap()): Routine {
        val routineExercises = exercises?.map { exerciseResponse ->
            val exerciseName = exerciseTemplateMapping[exerciseResponse.templateId]
            exerciseResponse.toRoutineExercise(exerciseName)
        }
        
        return Routine(
            id = id,
            name = name,
            exerciseCount = exerciseCount,
            exercises = routineExercises
        )
    }
}

/**
 * Paginated response wrapper for routines
 * Actual API response structure: {"page":1,"page_count":5,"routines":[...]}
 */
data class PaginatedRoutineResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("routines")
    val routines: List<RoutineResponse>?
)

