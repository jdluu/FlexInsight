package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeCalculatorTest {

    private fun workout(id: String, startTime: Long = 0) = Workout(
        id = id,
        name = "Workout $id",
        startTime = startTime,
        endTime = startTime + 3_600_000L,
        notes = null,
        routineId = null
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

    // ---- setVolume / totalSetVolume ----

    @Test
    fun setVolume_nullWeightOrRepsIsZero() {
        assertEquals(0.0, VolumeCalculator.setVolume(null, 10), 0.0001)
        assertEquals(0.0, VolumeCalculator.setVolume(50.0, null), 0.0001)
        assertEquals(0.0, VolumeCalculator.setVolume(null, null), 0.0001)
    }

    @Test
    fun setVolume_multipliesWeightByReps() {
        assertEquals(500.0, VolumeCalculator.setVolume(50.0, 10), 0.0001)
    }

    @Test
    fun totalSetVolume_sumsAllSets() {
        val sets = listOf(set("s1", "e1", 100.0, 10), set("s2", "e1", 25.0, 4))

        assertEquals(1100.0, VolumeCalculator.totalSetVolume(sets), 0.0001)
    }

    @Test
    fun totalSetVolume_emptyListIsZero() {
        assertEquals(0.0, VolumeCalculator.totalSetVolume(emptyList()), 0.0001)
    }

    // ---- totalVolume ----

    @Test
    fun totalVolume_sumsAcrossWorkoutsExercisesAndSets() {
        val sets = listOf(
            set("s1", "e1", 100.0, 10), // 1000
            set("s2", "e2", 20.0, 5),   // 100
            set("s3", "e3", 30.0, 10)   // 300
        )
        val exercises = listOf(exercise("e1", "w1"), exercise("e2", "w1"), exercise("e3", "w2"))

        assertEquals(
            1400.0,
            VolumeCalculator.totalVolume(listOf(workout("w1"), workout("w2")), exercises, sets),
            0.0001
        )
    }

    @Test
    fun totalVolume_ignoresOrphanExercisesAndSets() {
        val volume = VolumeCalculator.totalVolume(
            listOf(workout("w1")),
            listOf(exercise("orphan-e", "unknown-workout")),
            listOf(set("orphan-s", "unknown-exercise", 999.0, 99))
        )

        assertEquals(0.0, volume, 0.0001)
    }

    @Test
    fun totalVolume_emptyWorkoutsIsZero() {
        val volume = VolumeCalculator.totalVolume(
            emptyList(),
            listOf(exercise("e1", "w1")),
            listOf(set("s1", "e1", 100.0, 10))
        )

        assertEquals(0.0, volume, 0.0001)
    }

    // ---- sumWeeklyVolume ----

    @Test
    fun sumWeeklyVolume_sumsWeeklyTotals() {
        val progress = listOf(
            WeeklyProgress(weekStartDate = 0, totalVolume = 1200.0, workoutCount = 2, averageVolume = 600.0),
            WeeklyProgress(weekStartDate = 1, totalVolume = 800.5, workoutCount = 1, averageVolume = 800.5),
            WeeklyProgress(weekStartDate = 2, totalVolume = 0.0, workoutCount = 0, averageVolume = 0.0)
        )

        assertEquals(2000.5, VolumeCalculator.sumWeeklyVolume(progress), 0.0001)
    }

    @Test
    fun sumWeeklyVolume_emptyListIsZero() {
        assertEquals(0.0, VolumeCalculator.sumWeeklyVolume(emptyList()), 0.0001)
    }

    // ---- compactThousandsLabel ----

    @Test
    fun compactThousandsLabel_collapsesThousands() {
        assertEquals("12k", VolumeCalculator.compactThousandsLabel(12345.0))
        assertEquals("1k", VolumeCalculator.compactThousandsLabel(1999.9))
    }

    @Test
    fun compactThousandsLabel_belowThousandTruncatesToInteger() {
        assertEquals("999", VolumeCalculator.compactThousandsLabel(999.9))
        assertEquals("0", VolumeCalculator.compactThousandsLabel(0.0))
    }

    // ---- sharePercent ----

    @Test
    fun sharePercent_truncatesFractionalPercent() {
        // 1/3 -> 33.33 truncated to 33
        assertEquals(33, VolumeCalculator.sharePercent(1.0, 3.0))
    }

    @Test
    fun sharePercent_nonPositiveTotalIsZero() {
        assertEquals(0, VolumeCalculator.sharePercent(50.0, 0.0))
        assertEquals(0, VolumeCalculator.sharePercent(50.0, -10.0))
    }

    // ---- changePercent ----

    @Test
    fun changePercent_positiveDeltaYieldsPositivePercent() {
        assertEquals(25.0, VolumeCalculator.changePercent(1250.0, 1000.0), 0.0001)
    }

    @Test
    fun changePercent_negativeDeltaYieldsNegativePercent() {
        assertEquals(-40.0, VolumeCalculator.changePercent(600.0, 1000.0), 0.0001)
    }

    @Test
    fun changePercent_zeroPreviousWithCurrentReturns100() {
        assertEquals(100.0, VolumeCalculator.changePercent(500.0, 0.0), 0.0001)
    }

    @Test
    fun changePercent_bothZeroReturnsZero() {
        assertEquals(0.0, VolumeCalculator.changePercent(0.0, 0.0), 0.0001)
    }

    // ---- relativeIntensity ----

    @Test
    fun relativeIntensity_bucketsAgainstAverage() {
        assertEquals("HI", VolumeCalculator.relativeIntensity(volume = 1500.0, averageVolume = 1000.0))
        assertEquals("MD", VolumeCalculator.relativeIntensity(volume = 700.0, averageVolume = 1000.0))
        assertEquals("MD", VolumeCalculator.relativeIntensity(volume = 1499.99, averageVolume = 1000.0))
        assertEquals("LO", VolumeCalculator.relativeIntensity(volume = 699.99, averageVolume = 1000.0))
    }

    @Test
    fun relativeIntensity_zeroAverageMarksEverythingHigh() {
        assertEquals("HI", VolumeCalculator.relativeIntensity(volume = 0.0, averageVolume = 0.0))
    }

    // ---- absoluteIntensity ----

    @Test
    fun absoluteIntensity_thresholdsAreExclusive() {
        assertEquals("High Intensity", VolumeCalculator.absoluteIntensity(5000.01))
        assertEquals("Medium Intensity", VolumeCalculator.absoluteIntensity(5000.0))
        assertEquals("Medium Intensity", VolumeCalculator.absoluteIntensity(2000.01))
        assertEquals("Aerobic", VolumeCalculator.absoluteIntensity(2000.0))
        assertEquals("Aerobic", VolumeCalculator.absoluteIntensity(0.0))
    }

    // ---- volumeBalance ----

    @Test
    fun volumeBalance_emptyInputDefaultsToQuarterSplit() {
        assertEquals(VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f), VolumeCalculator.volumeBalance(emptyList()))
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

        val balance = VolumeCalculator.volumeBalance(progress)

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

        val balance = VolumeCalculator.volumeBalance(progress)

        assertEquals(0.75f, balance.push, 0.0001f)
        assertEquals(0.25f, balance.legs, 0.0001f)
    }

    @Test
    fun volumeBalance_unknownCategoriesFallBackToQuarterSplit() {
        val progress = listOf(MuscleGroupProgress("Neck", 900.0, 3, "LO"))

        assertEquals(VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f), VolumeCalculator.volumeBalance(progress))
    }
}
