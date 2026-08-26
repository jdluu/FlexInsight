package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.Set
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordCalculatorTest {

    private fun set(
        id: String,
        weight: Double?,
        reps: Int?,
        isPersonalRecord: Boolean = false
    ) = Set(
        id = id,
        exerciseId = "e1",
        number = 1,
        weight = weight,
        reps = reps,
        rpe = null,
        distance = null,
        duration = null,
        restDuration = null,
        notes = null,
        isPersonalRecord = isPersonalRecord
    )

    // ---- bestSetVolume ----

    @Test
    fun bestSetVolume_picksHeaviestSetByTonnage() {
        val sets = listOf(
            set("s1", weight = 100.0, reps = 5),   // 500
            set("s2", weight = 60.0, reps = 12),   // 720
            set("s3", weight = 140.0, reps = 1)    // 140
        )

        assertEquals(720.0, PersonalRecordCalculator.bestSetVolume(sets), 0.0001)
    }

    @Test
    fun bestSetVolume_emptyInputIsZero() {
        assertEquals(0.0, PersonalRecordCalculator.bestSetVolume(emptyList()), 0.0001)
    }

    @Test
    fun bestSetVolume_nullFieldsContributeZero() {
        val sets = listOf(set("s1", weight = null, reps = 10), set("s2", weight = 50.0, reps = null))

        assertEquals(0.0, PersonalRecordCalculator.bestSetVolume(sets), 0.0001)
    }

    @Test
    fun bestSetVolume_allZeroSetsIsZeroNotNegative() {
        assertEquals(0.0, PersonalRecordCalculator.bestSetVolume(listOf(set("s1", 0.0, 0))), 0.0001)
    }

    // ---- hasPersonalRecord ----

    @Test
    fun hasPersonalRecord_trueWhenAnySetFlagged() {
        val sets = listOf(
            set("s1", 50.0, 10),
            set("s2", 80.0, 6, isPersonalRecord = true)
        )

        assertTrue(PersonalRecordCalculator.hasPersonalRecord(sets))
    }

    @Test
    fun hasPersonalRecord_falseWhenNoSetFlagged() {
        assertFalse(
            PersonalRecordCalculator.hasPersonalRecord(listOf(set("s1", 50.0, 10)))
        )
    }

    @Test
    fun hasPersonalRecord_falseForEmptyInput() {
        assertFalse(PersonalRecordCalculator.hasPersonalRecord(emptyList()))
    }
}
