package com.jdluu.flexinsight.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodComparisonTest {

    @Test
    fun volumeChangePercent_positiveWhenCurrentHigher() {
        val comparison = PeriodComparison(
            currentPeriodLabel = "May",
            previousPeriodLabel = "April",
            totalVolumeCurrent = 1500.0,
            totalVolumePrevious = 1000.0,
            totalWorkoutsCurrent = 10,
            totalWorkoutsPrevious = 8,
            avgDurationCurrent = 60,
            avgDurationPrevious = 55
        )

        assertEquals(50.0, comparison.volumeChangePercent, 0.01)
    }

    @Test
    fun volumeChangePercent_zeroWhenPreviousVolumeZero() {
        val comparison = PeriodComparison(
            currentPeriodLabel = "May",
            previousPeriodLabel = "April",
            totalVolumeCurrent = 500.0,
            totalVolumePrevious = 0.0,
            totalWorkoutsCurrent = 2,
            totalWorkoutsPrevious = 0,
            avgDurationCurrent = 45,
            avgDurationPrevious = 0
        )

        assertEquals(0.0, comparison.volumeChangePercent, 0.01)
    }

    @Test
    fun workoutCountDelta_reflectsDifference() {
        val comparison = PeriodComparison(
            currentPeriodLabel = "May",
            previousPeriodLabel = "April",
            totalVolumeCurrent = 0.0,
            totalVolumePrevious = 0.0,
            totalWorkoutsCurrent = 12,
            totalWorkoutsPrevious = 9,
            avgDurationCurrent = 0,
            avgDurationPrevious = 0
        )

        assertEquals(3, comparison.workoutCountDelta)
        assertTrue(comparison.hasCurrentData)
    }
}
