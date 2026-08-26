package com.jdluu.flexinsight.domain.calc

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class TrainingLoadCalculatorTest {

    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    // ---- hevyVolumeScore ----

    @Test
    fun volumeScore_derivedFromGoalCompletion() {
        assertEquals(100, TrainingLoadCalculator.hevyVolumeScore(goalCompleted = 4, goalTarget = 4, totalWorkouts = null))
    }

    @Test
    fun volumeScore_truncatesFractionalPercentage() {
        // 2/3 of goal = 66.67%, toInt() truncates to 66
        assertEquals(66, TrainingLoadCalculator.hevyVolumeScore(goalCompleted = 2, goalTarget = 3, totalWorkouts = null))
    }

    @Test
    fun volumeScore_coercedTo100WhenGoalExceeded() {
        assertEquals(100, TrainingLoadCalculator.hevyVolumeScore(goalCompleted = 10, goalTarget = 5, totalWorkouts = null))
    }

    @Test
    fun volumeScore_coercedTo0WhenNothingCompleted() {
        assertEquals(0, TrainingLoadCalculator.hevyVolumeScore(goalCompleted = 0, goalTarget = 5, totalWorkouts = null))
    }

    @Test
    fun volumeScore_zeroTargetSkipsGoalBranchAndUsesWorkoutCountFallback() {
        assertEquals(65, TrainingLoadCalculator.hevyVolumeScore(goalCompleted = 0, goalTarget = 0, totalWorkouts = 3))
    }

    @Test
    fun volumeScore_nullGoalWithWorkoutsYields65() {
        assertEquals(65, TrainingLoadCalculator.hevyVolumeScore(null, null, totalWorkouts = 1))
    }

    @Test
    fun volumeScore_noDataAtAllYieldsFloorOf30() {
        assertEquals(30, TrainingLoadCalculator.hevyVolumeScore(null, null, null))
        assertEquals(30, TrainingLoadCalculator.hevyVolumeScore(null, null, totalWorkouts = 0))
    }

    // ---- cardioScore ----

    @Test
    fun cardioScore_is15PointsPerSession() {
        assertEquals(60, TrainingLoadCalculator.cardioScore(4))
        assertEquals(0, TrainingLoadCalculator.cardioScore(0))
    }

    @Test
    fun cardioScore_cappedAt100() {
        // 7 sessions would naively be 105; capped at 100
        assertEquals(100, TrainingLoadCalculator.cardioScore(7))
    }

    // ---- sleepScore ----

    @Test
    fun sleepScore_missingDataIsNeutral50() {
        assertEquals(50, TrainingLoadCalculator.sleepScore(null))
    }

    @Test
    fun sleepScore_sevenHoursOrMoreScores90() {
        assertEquals(90, TrainingLoadCalculator.sleepScore(7.0))
        assertEquals(90, TrainingLoadCalculator.sleepScore(8.25))
    }

    @Test
    fun sleepScore_sixToSevenHoursScores70() {
        assertEquals(70, TrainingLoadCalculator.sleepScore(6.0))
        assertEquals(70, TrainingLoadCalculator.sleepScore(6.5))
    }

    @Test
    fun sleepScore_fiveToSixHoursScores50() {
        assertEquals(50, TrainingLoadCalculator.sleepScore(5.0))
        assertEquals(50, TrainingLoadCalculator.sleepScore(5.5))
    }

    @Test
    fun sleepScore_belowFiveHoursScores25() {
        assertEquals(25, TrainingLoadCalculator.sleepScore(4.99))
    }

    // ---- overall ----

    @Test
    fun overall_blendsWithFixedWeightsAndTruncates() {
        // 100*0.5 + 60*0.2 + 90*0.3 = 89
        assertEquals(89, TrainingLoadCalculator.overall(volumeScore = 100, cardioScore = 60, sleepScore = 90))
        // 65*0.5 + 15*0.2 + 70*0.3 = 56.5 -> 56
        assertEquals(56, TrainingLoadCalculator.overall(volumeScore = 65, cardioScore = 15, sleepScore = 70))
        // 30*0.5 + 0*0.2 + 50*0.3 = 30
        assertEquals(30, TrainingLoadCalculator.overall(volumeScore = 30, cardioScore = 0, sleepScore = 50))
    }

    @Test
    fun overall_isClampedToHundredRange() {
        assertEquals(100, TrainingLoadCalculator.overall(100, 100, 100))
        assertEquals(0, TrainingLoadCalculator.overall(0, 0, 0))
    }

    // ---- label ----

    @Test
    fun label_thresholds() {
        assertEquals("High load", TrainingLoadCalculator.label(80))
        assertEquals("High load", TrainingLoadCalculator.label(100))
        assertEquals("Moderate load", TrainingLoadCalculator.label(79))
        assertEquals("Moderate load", TrainingLoadCalculator.label(55))
        assertEquals("Recovery focus", TrainingLoadCalculator.label(54))
        assertEquals("Recovery focus", TrainingLoadCalculator.label(0))
    }

    // ---- detail ----

    @Test
    fun detail_omitsHealthPartsWhenNoData() {
        assertEquals(
            "Hevy volume 65/100",
            TrainingLoadCalculator.detail(65, hasHealthData = false, 7.5, 5000L, 2)
        )
    }

    @Test
    fun detail_includesSleepStepsAndCardioWhenPresent() {
        assertEquals(
            "Hevy volume 60/100, sleep 7.5h, steps 5000, 2 cardio sessions",
            TrainingLoadCalculator.detail(60, hasHealthData = true, 7.5, 5000L, 2)
        )
    }

    @Test
    fun detail_restingHeartRateAloneCountsAsDataButNeverAppears() {
        // hasHealthData=true with all optional segments null/zero renders the bare prefix
        assertEquals(
            "Hevy volume 65/100",
            TrainingLoadCalculator.detail(65, hasHealthData = true, null, null, 0)
        )
    }

    @Test
    fun detail_omitsCardioSegmentWhenZeroSessions() {
        assertEquals(
            "Hevy volume 20/100, sleep 8.0h, steps 12000",
            TrainingLoadCalculator.detail(20, hasHealthData = true, 8.0, 12000L, 0)
        )
    }

    // ---- goalStatus ----

    @Test
    fun goalStatus_atTargetIsOnTrack() {
        assertEquals("On Track", TrainingLoadCalculator.goalStatus(completed = 5, target = 5))
    }

    @Test
    fun goalStatus_exactlySeventyPercentIsOnTrack() {
        assertEquals("On Track", TrainingLoadCalculator.goalStatus(completed = 7, target = 10))
    }

    @Test
    fun goalStatus_betweenFiftyAndSeventyPercentIsBehind() {
        assertEquals("Behind", TrainingLoadCalculator.goalStatus(completed = 6, target = 10))
    }

    @Test
    fun goalStatus_belowFiftyPercentIsBehind() {
        assertEquals("Behind", TrainingLoadCalculator.goalStatus(completed = 0, target = 10))
    }
}
