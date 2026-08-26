package com.jdluu.flexinsight.data.model

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Room entity representing a workout
 */
@Entity(
    tableName = "workouts",
    indices = [Index(value = ["startTime"]), Index(value = ["lastSynced"])]
)
@Stable
data class Workout(
    @PrimaryKey
    val id: String,
    val name: String?,
    val startTime: Long, // Unix timestamp
    val endTime: Long?, // Unix timestamp
    val notes: String?,
    val routineId: String?,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false,
    /** Soft-delete when Hevy reports a deleted event; hidden from UI but retained for audit. */
    val isDeleted: Boolean = false
)

/**
 * API response model for Workout
 */
data class WorkoutResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("start_time")
    val startTime: String, // ISO 8601 format
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
 * Actual API response structure: {"page":1,"page_count":12,"workouts":[...]}
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
 * Returns paginated list of workout events (created, updated, deleted) since a given date
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
 * Per API docs: events can be "created", "updated", or "deleted"
 */
data class WorkoutEvent(
    @SerializedName("type")
    val type: String, // "created", "updated", or "deleted"
    @SerializedName("workout_id")
    val workoutId: String?,
    @SerializedName("workout")
    val workout: WorkoutResponse? = null // Keeping just in case, but docs show workout_id
)

