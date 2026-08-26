package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.Workout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure streak math over workout history. Days are derived in the system time zone
 * and multiple workouts on one day collapse to a single date.
 */
object StreakCalculator {

    private const val DEFAULT_MAX_DISPLAY_DAYS = 7

    /**
     * Consecutive-day run ending today or yesterday; 0 when the most recent workout
     * is older than yesterday.
     */
    fun currentStreak(workouts: List<Workout>): Int {
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

    /** Longest consecutive-day run anywhere in the workout history. */
    fun longestStreak(workouts: List<Workout>): Int {
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
     * Fraction (0..1) of a display bar filled by [streak], capped at [maxDisplayDays].
     * Used for the dashboard streak indicator.
     */
    fun barProgress(streak: Int, maxDisplayDays: Int = DEFAULT_MAX_DISPLAY_DAYS): Float =
        (streak.coerceAtMost(maxDisplayDays) / maxDisplayDays.toFloat()).coerceIn(0f, 1f)
}
