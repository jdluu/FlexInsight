package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.Workout

/**
 * Pure volume math: per-set tonnage, aggregate totals, period-over-period change,
 * muscle-group share, push/pull/legs/cardio balance, and intensity classification.
 */
object VolumeCalculator {

    /** Tonnage of a single set; null weight or reps contributes zero. */
    fun setVolume(weight: Double?, reps: Int?): Double = (weight ?: 0.0) * (reps ?: 0)

    /** Summed tonnage across a list of sets. */
    fun totalSetVolume(sets: List<Set>): Double = sets.sumOf { setVolume(it.weight, it.reps) }

    /**
     * Total volume across the given workouts. Only sets whose exercise belongs to one
     * of the workouts are counted; orphan exercises and sets are ignored.
     */
    fun totalVolume(
        workouts: List<Workout>,
        allExercises: List<Exercise>,
        allSets: List<Set>
    ): Double {
        val exercisesByWorkout = allExercises.groupBy { it.workoutId }
        val setsByExercise = allSets.groupBy { it.exerciseId }

        return workouts.sumOf { workout ->
            val exercises = exercisesByWorkout[workout.id] ?: emptyList()
            exercises.sumOf { exercise ->
                val sets = setsByExercise[exercise.id] ?: emptyList()
                totalSetVolume(sets)
            }
        }
    }

    /** Headline volume summed over weekly progress entries. */
    fun sumWeeklyVolume(progress: List<WeeklyProgress>): Double = progress.sumOf { it.totalVolume }

    /** Compact display label: whole thousands collapse to "12k", otherwise integer value. */
    fun compactThousandsLabel(value: Double): String =
        if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()

    /** Percent share of [part] within [total], truncated toward zero; 0 when total is not positive. */
    fun sharePercent(part: Double, total: Double): Int =
        if (total > 0) ((part / total) * 100).toInt() else 0

    /**
     * Percentage change between two periods. A zero previous period yields 100% when
     * there is current volume and 0% otherwise.
     */
    fun changePercent(current: Double, previous: Double): Double =
        if (previous > 0) {
            ((current - previous) / previous) * 100.0
        } else {
            if (current > 0) 100.0 else 0.0
        }

    /** Relative intensity bucket ("HI", "MD", "LO") versus the average volume. */
    fun relativeIntensity(volume: Double, averageVolume: Double): String = when {
        volume >= averageVolume * 1.5 -> "HI"
        volume >= averageVolume * 0.7 -> "MD"
        else -> "LO"
    }

    /** Absolute intensity label based on raw volume thresholds. */
    fun absoluteIntensity(totalVolume: Double): String = when {
        totalVolume > 5000 -> "High Intensity"
        totalVolume > 2000 -> "Medium Intensity"
        else -> "Aerobic"
    }

    /**
     * Volume split across push, pull, legs, and cardio categories. Matching is a
     * case-insensitive substring check evaluated in that order; when no categorized
     * volume exists the split defaults to quarters.
     */
    fun volumeBalance(muscleGroupProgress: List<MuscleGroupProgress>): VolumeBalance {
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
}
