package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure workout-duration math plus day-window helpers. All minute values truncate
 * partial minutes; day boundaries use the system time zone.
 */
object DurationCalculator {

    private const val MILLIS_PER_MINUTE = 1000L * 60
    private const val MILLIS_PER_DAY = MILLIS_PER_MINUTE * 60 * 24

    /** Duration of a single workout in whole minutes; 0 while endTime is null. */
    fun workoutDurationMinutes(workout: Workout): Long =
        workout.endTime?.let { (it - workout.startTime) / MILLIS_PER_MINUTE } ?: 0L

    /** Total duration in whole minutes across workouts, skipping open-ended ones. */
    fun totalDuration(workouts: List<Workout>): Long =
        workouts.filter { it.endTime != null }.sumOf { workoutDurationMinutes(it) }

    /** Mean duration in whole minutes; 0 for empty input or when nothing has an endTime. */
    fun averageDurationMinutes(workouts: List<Workout>): Long {
        if (workouts.isEmpty()) return 0L
        val durations = workouts.mapNotNull { w ->
            w.endTime?.let { (it - w.startTime) / MILLIS_PER_MINUTE }
        }
        return if (durations.isEmpty()) 0L else durations.average().toLong()
    }

    /** Human label such as "45m" or "1h 30m"; non-positive input renders as "0m". */
    fun durationLabel(minutes: Long): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val minutesPart = minutes % 60
        return if (hours > 0) "${hours}h ${minutesPart}m" else "${minutesPart}m"
    }

    /** Whole days elapsed since [memberSinceMs]; never negative. */
    fun accountAgeDays(memberSinceMs: Long, nowMs: Long): Int =
        ((nowMs - memberSinceMs) / MILLIS_PER_DAY).toInt().coerceAtLeast(0)

    /** First millisecond of the local day containing [timestamp]. */
    fun startOfDay(timestamp: Long): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /** Last millisecond of the local day containing [timestamp]. */
    fun endOfDay(timestamp: Long): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Average duration per weekday for workouts within [startDate, endDate] inclusive,
     * returned Monday through Saturday in display order. Workouts on Sunday are
     * aggregated internally but never emitted, matching historical output.
     */
    fun durationTrend(
        workouts: List<Workout>,
        startDate: Long,
        endDate: Long
    ): List<DailyDurationData> {
        val dayGroups = mutableMapOf<DayOfWeek, MutableList<Long>>()

        // Initialize map with empty lists
        DayOfWeek.values().forEach { day ->
            dayGroups[day] = mutableListOf()
        }

        workouts.forEach { workout ->
            if (workout.endTime != null && workout.startTime >= startDate && workout.startTime <= endDate) {
                val workoutDate = Instant.ofEpochMilli(workout.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val dayOfWeek = workoutDate.dayOfWeek
                val duration = workoutDurationMinutes(workout)

                dayGroups.getOrPut(dayOfWeek) { mutableListOf() }.add(duration)
            }
        }

        // Map to display order M, T, W, T, F, S
        val displayDays = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        )

        return displayDays.map { day ->
            val durations = dayGroups[day] ?: emptyList()
            val avg = if (durations.isNotEmpty()) durations.average().toLong() else 0L
            DailyDurationData(
                dayOfWeek = day.name.take(1), // M, T, W...
                averageDuration = avg
            )
        }
    }
}
