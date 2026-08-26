package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BuildAiContextUseCaseTest {

    private fun snapshot(text: String, usesLive: Boolean = false) = HevyAiDataAccessor.ContextSnapshot(
        text = text,
        hasWorkoutData = true,
        hasApiKey = true,
        workoutCount = 42,
        usesLiveExerciseHistory = usesLive
    )

    private class FakeContextProvider : AiContextProvider {
        var lastQuery: String? = null
        lateinit var response: HevyAiDataAccessor.ContextSnapshot

        override suspend fun buildContext(userQuery: String?): HevyAiDataAccessor.ContextSnapshot {
            lastQuery = userQuery
            return response
        }
    }

    @Test
    fun `delegates to provider with null query by default`() = runTest {
        val provider = FakeContextProvider()
        val expected = snapshot("context text")
        provider.response = expected

        val result = BuildAiContextUseCase(provider)()

        assertEquals(expected, result)
        assertEquals(null, provider.lastQuery)
    }

    @Test
    fun `forwards non-null user query to provider`() = runTest {
        val provider = FakeContextProvider()
        val expected = snapshot("live history context", usesLive = true)
        provider.response = expected

        val result = BuildAiContextUseCase(provider)(userQuery = "how is my bench?")

        assertSame(expected, result)
        assertEquals("how is my bench?", provider.lastQuery)
    }
}
