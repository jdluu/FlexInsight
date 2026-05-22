package com.jdluu.flexinsight.domain.util

import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StatsCalculatorTest {

    private fun createWorkout(id: String, startTime: Long, endTime: Long?): Workout {
        return Workout(
            id = id,
            name = "Workout $id",
            startTime = startTime,
            endTime = endTime,
            notes = null,
            routineId = null
        )
    }

    private fun createExercise(id: String, workoutId: String): Exercise {
        return Exercise(
            id = id,
            workoutId = workoutId,
            exerciseTemplateId = null,
            name = "Exercise $id",
            notes = null,
            restDuration = null
        )
    }

    private fun createSet(id: String, exerciseId: String, weight: Double?, reps: Int?): Set {
        return Set(
            id = id,
            exerciseId = exerciseId,
            number = 1,
            weight = weight,
            reps = reps,
            rpe = null,
            distance = null,
            duration = null,
            restDuration = null,
            notes = null,
            isPersonalRecord = false
        )
    }

    @Test
    fun calculateTotalVolume_correctlySumsVolume() {
        val workout = createWorkout("w1", 0, 0)
        val exercise = createExercise("e1", "w1")
        val set1 = createSet("s1", "e1", 100.0, 10) // 1000
        val set2 = createSet("s2", "e1", 50.0, 10)  // 500
        
        val volume = StatsCalculator.calculateTotalVolume(
            listOf(workout),
            listOf(exercise),
            listOf(set1, set2)
        )
        
        assertEquals(1500.0, volume, 0.01)
    }

    @Test
    fun calculateStreak_detectsSequence() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        
        // Workout today, yesterday, 2 days ago
        val w1 = createWorkout("w1", now, now + 1000)
        val w2 = createWorkout("w2", now - dayMillis, now - dayMillis + 1000)
        val w3 = createWorkout("w3", now - 2 * dayMillis, now - 2 * dayMillis + 1000)
        
        val streak = StatsCalculator.calculateStreak(listOf(w1, w2, w3))
        assertEquals(3, streak)
    }

    @Test
    fun calculateStreak_breaksOnMissedDay() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        
        // Workout today, SKIP yesterday, 2 days ago
        val w1 = createWorkout("w1", now, now + 1000)
        val w3 = createWorkout("w3", now - 2 * dayMillis, now - 2 * dayMillis + 1000)
        
        val streak = StatsCalculator.calculateStreak(listOf(w1, w3))
        assertEquals(1, streak)
    }
    
    @Test
    fun calculateLongestStreak_findsLongest() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        
        // 3 days streak ... gap ... 5 days streak
        val gap = 10 * dayMillis
        
        val sequence1 = (0..2).map { i ->
             createWorkout("s1_$i", now - i * dayMillis, now - i * dayMillis + 1000)
        }
        
        val sequence2 = (0..4).map { i ->
             createWorkout("s2_$i", now - gap - i * dayMillis, now - gap - i * dayMillis + 1000)
        }
        
        val allWorkouts = sequence1 + sequence2
        val longest = StatsCalculator.calculateLongestStreak(allWorkouts)
        
        assertEquals(5, longest)
    }
    
    @Test
    fun getStartOfDay_correctTimestamp() {
        val now = System.currentTimeMillis()
        val startOfDay = StatsCalculator.getStartOfDay(now)
        
        val localDate = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val expected = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expected, startOfDay)
    }
}
