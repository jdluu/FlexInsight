package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.MuscleGroup

/**
 * Pure muscle-recovery math. Recovery for a group is the fraction of the recovery
 * window elapsed since it was last trained, clamped to 0..1.
 */
object RecoveryScoreCalculator {

    /** Hours of rest before a trained muscle group is considered fully recovered. */
    const val RECOVERY_WINDOW_MS: Long = 72 * 60 * 60 * 1000L

    /** Elapsed fraction of the recovery window, clamped to 0..1. */
    fun recoveryFraction(nowMs: Long, lastTrainedMs: Long, recoveryWindowMs: Long = RECOVERY_WINDOW_MS): Float =
        ((nowMs - lastTrainedMs).toFloat() / recoveryWindowMs).coerceIn(0f, 1f)

    /**
     * Recovery status for every muscle group. Groups without a recorded session
     * (missing or zero timestamp) are treated as fully recovered.
     */
    fun muscleRecoveryStatus(
        nowMs: Long,
        lastTrainedByGroup: Map<MuscleGroup, Long>,
        recoveryWindowMs: Long = RECOVERY_WINDOW_MS
    ): Map<MuscleGroup, Float> =
        MuscleGroup.values().associateWith { group ->
            val lastTrained = lastTrainedByGroup[group] ?: 0L
            if (lastTrained == 0L) 1.0f
            else recoveryFraction(nowMs, lastTrained, recoveryWindowMs)
        }

    /**
     * Average recovery across groups scaled to 0-100. An empty map yields 0.
     */
    fun overallRecoveryScore(recovery: Map<MuscleGroup, Float>): Int =
        (recovery.values.average() * 100).toInt().coerceIn(0, 100)
}
