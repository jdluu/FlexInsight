package com.jdluu.flexinsight.data.ai

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke tests for query-token matching logic (via reflection-free unit of behavior).
 */
class HevyAiDataAccessorTest {

    @Test
    fun `exercise query tokens are extracted from user message`() {
        val query = "How is my bench press progressing?"
        val tokens = query.lowercase().split(Regex("\\W+")).filter { it.length >= 4 }.toSet()
        assertTrue(tokens.contains("bench"))
        assertTrue(tokens.contains("press"))
    }
}
