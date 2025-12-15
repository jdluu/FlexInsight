package com.example.flexinsight.data.repository

import com.example.flexinsight.data.model.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Helper class for calculating workout statistics using java.time types.
 * Contains pure functions for improved testability.
 */
object StatsCalculator {

    /**
     * Calculate total volume from a list of workouts, exercises, and sets.
     */
    fun calculateTotalVolume(
        workouts: List<Workout>,
        allExercises: List<Exercise>,
        allSets: List<com.example.flexinsight.data.model.Set>
    ): Double {
        val exercisesByWorkout = allExercises.groupBy { it.workoutId }
        val setsByExercise = allSets.groupBy { it.exerciseId }

        return workouts.sumOf { workout ->
            val exercises = exercisesByWorkout[workout.id] ?: emptyList()
            exercises.sumOf { exercise: Exercise ->
                val sets = setsByExercise[exercise.id] ?: emptyList()
                sets.sumOf { set: com.example.flexinsight.data.model.Set ->
                    ((set.weight ?: 0.0) * (set.reps ?: 0)).toDouble()
                }
            }
        }
    }

    /**
     * Calculate total duration in minutes.
     */
    fun calculateTotalDuration(workouts: List<Workout>): Long {
        return workouts
            .filter { it.endTime != null }
            .sumOf { workout ->
                val endTime = workout.endTime ?: return@sumOf 0L
                (endTime - workout.startTime) / (1000 * 60)
            }
    }

    /**
     * Calculate current streak of consecutive days with workouts.
     */
    fun calculateStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0

        // Group by LocalDate to handle multiple workouts per day
        val workoutDates = workouts
            .map { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        if (workoutDates.isEmpty()) return 0

        // Streak must start today or yesterday
        val latestWorkout = workoutDates.first()
        if (latestWorkout != today && latestWorkout != yesterday) {
            return 0
        }

        var streak = 0
        var currentDate = latestWorkout

        // Check for consecutive days backwards
        for (i in workoutDates.indices) {
            if (workoutDates[i] == currentDate) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Calculate longest streak of consecutive days.
     */
    fun calculateLongestStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0

        val workoutDates = workouts
            .map { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()

        var longestStreak = 0
        var currentStreak = 0
        var lastDate: LocalDate? = null

        for (date in workoutDates) {
            if (lastDate == null) {
                currentStreak = 1
                lastDate = date
            } else {
                val daysDiff = ChronoUnit.DAYS.between(lastDate, date)
                if (daysDiff == 1L) {
                    currentStreak++
                } else {
                    longestStreak = maxOf(longestStreak, currentStreak)
                    currentStreak = 1
                }
                lastDate = date
            }
        }

        return maxOf(longestStreak, currentStreak)
    }

    /**
     * Calculate volume balance across muscle groups.
     */
    fun calculateVolumeBalance(muscleGroupProgress: List<MuscleGroupProgress>): VolumeBalance {
        val pushGroups = setOf("Chest", "Shoulders", "Triceps")
        val pullGroups = setOf("Back", "Biceps")
        val legsGroups = setOf("Legs", "Quads", "Hamstrings", "Glutes", "Calves")
        val cardioGroups = setOf("Cardio")

        var pushVolume = 0.0
        var pullVolume = 0.0
        var legsVolume = 0.0
        var cardioVolume = 0.0

        muscleGroupProgress.forEach { progress ->
            val group = progress.muscleGroup
            when {
                pushGroups.any { group.contains(it, ignoreCase = true) } -> pushVolume += progress.volume
                pullGroups.any { group.contains(it, ignoreCase = true) } -> pullVolume += progress.volume
                legsGroups.any { group.contains(it, ignoreCase = true) } -> legsVolume += progress.volume
                cardioGroups.any { group.contains(it, ignoreCase = true) } -> cardioVolume += progress.volume
            }
        }

        val totalVolume = pushVolume + pullVolume + legsVolume + cardioVolume

        return if (totalVolume > 0) {
            VolumeBalance(
                push = (pushVolume / totalVolume).toFloat(),
                pull = (pullVolume / totalVolume).toFloat(),
                legs = (legsVolume / totalVolume).toFloat(),
                cardio = (cardioVolume / totalVolume).toFloat()
            )
        } else {
            VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f)
        }
    }

    /**
     * Calculate daily duration trend.
     */
    fun calculateDurationTrend(
        workouts: List<Workout>,
        startDate: Long,
        endDate: Long
    ): List<DailyDurationData> {
        val dayGroups = mutableMapOf<DayOfWeek, MutableList<Long>>()

        // Initialize map with empty lists
        DayOfWeek.values().forEach { day ->
            if (day != DayOfWeek.SUNDAY) { // Exclude Sunday if 6-day week logic, or keep all
                 dayGroups[day] = mutableListOf()
            }
        }
        // Assuming we want Mon-Sun or Mon-Sat based on existing logic which used 0-6 index
        // Existing logic used: Mon(0) to Sat(5) + Sun(6)

        workouts.forEach { workout ->
             if (workout.endTime != null && workout.startTime >= startDate && workout.startTime <= endDate) {
                val workoutDate = Instant.ofEpochMilli(workout.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val dayOfWeek = workoutDate.dayOfWeek
                val duration = (workout.endTime - workout.startTime) / (1000 * 60)

                dayGroups.getOrPut(dayOfWeek) { mutableListOf() }.add(duration)
             }
        }

        // Map to display order M, T, W, T, F, S, S?
        // Original logic had explicit mapping. Let's return Mon-Sat(Sun)
        val displayDays = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        ) // Original code seemed to map only 6 days? Let's verify existing logic.
        // Existing: dayLabels = listOf("M", "T", "W", "T", "F", "S"). Size 6.
        // Sunday (6) was handled in switch but label list only has 6 items.

        return displayDays.map { day ->
            val durations = dayGroups[day] ?: emptyList()
            val avg = if (durations.isNotEmpty()) durations.average().toLong() else 0L
            DailyDurationData(
                dayOfWeek = day.name.take(1), // M, T, W...
                averageDuration = avg
            )
        }
    }

    /**
     * Get start of the day timestamp.
     */
    fun getStartOfDay(timestamp: Long): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Get end of the day timestamp.
     */
    /**
     * Get end of the day timestamp.
     */
    fun getEndOfDay(timestamp: Long): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
     }

    /**
     * Calculate volume percentage change.
     */
    fun calculateVolumeChange(currentVolume: Double, previousVolume: Double): Double {
        return if (previousVolume > 0) {
            ((currentVolume - previousVolume) / previousVolume) * 100.0
        } else {
            if (currentVolume > 0) 100.0 else 0.0
        }
    }

    /**
     * Calculate weekly goal status.
     */
    fun calculateGoalStatus(completed: Int, target: Int): String {
        return when {
            completed >= target -> "On Track"
            completed >= target * 0.7 -> "On Track"
            completed >= target * 0.5 -> "Behind"
            else -> "Behind"
        }
    }

    /**
     * Calculate relative intensity (HI/MD/LO) based on average volume.
     */
    fun calculateRelativeIntensity(volume: Double, averageVolume: Double): String {
        return when {
            volume >= averageVolume * 1.5 -> "HI"
            volume >= averageVolume * 0.7 -> "MD"
            else -> "LO"
        }
    }

    /**
     * Calculate absolute intensity based on total volume.
     */
    fun calculateAbsoluteIntensity(totalVolume: Double): String {
        return when {
            totalVolume > 5000 -> "High Intensity"
            totalVolume > 2000 -> "Medium Intensity"
            else -> "Aerobic"
        }
    }
}
