package com.jdluu.flexinsight.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRoutineParserTest {

    private val resolver: (String) -> String? = { name ->
        when {
            name.equals("Bench Press", ignoreCase = true) -> "bench-id"
            name.equals("Squat", ignoreCase = true) -> "squat-id"
            else -> null
        }
    }

    @Test
    fun parse_matchesRecognizedExercises() {
        val plan = """
            1. Bench Press - 3 x 8
            2. Squat - 4 x 5
        """.trimIndent()

        val result = AiRoutineParser.parse(plan, "Push Day", resolver)

        assertEquals(listOf("Bench Press", "Squat"), result.matchedExercises)
        assertTrue(result.unmatchedExercises.isEmpty())
        assertFalse(result.usedPlaceholder)
        assertEquals(2, result.request.routine.exercises.size)
        assertEquals("bench-id", result.request.routine.exercises[0].exerciseTemplateId)
    }

    @Test
    fun parse_collectsUnmatchedExerciseNames() {
        val plan = """
            Bench Press - 3 x 8
            Mystery Curl - 3 x 12
        """.trimIndent()

        val result = AiRoutineParser.parse(plan, "Arms", resolver)

        assertEquals(listOf("Bench Press"), result.matchedExercises)
        assertEquals(listOf("Mystery Curl"), result.unmatchedExercises)
        assertFalse(result.usedPlaceholder)
        assertEquals(1, result.request.routine.exercises.size)
    }

    @Test
    fun parse_usesPlaceholderWhenNoLinesMatch() {
        val result = AiRoutineParser.parse("Rest day — stretch only", "Recovery", resolver)

        assertTrue(result.usedPlaceholder)
        assertTrue(result.matchedExercises.isEmpty())
        assertTrue(result.unmatchedExercises.isEmpty())
        assertEquals("0", result.request.routine.exercises.single().exerciseTemplateId)
    }

    @Test
    fun parse_usesPlaceholderWhenAllLinesUnmatched() {
        val plan = """
            Mystery Press - 3 x 8
            Unknown Row - 4 x 6
        """.trimIndent()

        val result = AiRoutineParser.parse(plan, "Custom", resolver)

        assertTrue(result.usedPlaceholder)
        assertEquals(listOf("Mystery Press", "Unknown Row"), result.unmatchedExercises)
        assertTrue(result.matchedExercises.isEmpty())
    }
}
