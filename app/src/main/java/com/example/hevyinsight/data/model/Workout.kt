package com.example.hevyinsight.data.model

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
    @SerializedName("name")
    val name: String?,
    @SerializedName("start_time")
    val startTime: String, // ISO 8601 format
    @SerializedName("end_time")
    val endTime: String?,
    @SerializedName("notes")
    val notes: String?,
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
            name = name,
            startTime = startTimestamp,
            endTime = endTimestamp,
            notes = notes,
            routineId = routineId,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }
    
    private fun parseTimestamp(isoString: String): Long {
        return try {
            // Parse ISO 8601 format (e.g., "2024-01-15T10:30:00Z")
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            dateFormat.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

/**
 * Paginated response wrapper for workouts
 */
data class PaginatedWorkoutResponse(
    @SerializedName("data")
    val data: List<WorkoutResponse>,
    @SerializedName("pagination")
    val pagination: Pagination?
)

data class Pagination(
    @SerializedName("page")
    val page: Int,
    @SerializedName("per_page")
    val perPage: Int,
    @SerializedName("total")
    val total: Int,
    @SerializedName("total_pages")
    val totalPages: Int
)

