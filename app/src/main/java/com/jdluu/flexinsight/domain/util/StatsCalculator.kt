package com.jdluu.flexinsight.domain.util

import com.jdluu.flexinsight.data.model.*
import com.jdluu.flexinsight.domain.calc.DurationCalculator
import com.jdluu.flexinsight.domain.calc.StreakCalculator
import com.jdluu.flexinsight.domain.calc.TrainingLoadCalculator
import com.jdluu.flexinsight.domain.calc.VolumeCalculator

/**
 * Facade over the pure calculators in [com.jdluu.flexinsight.domain.calc].
 * Kept for source compatibility with existing call sites and tests; new code
 * should call the calculators directly.
 */
object StatsCalculator {

    /** Calculate total volume from a list of workouts, exercises, and sets. */
    fun calculateTotalVolume(
        workouts: List<Workout>,
        allExercises: List<Exercise>,
        allSets: List<com.jdluu.flexinsight.data.model.Set>
    ): Double = VolumeCalculator.totalVolume(workouts, allExercises, allSets)

    /** Calculate total duration in minutes. */
    fun calculateTotalDuration(workouts: List<Workout>): Long =
        DurationCalculator.totalDuration(workouts)

    /** Calculate current streak of consecutive days with workouts. */
    fun calculateStreak(workouts: List<Workout>): Int =
        StreakCalculator.currentStreak(workouts)

    /** Calculate longest streak of consecutive days. */
    fun calculateLongestStreak(workouts: List<Workout>): Int =
        StreakCalculator.longestStreak(workouts)

    /** Calculate volume balance across muscle groups. */
    fun calculateVolumeBalance(muscleGroupProgress: List<MuscleGroupProgress>): VolumeBalance =
        VolumeCalculator.volumeBalance(muscleGroupProgress)

    /** Calculate daily duration trend. */
    fun calculateDurationTrend(
        workouts: List<Workout>,
        startDate: Long,
        endDate: Long
    ): List<DailyDurationData> = DurationCalculator.durationTrend(workouts, startDate, endDate)

    /** Get start of the day timestamp. */
    fun getStartOfDay(timestamp: Long): Long = DurationCalculator.startOfDay(timestamp)

    /** Get end of the day timestamp. */
    fun getEndOfDay(timestamp: Long): Long = DurationCalculator.endOfDay(timestamp)

    /** Calculate volume percentage change. */
    fun calculateVolumeChange(currentVolume: Double, previousVolume: Double): Double =
        VolumeCalculator.changePercent(currentVolume, previousVolume)

    /** Calculate weekly goal status. */
    fun calculateGoalStatus(completed: Int, target: Int): String =
        TrainingLoadCalculator.goalStatus(completed, target)

    /** Calculate relative intensity (HI/MD/LO) based on average volume. */
    fun calculateRelativeIntensity(volume: Double, averageVolume: Double): String =
        VolumeCalculator.relativeIntensity(volume, averageVolume)

    /** Calculate absolute intensity based on total volume. */
    fun calculateAbsoluteIntensity(totalVolume: Double): String =
        VolumeCalculator.absoluteIntensity(totalVolume)
}
