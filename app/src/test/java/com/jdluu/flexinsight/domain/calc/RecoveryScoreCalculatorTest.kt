package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryScoreCalculatorTest {

    private val hourMs = 60L * 60 * 1000

    // ---- recoveryFraction ----

    @Test
    fun recoveryFraction_halfWindowIsHalf() {
        assertEquals(0.5f, RecoveryScoreCalculator.recoveryFraction(nowMs = 36 * hourMs, lastTrainedMs = 0), 0.0001f)
    }

    @Test
    fun recoveryFraction_clampsToOneBeyondWindow() {
        assertEquals(1.0f, RecoveryScoreCalculator.recoveryFraction(nowMs = 100 * hourMs, lastTrainedMs = 0), 0.0001f)
    }

    @Test
    fun recoveryFraction_futureTimestampClampsToZero() {
        assertEquals(0.0f, RecoveryScoreCalculator.recoveryFraction(nowMs = 0, lastTrainedMs = hourMs), 0.0001f)
    }

    @Test
    fun recoveryFraction_customWindowScales() {
        // 12 hours elapsed against a 24-hour window -> 0.5
        assertEquals(
            0.5f,
            RecoveryScoreCalculator.recoveryFraction(nowMs = 12 * hourMs, lastTrainedMs = 0, recoveryWindowMs = 24 * hourMs),
            0.0001f
        )
    }

    // ---- muscleRecoveryStatus ----

    @Test
    fun status_coversEveryMuscleGroup() {
        val status = RecoveryScoreCalculator.muscleRecoveryStatus(nowMs = 0, lastTrainedByGroup = emptyMap())

        assertEquals(MuscleGroup.values().toSet(), status.keys)
    }

    @Test
    fun status_untrainedGroupsAreFullyRecovered() {
        val status = RecoveryScoreCalculator.muscleRecoveryStatus(nowMs = 0, lastTrainedByGroup = emptyMap())

        MuscleGroup.values().forEach { group ->
            assertEquals(1.0f, status.getValue(group), 0.0001f)
        }
    }

    @Test
    fun status_trainedGroupsReflectElapsedWindow() {
        val now = 72 * hourMs
        val status = RecoveryScoreCalculator.muscleRecoveryStatus(
            nowMs = now,
            lastTrainedByGroup = mapOf(
                MuscleGroup.CHEST to now - 36 * hourMs,  // half recovered
                MuscleGroup.BACK to now - 80 * hourMs    // beyond window, clamped
            )
        )

        assertEquals(0.5f, status.getValue(MuscleGroup.CHEST), 0.0001f)
        assertEquals(1.0f, status.getValue(MuscleGroup.BACK), 0.0001f)
        assertEquals(1.0f, status.getValue(MuscleGroup.LEGS), 0.0001f)
    }

    // ---- overallRecoveryScore ----

    @Test
    fun overall_truncatesAverageTimesHundred() {
        // avg(0.5, 0.75) = 0.625 -> 62
        assertEquals(
            62,
            RecoveryScoreCalculator.overallRecoveryScore(
                mapOf(MuscleGroup.CHEST to 0.5f, MuscleGroup.BACK to 0.75f)
            )
        )
    }

    @Test
    fun overall_emptyMapYieldsZero() {
        assertEquals(0, RecoveryScoreCalculator.overallRecoveryScore(emptyMap()))
    }

    @Test
    fun overall_fullRecoveryYieldsHundred() {
        val allRecovered = MuscleGroup.values().associateWith { 1.0f }

        assertEquals(100, RecoveryScoreCalculator.overallRecoveryScore(allRecovered))
    }
}
