package com.jdluu.flexinsight.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MuscleGroupTest {

    @Test
    fun `fromString maps known aliases case-insensitively`() {
        assertEquals(MuscleGroup.CHEST, MuscleGroup.fromString("Upper Chest"))
        assertEquals(MuscleGroup.BACK, MuscleGroup.fromString("Lats & Back"))
        assertEquals(MuscleGroup.SHOULDERS, MuscleGroup.fromString("Rear Delts"))
        assertEquals(MuscleGroup.SHOULDERS, MuscleGroup.fromString("Shoulder Press"))
        assertEquals(MuscleGroup.ARMS, MuscleGroup.fromString("Biceps"))
        assertEquals(MuscleGroup.ARMS, MuscleGroup.fromString("Triceps"))
        assertEquals(MuscleGroup.ARMS, MuscleGroup.fromString("Arms"))
        assertEquals(MuscleGroup.LEGS, MuscleGroup.fromString("Quads"))
        assertEquals(MuscleGroup.LEGS, MuscleGroup.fromString("Hamstrings"))
        assertEquals(MuscleGroup.LEGS, MuscleGroup.fromString("Calves - Calf"))
        assertEquals(MuscleGroup.LEGS, MuscleGroup.fromString("Glutes"))
        assertEquals(MuscleGroup.LEGS, MuscleGroup.fromString("Leg Day"))
        assertEquals(MuscleGroup.CORE, MuscleGroup.fromString("Abs"))
        assertEquals(MuscleGroup.CORE, MuscleGroup.fromString("Core Stability"))
        assertEquals(MuscleGroup.CARDIO, MuscleGroup.fromString("Cardio Conditioning"))
    }

    @Test
    fun `fromString returns null for null or unknown input`() {
        assertNull(MuscleGroup.fromString(null))
        assertNull(MuscleGroup.fromString(""))
        assertNull(MuscleGroup.fromString("Neck"))
    }

    @Test
    fun `summary value models hold expected fields`() {
        val pr = PersonalRecord(
            exerciseName = "Squat",
            exerciseTemplateId = "tpl-1",
            recordType = "weight",
            value = 140.0,
            date = 1_000L,
            workoutId = "w-1"
        )
        val weekly = WeeklyVolumeData(weekLabel = "Nov 4", volume = 12_500.0)
        val prefs = UserPreferences(
            weeklyGoal = 4,
            theme = "Dark",
            units = "Metric",
            viewOnlyMode = false
        )

        assertEquals("Squat", pr.exerciseName)
        assertEquals("w-1", pr.workoutId)
        assertEquals(12_500.0, weekly.volume, 0.0)
        assertEquals(4, prefs.weeklyGoal)
    }
}
