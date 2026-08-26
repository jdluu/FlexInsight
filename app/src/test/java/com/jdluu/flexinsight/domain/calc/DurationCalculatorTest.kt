package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.Workout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class DurationCalculatorTest {

    private val originalTimeZone = TimeZone.getDefault()

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun pinUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private fun zone(): ZoneId = ZoneId.systemDefault()

    private fun millis(date: LocalDate, hour: Int = 10): Long =
        date.atTime(hour, 0).atZone(zone()).toInstant().toEpochMilli()

    private fun workout(
        id: String,
        startTime: Long,
        endTime: Long? = startTime + 3_600_000L
    ) = Workout(
        id = id,
        name = "Workout $id",
        startTime = startTime,
        endTime = endTime,
        notes = null,
        routineId = null
    )

    // ---- workoutDurationMinutes ----

    @Test
    fun durationMinutes_openWorkoutIsZero() {
        assertEquals(0L, DurationCalculator.workoutDurationMinutes(workout("open", 0, endTime = null)))
    }

    @Test
    fun durationMinutes_truncatesPartialMinutes() {
        // 90 minutes and 59 seconds -> 90
        assertEquals(90L, DurationCalculator.workoutDurationMinutes(workout("w", 0, endTime = 90 * 60 * 1000L + 59_999L)))
    }

    // ---- totalDuration ----

    @Test
    fun totalDuration_emptyListIsZero() {
        assertEquals(0L, DurationCalculator.totalDuration(emptyList()))
    }

    @Test
    fun totalDuration_skipsWorkoutsWithoutEndTime() {
        val open = workout("open", 0, endTime = null)
        val closed = workout("closed", 0, endTime = 30 * 60 * 1000L)

        assertEquals(30L, DurationCalculator.totalDuration(listOf(open, closed)))
    }

    @Test
    fun totalDuration_sumsMultipleWorkouts() {
        val a = workout("a", 0, endTime = 45 * 60 * 1000L)
        val b = workout("b", 1000, endTime = 1000 + 75 * 60 * 1000L)

        assertEquals(120L, DurationCalculator.totalDuration(listOf(a, b)))
    }

    // ---- averageDurationMinutes ----

    @Test
    fun averageDurationMinutes_emptyListIsZero() {
        assertEquals(0L, DurationCalculator.averageDurationMinutes(emptyList()))
    }

    @Test
    fun averageDurationMinutes_allOpenWorkoutsIsZero() {
        val open = workout("open", 0, endTime = null)

        assertEquals(0L, DurationCalculator.averageDurationMinutes(listOf(open)))
    }

    @Test
    fun averageDurationMinutes_truncatesFractionalAverage() {
        val a = workout("a", 0, endTime = 61 * 60_000L)
        val b = workout("b", 1000, endTime = 1000 + 60 * 60_000L)

        // avg(61, 60) = 60.5 truncated to 60
        assertEquals(60L, DurationCalculator.averageDurationMinutes(listOf(a, b)))
    }

    // ---- durationLabel ----

    @Test
    fun durationLabel_nonPositiveRendersAsZero() {
        assertEquals("0m", DurationCalculator.durationLabel(0))
        assertEquals("0m", DurationCalculator.durationLabel(-5))
    }

    @Test
    fun durationLabel_underAnHourShowsMinutesOnly() {
        assertEquals("45m", DurationCalculator.durationLabel(45))
    }

    @Test
    fun durationLabel_exactHourStillShowsMinutesPart() {
        assertEquals("1h 0m", DurationCalculator.durationLabel(60))
        assertEquals("1h 30m", DurationCalculator.durationLabel(90))
    }

    // ---- accountAgeDays ----

    @Test
    fun accountAgeDays_truncatesPartialDays() {
        val twoAndAHalfDaysMs = (2 * 24 + 12) * 60 * 60 * 1000L

        assertEquals(2, DurationCalculator.accountAgeDays(0, twoAndAHalfDaysMs))
    }

    @Test
    fun accountAgeDays_futureMemberSinceClampsToZero() {
        assertEquals(0, DurationCalculator.accountAgeDays(memberSinceMs = 1000, nowMs = 0))
    }

    // ---- startOfDay / endOfDay ----

    @Test
    fun startOfDay_mapsToMidnightOfSameLocalDate() {
        pinUtc()
        val timestamp = java.time.Instant.parse("2026-06-15T14:30:45Z").toEpochMilli()
        val expected = LocalDate.of(2026, 6, 15).atStartOfDay(zone()).toInstant().toEpochMilli()

        assertEquals(expected, DurationCalculator.startOfDay(timestamp))
    }

    @Test
    fun endOfDay_isLastMillisecondOfSameLocalDate() {
        pinUtc()
        val timestamp = millis(LocalDate.of(2026, 6, 15), hour = 18)
        val start = DurationCalculator.startOfDay(timestamp)

        assertEquals(start + 86_399_999L, DurationCalculator.endOfDay(timestamp))
    }

    // ---- durationTrend ----

    @Test
    fun durationTrend_returnsSixEntriesMondayThroughSaturday() {
        val result = DurationCalculator.durationTrend(emptyList(), 0, Long.MAX_VALUE)

        assertEquals(listOf("M", "T", "W", "T", "F", "S"), result.map { it.dayOfWeek })
    }

    @Test
    fun durationTrend_sundayWorkoutsAreDroppedFromOutput() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)
        val sunday = LocalDate.of(2026, 1, 11)

        val result = DurationCalculator.durationTrend(
            listOf(workout("sun", millis(sunday), millis(sunday, hour = 11))),
            millis(monday),
            millis(sunday, hour = 23)
        )

        assertEquals((1..6).map { 0L }, result.map { it.averageDuration })
    }

    @Test
    fun durationTrend_averagesPerWeekdayAndTruncatesFractionalMinutes() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)
        val wednesday = LocalDate.of(2026, 1, 7)
        val workouts = listOf(
            workout("m1", millis(monday, hour = 9), millis(monday, hour = 9) + 61 * 60_000L),
            workout("m2", millis(monday, hour = 15), millis(monday, hour = 15) + 60 * 60_000L),
            workout("w1", millis(wednesday, hour = 12), millis(wednesday, hour = 13))
        )

        val result = DurationCalculator.durationTrend(
            workouts,
            millis(monday, hour = 0),
            millis(wednesday, hour = 23)
        )

        assertEquals(
            listOf(
                DailyDurationData("M", 60L),
                DailyDurationData("T", 0L),
                DailyDurationData("W", 60L),
                DailyDurationData("T", 0L),
                DailyDurationData("F", 0L),
                DailyDurationData("S", 0L)
            ),
            result
        )
    }

    @Test
    fun durationTrend_windowBoundsAreInclusive() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)
        val wednesday = LocalDate.of(2026, 1, 7)
        val workouts = listOf(
            workout("at-start", millis(monday, hour = 6), millis(monday, hour = 7)),
            workout("at-end", millis(wednesday, hour = 22), millis(wednesday, hour = 23)),
            workout("before-window", millis(monday.minusDays(1)), millis(monday.minusDays(1), hour = 12)),
            workout("after-window", millis(wednesday.plusDays(1)), millis(wednesday.plusDays(1), hour = 12))
        )

        val result = DurationCalculator.durationTrend(
            workouts,
            millis(monday, hour = 0),
            millis(wednesday, hour = 23)
        )

        assertEquals(60L, result[0].averageDuration)
        assertEquals(0L, result[1].averageDuration)
        assertEquals(60L, result[2].averageDuration)
    }

    @Test
    fun durationTrend_excludesWorkoutsMissingEndTime() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)

        val result = DurationCalculator.durationTrend(
            listOf(workout("open-ended", millis(monday), endTime = null)),
            millis(monday, hour = 0),
            millis(monday, hour = 23)
        )

        assertEquals(0L, result[0].averageDuration)
    }
}
