package com.example.flexinsight.data.model

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
data class Workout(
    @PrimaryKey
    val id: String,
    val name: String?,
    val startTime: Long, // Unix timestamp
    val endTime: Long?, // Unix timestamp
    val notes: String?,
    val routineId: String?,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
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
) {
    fun toWorkout(): Workout {
        val startTimestamp = parseTimestamp(startTime)
        val endTimestamp = endTime?.let { parseTimestamp(it) }

        return Workout(
            id = id,
            name = title, // API uses "title", we store as "name"
            startTime = startTimestamp,
            endTime = endTimestamp,
            notes = description, // API uses "description", we store as "notes"
            routineId = routineId,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }

    private fun parseTimestamp(isoString: String): Long {
        return try {
            // Parse ISO 8601 format (e.g., "2025-12-12T18:27:13+00:00" or "2024-01-15T10:30:00Z")
            // Check if string has timezone offset (contains "+" or has "-" after the date part)
            val hasTimezoneOffset = isoString.contains("+") ||
                (isoString.length > 19 && isoString.substring(19).contains("-"))

            if (hasTimezoneOffset) {
                // Format: "2025-12-12T18:27:13+00:00" - replace timezone with Z
                val cleanString = isoString.replace(Regex("[+-]\\d{2}:\\d{2}$"), "Z")
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                dateFormat.parse(cleanString)?.time ?: System.currentTimeMillis()
            } else {
                // Format: "2024-01-15T10:30:00Z"
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                dateFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Fallback: try parsing with java.time if available (Android API 26+)
            try {
                java.time.Instant.parse(isoString).toEpochMilli()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

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

