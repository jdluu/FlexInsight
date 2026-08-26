package com.jdluu.flexinsight.data.model

import androidx.compose.runtime.Stable
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
    @SerializedName("exercise_template_id")
    val templateId: String,
    @SerializedName("title")
    val title: String?
)

/**
 * Routine - Local representation
 */
@Stable
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
)

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

