package com.jdluu.flexinsight.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseTemplateMatcherTest {

    private val mapping = linkedMapOf(
        "bench-id" to "Bench Press (Barbell)",
        "squat-id" to "Squat (Barbell)"
    )

    // ---- Guard clauses ----

    @Test
    fun `blank exercise name returns null`() {
        assertNull(ExerciseTemplateMatcher.resolveTemplateId("", mapping))
        assertNull(ExerciseTemplateMatcher.resolveTemplateId("   ", mapping))
    }

    @Test
    fun `empty lookup map returns null`() {
        assertNull(ExerciseTemplateMatcher.resolveTemplateId("Bench Press", emptyMap()))
    }

    @Test
    fun `unknown exercise returns null`() {
        assertNull(ExerciseTemplateMatcher.resolveTemplateId("Lat Pulldown", mapping))
    }

    // ---- Exact matching ----

    @Test
    fun `exact match ignoring case resolves template id`() {
        assertEquals(
            "bench-id",
            ExerciseTemplateMatcher.resolveTemplateId("BENCH PRESS (BARBELL)", mapping)
        )
    }

    @Test
    fun `surrounding whitespace is trimmed before matching`() {
        assertEquals("squat-id", ExerciseTemplateMatcher.resolveTemplateId("  Squat (Barbell) ", mapping))
    }

    @Test
    fun `exact match wins regardless of map insertion order`() {
        val reversed = linkedMapOf(
            "other-id" to "Incline Bench Press",
            "bench-id" to "Bench Press"
        )
        assertEquals("bench-id", ExerciseTemplateMatcher.resolveTemplateId("Bench Press", reversed))
    }

    // ---- Substring matching ----

    @Test
    fun `query containing template name resolves`() {
        assertEquals("squat-id", ExerciseTemplateMatcher.resolveTemplateId("Squat", mapping))
        assertEquals("bench-id", ExerciseTemplateMatcher.resolveTemplateId("Paused Bench Press (Barbell)", mapping))
    }

    @Test
    fun `template name contained inside longer query resolves either direction`() {
        assertEquals("bench-id", ExerciseTemplateMatcher.resolveTemplateId("Barbell", mapping))
    }

    @Test
    fun `substring matching is case insensitive in both directions`() {
        assertEquals("bench-id", ExerciseTemplateMatcher.resolveTemplateId("bench press", mapping))
    }

    @Test
    fun `first substring hit in insertion order wins`() {
        val many = linkedMapOf(
            "a-id" to "Press",
            "b-id" to "Bench"
        )
        // "Press" is encountered first and query.contains("Press") holds
        assertEquals("a-id", ExerciseTemplateMatcher.resolveTemplateId("Bench Press", many))
    }

    // ---- Quirks worth pinning down ----

    @Test
    fun `empty template title acts as wildcard substring match`() {
        // Exact matching runs first, so a true exact hit still wins over an empty title:
        assertEquals("bench-id", ExerciseTemplateMatcher.resolveTemplateId("Bench Press", linkedMapOf("weird-id" to "", "bench-id" to "Bench Press")))
        // For non-exact queries the ""-title entry matches everything via contains("")
        // NOTE: possibly unintended - an empty stored title shadows real templates on substring passes
        val withEmptyTitle = linkedMapOf(
            "weird-id" to "",
            "bench-id" to "Bench Press"
        )
        assertEquals("weird-id", ExerciseTemplateMatcher.resolveTemplateId("Incline Bench Press", withEmptyTitle))
    }

    @Test
    fun `single word query matches template by prefix containment`() {
        val templates = linkedMapOf("dl-id" to "Deadlift")
        assertEquals("dl-id", ExerciseTemplateMatcher.resolveTemplateId("deadlift", templates))
        // Neither direction contains the other
        assertNull(ExerciseTemplateMatcher.resolveTemplateId("Row", templates))
    }
}
