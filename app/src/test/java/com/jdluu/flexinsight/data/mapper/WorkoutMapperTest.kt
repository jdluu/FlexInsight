package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.ExerciseResponse
import com.jdluu.flexinsight.data.model.SetResponse
import com.jdluu.flexinsight.data.model.WorkoutResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMapperTest {

    // region toWorkout

    @Test
    fun toWorkout_mapsBasicFields() {
        val response = WorkoutResponse(
            id = "w1",
            title = "Push Day",
            startTime = "2024-01-15T10:30:00Z",
            endTime = "2024-01-15T11:30:00Z",
            description = "Felt strong",
            routineId = "r1",
            exercises = null
        )

        val workout = WorkoutMapper.toWorkout(response)

        assertEquals("w1", workout.id)
        assertEquals("Push Day", workout.name)
        assertEquals(1705314600000L, workout.startTime) // 2024-01-15T10:30:00Z
        assertEquals(1705318200000L, workout.endTime) // 2024-01-15T11:30:00Z
        assertEquals("Felt strong", workout.notes)
        assertEquals("r1", workout.routineId)
        assertFalse(workout.needsSync)
        assertFalse(workout.isDeleted)
        assertTrue(workout.lastSynced > 0)
    }

    @Test
    fun toWorkout_nullEndTimeMapsToNull() {
        val response = WorkoutResponse(
            id = "w1",
            title = null,
            startTime = "2024-01-15T10:30:00Z",
            endTime = null,
            description = null,
            routineId = null,
            exercises = null
        )

        val workout = WorkoutMapper.toWorkout(response)

        assertNull(workout.endTime)
        assertNull(workout.name)
        assertNull(workout.notes)
        assertNull(workout.routineId)
    }

    @Test
    fun toWorkout_parsesIsoWithZeroOffset() {
        val response = WorkoutResponse(
            id = "w2",
            title = null,
            startTime = "2025-12-12T18:27:13+00:00",
            endTime = null,
            description = null,
            routineId = null,
            exercises = null
        )

        val workout = WorkoutMapper.toWorkout(response)

        // Offset is normalized away; time is interpreted as UTC.
        assertEquals(1765564033000L, workout.startTime)
    }

    @Test
    fun toWorkout_parsesIsoWithNonZeroOffsetAsUtcWallClock() {
        val response = WorkoutResponse(
            id = "w3",
            title = null,
            startTime = "2025-12-12T18:27:13+05:30",
            endTime = null,
            description = null,
            routineId = null,
            exercises = null
        )

        val workout = WorkoutMapper.toWorkout(response)

        // Existing behavior: the numeric offset is stripped, wall clock read as UTC.
        assertEquals(1765564033000L, workout.startTime)
    }

    @Test
    fun toWorkout_unparseableTimestampFallsBackToCurrentTime() {
        val before = System.currentTimeMillis()
        val response = WorkoutResponse(
            id = "w4",
            title = null,
            startTime = "not-a-timestamp",
            endTime = null,
            description = null,
            routineId = null,
            exercises = null
        )

        val workout = WorkoutMapper.toWorkout(response)

        val after = System.currentTimeMillis()
        assertTrue(workout.startTime in before..after)
    }

    // endregion

    // region toExercise

    @Test
    fun toExercise_buildsIdFromIndexAndMapsFields() {
        val response = ExerciseResponse(
            index = 0,
            title = "Bench Press",
            exerciseTemplateId = "tpl-1",
            notes = "pause at bottom",
            restSeconds = 90,
            sets = null
        )

        val exercise = WorkoutMapper.toExercise(response, workoutId = "w1")

        assertEquals("w1_exercise_0", exercise.id)
        assertEquals("w1", exercise.workoutId)
        assertEquals("tpl-1", exercise.exerciseTemplateId)
        assertEquals("Bench Press", exercise.name)
        assertEquals("pause at bottom", exercise.notes)
        assertEquals(90, exercise.restDuration)
        assertFalse(exercise.needsSync)
        assertTrue(exercise.lastSynced > 0)
    }

    @Test
    fun toExercise_nullIndexUsesTitleHashInId() {
        val response = ExerciseResponse(
            index = null,
            title = "Squat",
            exerciseTemplateId = null,
            notes = null,
            restSeconds = null,
            sets = null
        )

        val exercise = WorkoutMapper.toExercise(response, workoutId = "w9")

        assertEquals("w9_exercise_${"Squat".hashCode()}", exercise.id)
        assertNull(exercise.exerciseTemplateId)
        assertNull(exercise.restDuration)
    }

    // endregion

    // region toSet

    @Test
    fun toSet_mapsAllFieldsAndBuildsIdFromIndex() {
        val response = SetResponse(
            index = 2,
            type = "warmup",
            weightKg = 62.5,
            reps = 8,
            rpe = 7.5,
            distanceMeters = 100.5,
            durationSeconds = 60,
            customMetric = null,
            personalRecord = true
        )

        val set = WorkoutMapper.toSet(response, exerciseId = "w1_exercise_0")

        assertEquals("w1_exercise_0_set_2", set.id)
        assertEquals("w1_exercise_0", set.exerciseId)
        assertEquals(3, set.number) // 0-based API index becomes 1-based number
        assertEquals(62.5, set.weight!!, 0.0)
        assertEquals(8, set.reps)
        assertEquals(7.5, set.rpe!!, 0.0)
        assertEquals(100.5, set.distance!!, 0.0)
        assertEquals(60, set.duration)
        assertNull(set.restDuration) // Not provided by API
        assertEquals("warmup", set.notes) // Set type stored as notes
        assertTrue(set.isPersonalRecord)
        assertFalse(set.needsSync)
        assertTrue(set.lastSynced > 0)
    }

    @Test
    fun toSet_nullPersonalRecordDefaultsToFalse() {
        val response = SetResponse(
            index = 0,
            type = null,
            weightKg = null,
            reps = null,
            rpe = null,
            distanceMeters = null,
            durationSeconds = null,
            customMetric = null,
            personalRecord = null
        )

        val set = WorkoutMapper.toSet(response, exerciseId = "e1")

        assertFalse(set.isPersonalRecord)
        assertNull(set.weight)
        assertNull(set.reps)
        assertNull(set.rpe)
        assertNull(set.distance)
        assertNull(set.duration)
        assertNull(set.notes)
    }

    // endregion
}
