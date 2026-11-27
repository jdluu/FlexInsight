package com.jdluu.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * API response model for Workout
 */
data class WorkoutResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("routine_id")
    val routineId: String?,
    @SerializedName("exercises")
    val exercises: List<ExerciseResponse>?
)

/**
 * Paginated response wrapper for workouts
 */
data class PaginatedWorkoutResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("workouts")
    val workouts: List<WorkoutResponse>?
)

/**
 * Response for workout events endpoint
 */
data class PaginatedWorkoutEventsResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("events")
    val events: List<WorkoutEvent>?
)

/**
 * Individual workout event (created, updated, or deleted)
 */
data class WorkoutEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("workout_id")
    val workoutId: String?,
    @SerializedName("workout")
    val workout: WorkoutResponse? = null
)
