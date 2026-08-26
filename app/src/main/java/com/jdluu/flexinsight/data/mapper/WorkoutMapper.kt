package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.ExerciseResponse
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.SetResponse
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.WorkoutResponse

/**
 * Pure mapper for workout-related API responses to domain models.
 * Mapping must stay identical to the previous inline implementations.
 */
object WorkoutMapper {

    fun toWorkout(response: WorkoutResponse): Workout {
        val startTimestamp = parseTimestamp(response.startTime)
        val endTimestamp = response.endTime?.let { parseTimestamp(it) }

        return Workout(
            id = response.id,
            name = response.title, // API uses "title", we store as "name"
            startTime = startTimestamp,
            endTime = endTimestamp,
            notes = response.description, // API uses "description", we store as "notes"
            routineId = response.routineId,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }

    fun toExercise(response: ExerciseResponse, workoutId: String): Exercise {
        // Generate ID from workout ID and index, or use a hash if index is null
        val exerciseId = if (response.index != null) {
            "${workoutId}_exercise_${response.index}"
        } else {
            "${workoutId}_exercise_${response.title.hashCode()}"
        }

        return Exercise(
            id = exerciseId,
            workoutId = workoutId,
            exerciseTemplateId = response.exerciseTemplateId,
            name = response.title, // API uses "title", we store as "name"
            notes = response.notes,
            restDuration = response.restSeconds, // API uses "rest_seconds", we store as "restDuration"
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }

    fun toSet(response: SetResponse, exerciseId: String): Set {
        // Generate ID from exercise ID and index
        val setId = "${exerciseId}_set_${response.index}"

        return Set(
            id = setId,
            exerciseId = exerciseId,
            number = response.index + 1, // Convert 0-based index to 1-based number
            weight = response.weightKg, // API uses "weight_kg", we store as "weight"
            reps = response.reps,
            rpe = response.rpe,
            distance = response.distanceMeters, // API uses "distance_meters", we store as "distance"
            duration = response.durationSeconds, // API uses "duration_seconds", we store as "duration"
            restDuration = null, // Not provided in API response
            notes = response.type, // Store set type as notes for now
            isPersonalRecord = response.personalRecord ?: false,
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
