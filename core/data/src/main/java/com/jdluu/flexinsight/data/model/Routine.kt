package com.jdluu.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * Routine exercise API response
 */
data class RoutineExerciseResponse(
    @SerializedName("exercise_template_id")
    val templateId: String,
    @SerializedName("title")
    val title: String?
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
)

/**
 * Paginated response wrapper for routines
 */
data class PaginatedRoutineResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("routines")
    val routines: List<RoutineResponse>?
)
