package com.jdluu.flexinsight.domain.util

import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.model.Workout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * Characterization tests for [StatsCalculator]. Timezone is pinned to UTC so that
 * day-boundary math is deterministic; production behavior reads ZoneId.systemDefault().
 */
class StatsCalculatorTest {

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
        endTime: Long? = startTime + 3_600_000L,
        routineId: String? = null,
        isDeleted: Boolean = false
    ) = Workout(
        id = id,
        name = "Workout $id",
        startTime = startTime,
        endTime = endTime,
        notes = null,
        routineId = routineId,
        isDeleted = isDeleted
    )

    private fun exercise(id: String, workoutId: String) = Exercise(
        id = id,
        workoutId = workoutId,
        exerciseTemplateId = null,
        name = "Exercise $id",
        notes = null,
        restDuration = null
    )

    private fun set(id: String, exerciseId: String, weight: Double?, reps: Int?) = Set(
        id = id,
        exerciseId = exerciseId,
        number = 1,
        weight = weight,
        reps = reps,
        rpe = null,
        distance = null,
        duration = null,
        restDuration = null,
        notes = null
    )

    // ---- calculateTotalVolume ----

    @Test
    fun totalVolume_emptyWorkoutsIsZero() {
        val volume = StatsCalculator.calculateTotalVolume(
            workouts = emptyList(),
            allExercises = listOf(exercise("e1", "w1")),
            allSets = listOf(set("s1", "e1", 100.0, 10))
        )
        assertEquals(0.0, volume, 0.0001)
    }

    @Test
    fun totalVolume_sumsAcrossWorkoutsExercisesAndSets() {
        val w1 = workout("w1", 0)
        val w2 = workout("w2", 100)
        val sets = listOf(
            set("s1", "e1", 100.0, 10), // 1000
            set("s2", "e1", 50.0, 10),  // 500
            set("s3", "e2", 20.0, 5),   // 100
            set("s4", "e3", 30.0, 10)   // 300
        )
        val exercises = listOf(exercise("e1", "w1"), exercise("e2", "w1"), exercise("e3", "w2"))

        assertEquals(1900.0, StatsCalculator.calculateTotalVolume(listOf(w1, w2), exercises, sets), 0.0001)
    }

    @Test
    fun totalVolume_ignoresOrphanExercisesAndSets() {
        val w1 = workout("w1", 0)

        val volume = StatsCalculator.calculateTotalVolume(
            listOf(w1),
            listOf(exercise("orphan-e", "unknown-workout")),
            listOf(set("orphan-s", "unknown-exercise", 999.0, 99))
        )
        assertEquals(0.0, volume, 0.0001)
    }

    @Test
    fun totalVolume_nullWeightOrRepsCountAsZero() {
        val w1 = workout("w1", 0)
        val sets = listOf(
            set("s1", "e1", null, 10),   // null weight -> 0
            set("s2", "e1", 50.0, null), // null reps -> 0
            set("s3", "e1", 25.0, 8)     // 200
        )

        assertEquals(200.0, StatsCalculator.calculateTotalVolume(listOf(w1), listOf(exercise("e1", "w1")), sets), 0.0001)
    }

    @Test
    fun totalVolume_accumulatesFractionalWeights() {
        val w1 = workout("w1", 0)
        val sets = listOf(set("s1", "e1", 12.5, 4), set("s2", "e1", 7.25, 2))

        assertEquals(64.5, StatsCalculator.calculateTotalVolume(listOf(w1), listOf(exercise("e1", "w1")), sets), 0.0001)
    }

    // ---- calculateTotalDuration ----

    @Test
    fun totalDuration_emptyListIsZero() {
        assertEquals(0L, StatsCalculator.calculateTotalDuration(emptyList()))
    }

    @Test
    fun totalDuration_skipsWorkoutsWithoutEndTime() {
        val open = workout("open", 0, endTime = null)
        val closed = workout("closed", 0, endTime = 30 * 60 * 1000L)

        assertEquals(30L, StatsCalculator.calculateTotalDuration(listOf(open, closed)))
    }

    @Test
    fun totalDuration_truncatesPartialMinutes() {
        // 90 minutes and 59 seconds -> 90
        val w = workout("w", 0, endTime = 90 * 60 * 1000L + 59_999L)

        assertEquals(90L, StatsCalculator.calculateTotalDuration(listOf(w)))
    }

    @Test
    fun totalDuration_sumsMultipleWorkouts() {
        val a = workout("a", 0, endTime = 45 * 60 * 1000L)
        val b = workout("b", 1000, endTime = 1000 + 75 * 60 * 1000L)

        assertEquals(120L, StatsCalculator.calculateTotalDuration(listOf(a, b)))
    }

    // ---- calculateStreak ----

    @Test
    fun streak_emptyHistoryIsZero() {
        assertEquals(0, StatsCalculator.calculateStreak(emptyList()))
    }

    @Test
    fun streak_staleLatestDayBreaksStreakImmediately() {
        pinUtc()
        val today = LocalDate.now()
        val old = today.minusDays(5)

        assertEquals(0, StatsCalculator.calculateStreak(listOf(workout("old", millis(old)))))
    }

    @Test
    fun streak_countsBackThroughConsecutiveDaysFromToday() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today, today.minusDays(1), today.minusDays(2))

        assertEquals(3, StatsCalculator.calculateStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun streak_startedYesterdayStillCountsWhenTodayHasNoWorkout() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today.minusDays(1), today.minusDays(2))

        assertEquals(2, StatsCalculator.calculateStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun streak_multipleSessionsPerDayCollapseToOneDate() {
        pinUtc()
        val today = LocalDate.now()
        val sameDayTwice = listOf(
            workout("am", millis(today, hour = 8)),
            workout("pm", millis(today, hour = 20))
        )

        assertEquals(1, StatsCalculator.calculateStreak(sameDayTwice))
    }

    @Test
    fun streak_missedDayTruncatesRun() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4))

        assertEquals(2, StatsCalculator.calculateStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun streak_handlesUnsortedInput() {
        pinUtc()
        val today = LocalDate.now()
        val days = listOf(today.minusDays(2), today, today.minusDays(1))

        assertEquals(3, StatsCalculator.calculateStreak(days.map { workout(it.toString(), millis(it)) }))
    }

    // ---- calculateLongestStreak ----

    @Test
    fun longestStreak_emptyHistoryIsZero() {
        assertEquals(0, StatsCalculator.calculateLongestStreak(emptyList()))
    }

    @Test
    fun longestStreak_singleWorkoutIsOne() {
        assertEquals(1, StatsCalculator.calculateLongestStreak(listOf(workout("w", 0))))
    }

    @Test
    fun longestStreak_findsLongestRunAnywhereInHistory() {
        pinUtc()
        val base = LocalDate.of(2026, 1, 5) // Monday
        val run1 = (0..2).map { base.plusDays(it.toLong()) }          // 3-day run
        val run2 = (0..4).map { base.plusDays(10 + it.toLong()) }     // 5-day run
        val dates = run1 + run2

        assertEquals(5, StatsCalculator.calculateLongestStreak(dates.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun longestStreak_fullyConsecutiveHistoryReturnsEntireLength() {
        pinUtc()
        val base = LocalDate.of(2026, 3, 1)
        val dates = (0..6).map { base.plusDays(it.toLong()) }

        assertEquals(7, StatsCalculator.calculateLongestStreak(dates.map { workout(it.toString(), millis(it)) }))
    }

    @Test
    fun longestStreak_sameDayDuplicatesDoNotInflateStreak() {
        pinUtc()
        val base = LocalDate.of(2026, 2, 1)
        val doubled = listOf(base, base, base.plusDays(1))

        assertEquals(2, StatsCalculator.calculateLongestStreak(doubled.map { workout(it.toString(), millis(it)) }))
    }

    // ---- calculateVolumeBalance ----

    @Test
    fun volumeBalance_emptyInputDefaultsToQuarterSplit() {
        assertEquals(VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f), StatsCalculator.calculateVolumeBalance(emptyList()))
    }

    @Test
    fun volumeBalance_categorizesPushPullLegsCardio() {
        val progress = listOf(
            MuscleGroupProgress("Chest", 300.0, 10, "MD"),
            MuscleGroupProgress("Shoulders", 100.0, 5, "LO"),
            MuscleGroupProgress("Back", 200.0, 8, "MD"),
            MuscleGroupProgress("Biceps", 100.0, 4, "LO"),
            MuscleGroupProgress("Quads", 400.0, 12, "HI"),
            MuscleGroupProgress("Cardio", 50.0, 1, "LO")
        )

        val balance = StatsCalculator.calculateVolumeBalance(progress)

        assertEquals(400.0f / 1150.0f, balance.push, 0.0001f)
        assertEquals(300.0f / 1150.0f, balance.pull, 0.0001f)
        assertEquals(400.0f / 1150.0f, balance.legs, 0.0001f)
        assertEquals(50.0f / 1150.0f, balance.cardio, 0.0001f)
    }

    @Test
    fun volumeBalance_matchingIsCaseInsensitiveOnSubstrings() {
        val progress = listOf(
            MuscleGroupProgress("upper chest", 600.0, 6, "HI"),
            MuscleGroupProgress("HAMSTRINGS", 200.0, 6, "MD")
        )

        val balance = StatsCalculator.calculateVolumeBalance(progress)

        assertEquals(0.75f, balance.push, 0.0001f)
        assertEquals(0.25f, balance.legs, 0.0001f)
    }

    @Test
    fun volumeBalance_unknownCategoriesAreExcludedFromSplit() {
        val progress = listOf(MuscleGroupProgress("Neck", 900.0, 3, "LO"))

        // Total categorized volume is zero -> falls back to the default quarter split
        assertEquals(VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f), StatsCalculator.calculateVolumeBalance(progress))
    }

    @Test
    fun volumeBalance_categoryCheckOrderPrefersPushOverPullOverLegs() {
        // A group name containing keywords from multiple categories resolves to the first
        // matching branch: push ("Chest"/"Shoulders"/"Triceps") beats pull beats legs.
        val tricky = listOf(
            MuscleGroupProgress("Back Chest Combo", 100.0, 1, "MD"),
            MuscleGroupProgress("Legs Back Hybrid", 100.0, 1, "MD")
        )

        val balance = StatsCalculator.calculateVolumeBalance(tricky)

        assertEquals(0.5f, balance.push, 0.0001f) // "Back Chest Combo" contains "Chest" -> push
        assertEquals(0.5f, balance.pull, 0.0001f) // "Legs Back Hybrid" contains "Back" -> pull
        assertEquals(0.0f, balance.legs, 0.0001f)
    }

    // ---- calculateDurationTrend ----

    @Test
    fun durationTrend_returnsSixEntriesMondayThroughSaturday() {
        // NOTE: possibly unintended - Sunday is aggregated internally but never emitted,
        // so Sunday sessions are invisible in the trend output
        val result = StatsCalculator.calculateDurationTrend(emptyList(), 0, Long.MAX_VALUE)

        assertEquals(
            listOf("M", "T", "W", "T", "F", "S"),
            result.map { it.dayOfWeek }
        )
    }

    @Test
    fun durationTrend_sundayWorkoutsAreDroppedFromOutput() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)  // Monday
        val sunday = LocalDate.of(2026, 1, 11) // Sunday of that week
        val start = millis(monday)
        val end = millis(sunday, hour = 23)

        val result = StatsCalculator.calculateDurationTrend(
            listOf(workout("sun", millis(sunday), millis(sunday, hour = 11))),
            start,
            end
        )

        // No Monday..Saturday entry carries the Sunday session's average
        assertEquals((1..6).map { 0L }, result.map { it.averageDuration })
    }

    @Test
    fun durationTrend_averagesPerWeekdayAndTruncatesFractionalMinutes() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)
        val wednesday = LocalDate.of(2026, 1, 7)
        val start = millis(monday, hour = 0)
        val end = millis(wednesday, hour = 23)

        val workouts = listOf(
            workout("m1", millis(monday, hour = 9), millis(monday, hour = 9) + 61 * 60_000L),
            workout("m2", millis(monday, hour = 15), millis(monday, hour = 15) + 60 * 60_000L),
            workout("w1", millis(wednesday, hour = 12), millis(wednesday, hour = 13))
        )

        val result = StatsCalculator.calculateDurationTrend(workouts, start, end)

        assertEquals(
            listOf(
                DailyDurationData("M", 60L), // avg(61, 60) = 60.5 truncated to 60
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
        val start = millis(monday, hour = 0)
        val end = millis(wednesday, hour = 23)

        val workouts = listOf(
            workout("at-start", millis(monday, hour = 6), millis(monday, hour = 7)),
            workout("at-end", millis(wednesday, hour = 22), millis(wednesday, hour = 23)),
            workout("before-window", millis(monday.minusDays(1)), millis(monday.minusDays(1), hour = 12)),
            workout("after-window", millis(wednesday.plusDays(1)), millis(wednesday.plusDays(1), hour = 12))
        )

        val result = StatsCalculator.calculateDurationTrend(workouts, start, end)

        // Monday and Wednesday both carry data; Tuesday stays empty
        assertEquals(60L, result[0].averageDuration)
        assertEquals(0L, result[1].averageDuration)
        assertEquals(60L, result[2].averageDuration)
    }

    @Test
    fun durationTrend_excludesWorkoutsMissingEndTime() {
        pinUtc()
        val monday = LocalDate.of(2026, 1, 5)

        val result = StatsCalculator.calculateDurationTrend(
            listOf(workout("open-ended", millis(monday), endTime = null)),
            millis(monday, hour = 0),
            millis(monday, hour = 23)
        )

        assertEquals(0L, result[0].averageDuration)
    }

    // ---- getStartOfDay / getEndOfDay ----

    @Test
    fun startOfDay_mapsToMidnightOfSameLocalDate() {
        pinUtc()
        val timestamp = Instant.parse("2026-06-15T14:30:45Z").toEpochMilli()

        val expected = LocalDate.of(2026, 6, 15).atStartOfDay(zone()).toInstant().toEpochMilli()

        assertEquals(expected, StatsCalculator.getStartOfDay(timestamp))
    }

    @Test
    fun endOfDay_isLastMillisecondOfSameLocalDate() {
        pinUtc()
        val date = LocalDate.of(2026, 6, 15)
        val timestamp = millis(date, hour = 18)

        val start = StatsCalculator.getStartOfDay(timestamp)
        val end = StatsCalculator.getEndOfDay(timestamp)

        // LocalTime.MAX truncates to milliseconds when converted to epoch millis
        assertEquals(start + 86_399_999L, end)
        assertEquals(date.atTime(LocalTime.MAX).atZone(zone()).toInstant().toEpochMilli(), end)
    }

    // ---- calculateVolumeChange ----

    @Test
    fun volumeChange_positiveDeltaYieldsPositivePercent() {
        assertEquals(25.0, StatsCalculator.calculateVolumeChange(1250.0, 1000.0), 0.0001)
    }

    @Test
    fun volumeChange_negativeDeltaYieldsNegativePercent() {
        assertEquals(-40.0, StatsCalculator.calculateVolumeChange(600.0, 1000.0), 0.0001)
    }

    @Test
    fun volumeChange_zeroPreviousWithCurrentReturns100() {
        assertEquals(100.0, StatsCalculator.calculateVolumeChange(500.0, 0.0), 0.0001)
    }

    @Test
    fun volumeChange_bothZeroReturnsZero() {
        assertEquals(0.0, StatsCalculator.calculateVolumeChange(0.0, 0.0), 0.0001)
    }

    // ---- calculateGoalStatus ----

    @Test
    fun goalStatus_atTargetIsOnTrack() {
        assertEquals("On Track", StatsCalculator.calculateGoalStatus(completed = 5, target = 5))
    }

    @Test
    fun goalStatus_exactlySeventyPercentIsOnTrack() {
        assertEquals("On Track", StatsCalculator.calculateGoalStatus(completed = 7, target = 10))
    }

    @Test
    fun goalStatus_betweenFiftyAndSeventyPercentIsBehind() {
        // NOTE: possibly unintended - the function can never return "Ahead", and the
        // >=target and >=70% branches are duplicates of each other
        assertEquals("Behind", StatsCalculator.calculateGoalStatus(completed = 6, target = 10))
    }

    @Test
    fun goalStatus_belowFiftyPercentIsBehind() {
        assertEquals("Behind", StatsCalculator.calculateGoalStatus(completed = 4, target = 10))
    }

    @Test
    fun goalStatus_zeroCompletedIsBehind() {
        assertEquals("Behind", StatsCalculator.calculateGoalStatus(completed = 0, target = 10))
    }

    // ---- calculateRelativeIntensity ----

    @Test
    fun relativeIntensity_onePointFiveTimesAverageIsHigh() {
        assertEquals("HI", StatsCalculator.calculateRelativeIntensity(volume = 1500.0, averageVolume = 1000.0))
    }

    @Test
    fun relativeIntensity_seventyToHundredFortyNinePercentIsMedium() {
        assertEquals("MD", StatsCalculator.calculateRelativeIntensity(volume = 700.0, averageVolume = 1000.0))
        assertEquals("MD", StatsCalculator.calculateRelativeIntensity(volume = 1499.99, averageVolume = 1000.0))
    }

    @Test
    fun relativeIntensity_belowSeventyPercentIsLow() {
        assertEquals("LO", StatsCalculator.calculateRelativeIntensity(volume = 699.99, averageVolume = 1000.0))
    }

    @Test
    fun relativeIntensity_boundariesAreInclusive() {
        assertEquals("HI", StatsCalculator.calculateRelativeIntensity(volume = 1500.0, averageVolume = 1000.0))
        assertEquals("MD", StatsCalculator.calculateRelativeIntensity(volume = 700.0, averageVolume = 1000.0))
    }

    @Test
    fun relativeIntensity_zeroAverageMarksEverythingHigh() {
        // NOTE: possibly unintended - with no historical average, even zero volume scores "HI"
        assertEquals("HI", StatsCalculator.calculateRelativeIntensity(volume = 0.0, averageVolume = 0.0))
    }

    // ---- calculateAbsoluteIntensity ----

    @Test
    fun absoluteIntensity_aboveFiveThousandIsHigh() {
        assertEquals("High Intensity", StatsCalculator.calculateAbsoluteIntensity(5000.01))
    }

    @Test
    fun absoluteIntensity_exactlyFiveThousandIsMedium() {
        assertEquals("Medium Intensity", StatsCalculator.calculateAbsoluteIntensity(5000.0))
    }

    @Test
    fun absoluteIntensity_aboveTwoThousandIsMedium() {
        assertEquals("Medium Intensity", StatsCalculator.calculateAbsoluteIntensity(2000.01))
    }

    @Test
    fun absoluteIntensity_exactlyTwoThousandOrBelowIsAerobic() {
        assertEquals("Aerobic", StatsCalculator.calculateAbsoluteIntensity(2000.0))
        assertEquals("Aerobic", StatsCalculator.calculateAbsoluteIntensity(0.0))
    }
}
