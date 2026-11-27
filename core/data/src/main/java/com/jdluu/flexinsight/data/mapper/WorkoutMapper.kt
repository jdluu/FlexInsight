package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.ExerciseResponse
import com.jdluu.flexinsight.data.model.SetResponse
import com.jdluu.flexinsight.data.model.WorkoutResponse
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout

/**
 * Pure mapper for workout-related API responses to domain models.
 */
object WorkoutMapper {

    fun toWorkout(response: WorkoutResponse): Workout {
        val startTimestamp = parseTimestamp(response.startTime)
        val endTimestamp = response.endTime?.let { parseTimestamp(it) }

        return Workout(
            id = response.id,
            name = response.title,
            startTime = startTimestamp,
            endTime = endTimestamp,
            notes = response.description,
            routineId = response.routineId,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }

    fun toExercise(response: ExerciseResponse, workoutId: String): Exercise {
        val exerciseId = if (response.index != null) {
            "${workoutId}_exercise_${response.index}"
        } else {
            "${workoutId}_exercise_${response.title.hashCode()}"
        }

        return Exercise(
            id = exerciseId,
            workoutId = workoutId,
            exerciseTemplateId = response.exerciseTemplateId,
            name = response.title,
            notes = response.notes,
            restDuration = response.restSeconds,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }

    fun toSet(response: SetResponse, exerciseId: String): Set {
        val setId = "${exerciseId}_set_${response.index}"

        return Set(
            id = setId,
            exerciseId = exerciseId,
            number = response.index + 1,
            weight = response.weightKg,
            reps = response.reps,
            rpe = response.rpe,
            distance = response.distanceMeters,
            duration = response.durationSeconds,
            restDuration = null,
            notes = response.type,
            isPersonalRecord = response.personalRecord ?: false,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }

    private fun parseTimestamp(isoString: String): Long {
        return try {
            val hasTimezoneOffset = isoString.contains("+") ||
                (isoString.length > 19 && isoString.substring(19).contains("-"))

            if (hasTimezoneOffset) {
                val cleanString = isoString.replace(Regex("[+-]\\d{2}:\\d{2}$"), "Z")
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                dateFormat.parse(cleanString)?.time ?: System.currentTimeMillis()
            } else {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                dateFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            try {
                java.time.Instant.parse(isoString).toEpochMilli()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
