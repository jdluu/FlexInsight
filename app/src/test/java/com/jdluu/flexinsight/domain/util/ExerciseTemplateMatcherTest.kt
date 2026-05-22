package com.jdluu.flexinsight.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseTemplateMatcherTest {

    private val mapping = mapOf(
        "bench-id" to "Bench Press (Barbell)",
        "squat-id" to "Squat (Barbell)"
    )

    @Test
    fun resolveTemplateId_exactMatch() {
        assertEquals("bench-id", ExerciseTemplateMatcher.resolveTemplateId("Bench Press (Barbell)", mapping))
    }

    @Test
    fun resolveTemplateId_partialMatch() {
        assertEquals("squat-id", ExerciseTemplateMatcher.resolveTemplateId("Squat", mapping))
    }

    @Test
    fun resolveTemplateId_returnsNullWhenUnknown() {
        assertNull(ExerciseTemplateMatcher.resolveTemplateId("Lat Pulldown", mapping))
    }
}
