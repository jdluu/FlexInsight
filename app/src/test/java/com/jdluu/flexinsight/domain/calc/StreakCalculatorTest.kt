package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.Workout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class StreakCalculatorTest {

    private val originalTimeZone = TimeZone.getDefault()

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun pinUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private fun millis(date: LocalDate, hour: Int = 10): Long =
        date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun workout(id: String, startTime: Long) = Workout(
        id = id,
        name = "Workout $id",
        startTime = startTime,
        endTime = startTime + 3_600_000L,
        notes = null,
        routineId = null
    )

    // ---- currentStreak ----

    @Test
    fun streak_emptyHistoryIsZero() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList()))
    }

    @Test
    fun streak_staleLatestDayBreaksStreakImmediately() {
        pinUtc()
        val old = LocalDate.now().minusDays(5)

        assertEquals(0, StreakCalculator.currentStreak(listOf(workout("old", millis(old)))))
    }

    @Test
    fun streak_countsBackThroughConsecutiveDaysFromToday() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today, today.minusDays(1), today.minusDays(2))

        assertEquals(3, StreakCalculator.currentStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun streak_startedYesterdayStillCountsWhenTodayHasNoWorkout() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today.minusDays(1), today.minusDays(2))

        assertEquals(2, StreakCalculator.currentStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun streak_multipleSessionsPerDayCollapseToOneDate() {
        pinUtc()
        val today = LocalDate.now()
        val sameDayTwice = listOf(
            workout("am", millis(today, hour = 8)),
            workout("pm", millis(today, hour = 20))
        )

        assertEquals(1, StreakCalculator.currentStreak(sameDayTwice))
    }

    @Test
    fun streak_missedDayTruncatesRun() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4))

        assertEquals(2, StreakCalculator.currentStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun streak_handlesUnsortedInput() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today.minusDays(2), today, today.minusDays(1))

        assertEquals(3, StreakCalculator.currentStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    // ---- longestStreak ----

    @Test
    fun longestStreak_emptyHistoryIsZero() {
        assertEquals(0, StreakCalculator.longestStreak(emptyList()))
    }

    @Test
    fun longestStreak_singleWorkoutIsOne() {
        assertEquals(1, StreakCalculator.longestStreak(listOf(workout("w", 0))))
    }

    @Test
    fun longestStreak_findsLongestRunAnywhereInHistory() {
        pinUtc()
        val base = LocalDate.of(2026, 1, 5)
        val run1 = (0..2).map { base.plusDays(it.toLong()) }          // 3-day run
        val run2 = (0..4).map { base.plusDays(10 + it.toLong()) }     // 5-day run
        val dates = run1 + run2

        assertEquals(5, StreakCalculator.longestStreak(dates.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun longestStreak_fullyConsecutiveHistoryReturnsEntireLength() {
        pinUtc()
        val base = LocalDate.of(2026, 3, 1)
        val dates = (0..6).map { base.plusDays(it.toLong()) }

        assertEquals(7, StreakCalculator.longestStreak(dates.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun longestStreak_sameDayDuplicatesDoNotInflateStreak() {
        pinUtc()
        val base = LocalDate.of(2026, 2, 1)
        val doubled = listOf(base, base, base.plusDays(1))

        assertEquals(2, StreakCalculator.longestStreak(doubled.map { workout(it.toString(), millis(it)) }))
    }

    // ---- barProgress ----

    @Test
    fun barProgress_zeroStreakIsEmptyBar() {
        assertEquals(0f, StreakCalculator.barProgress(0), 0.0001f)
    }

    @Test
    fun barProgress_partialStreakIsFractional() {
        assertEquals(3f / 7f, StreakCalculator.barProgress(3), 0.0001f)
    }

    @Test
    fun barProgress_capsAtSevenDays() {
        assertEquals(1f, StreakCalculator.barProgress(7), 0.0001f)
        assertEquals(1f, StreakCalculator.barProgress(30), 0.0001f)
    }

    @Test
    fun barProgress_negativeStreakClampsToZero() {
        assertEquals(0f, StreakCalculator.barProgress(-2), 0.0001f)
    }

    @Test
    fun barProgress_customMaxScalesLinearly() {
        assertEquals(0.5f, StreakCalculator.barProgress(5, maxDisplayDays = 10), 0.0001f)
    }
}
