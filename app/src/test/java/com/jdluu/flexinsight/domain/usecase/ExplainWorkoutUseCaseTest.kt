package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.fakes.FakeFlexAIClient
import com.jdluu.flexinsight.fakes.FakeFlexRepository
import com.jdluu.flexinsight.fakes.FakeWorkoutRepository
import com.jdluu.flexinsight.fakes.TestDefaults.exercise
import com.jdluu.flexinsight.fakes.TestDefaults.set
import com.jdluu.flexinsight.fakes.TestDefaults.workout
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplainWorkoutUseCaseTest {

    private val flexRepo = FakeFlexRepository()
    private val workoutRepo = FakeWorkoutRepository()
    private val aiClient = FakeFlexAIClient()
    private val accessor = mockk<HevyAiDataAccessor>()
    private val buildAiContext = BuildAiContextUseCase(accessor)

    private fun useCase() = ExplainWorkoutUseCase(flexRepo, workoutRepo, aiClient, buildAiContext)

    @Test
    fun `unknown workout id yields error and skips AI call`() = runTest {
        val result = useCase()("missing-id")

        assertTrue(result is Result.Error)
        assertEquals(
            ApiError.Unknown("Workout not found"),
            (result as Result.Error).error
        )
        assertEquals(0, aiClient.prompts.size)
        coVerify(exactly = 0) { accessor.buildContext(any()) }
    }

    @Test
    fun `prompt embeds context title exercises and set details`() = runTest {
        flexRepo.workoutByIdToReturn = workout("w1", name = "Push Day")
        workoutRepo.exercisesByWorkout["w1"] = listOf(exercise("e1", "w1", "Bench Press"))
        workoutRepo.setsByExercise["e1"] = listOf(
            set("s1", "e1", weight = 100.0, reps = 10, rpe = 8.0),
            set("s2", "e1", weight = 80.0, reps = 12, rpe = null)
        )
        coEvery { accessor.buildContext("Push Day") } returns HevyAiDataAccessor.ContextSnapshot(
            text = "AI CONTEXT",
            hasWorkoutData = true,
            hasApiKey = true,
            workoutCount = 1
        )

        val result = useCase()("w1")

        assertTrue(result is Result.Success)
        assertEquals(Result.Success("AI says hi"), result)
        val prompt = aiClient.prompts.single()
        assertEquals(
            "AI CONTEXT\n\n" +
                "Explain this completed workout:\n" +
                "Title: Push Day\n" +
                "Exercise: Bench Press\n" +
                "  - 100.0kg x 10 reps RPE 8.0\n" +
                "  - 80.0kg x 12 reps RPE -\n" +
                "\n\nProvide a concise analysis: highlights, progression, and one actionable tip.",
            prompt
        )
    }

    @Test
    fun `workout name is used as live history query`() = runTest {
        flexRepo.workoutByIdToReturn = workout("w1", name = "Legs")
        workoutRepo.exercisesByWorkout["w1"] = emptyList()
        coEvery { accessor.buildContext("Legs") } returns HevyAiDataAccessor.ContextSnapshot(
            text = "", hasWorkoutData = false, hasApiKey = true, workoutCount = 0
        )

        useCase()("w1")

        coVerify(exactly = 1) { accessor.buildContext("Legs") }
    }

    @Test
    fun `null workout name falls back to generic query`() = runTest {
        // NOTE: possibly unintended - the fallback string "workout" is used verbatim as an
        // exercise-history query token, which can match unrelated template names
        flexRepo.workoutByIdToReturn = workout("w1", name = null)
        workoutRepo.exercisesByWorkout["w1"] = emptyList()
        coEvery { accessor.buildContext("workout") } returns HevyAiDataAccessor.ContextSnapshot(
            text = "", hasWorkoutData = false, hasApiKey = true, workoutCount = 0
        )

        useCase()("w1")

        coVerify(exactly = 1) { accessor.buildContext("workout") }
    }

    @Test
    fun `multiple exercises render in stored order`() = runTest {
        flexRepo.workoutByIdToReturn = workout("w1", name = "W")
        workoutRepo.exercisesByWorkout["w1"] = listOf(
            exercise("e1", "w1", "Squat"),
            exercise("e2", "w1", "Plank")
        )
        workoutRepo.setsByExercise["e1"] = listOf(set("s1", "e1", weight = 140.0, reps = 5))
        workoutRepo.setsByExercise["e2"] = listOf(set("s2", "e2", weight = null, reps = null))
        coEvery { accessor.buildContext("W") } returns HevyAiDataAccessor.ContextSnapshot(
            text = "", hasWorkoutData = true, hasApiKey = true, workoutCount = 1
        )

        useCase()("w1")

        val prompt = aiClient.prompts.single()
        assertTrue(prompt.contains("Exercise: Squat"))
        // NOTE: possibly unintended - a bodyweight set renders literally as "nullkg x null reps"
        assertTrue(prompt.contains("  - nullkg x null reps RPE -"))
        assertTrue(prompt.indexOf("Squat") < prompt.indexOf("Plank"))
    }

    @Test
    fun `ai client error result is propagated unchanged`() = runTest {
        flexRepo.workoutByIdToReturn = workout("w1", name = "N")
        coEvery { accessor.buildContext("N") } returns HevyAiDataAccessor.ContextSnapshot(
            text = "", hasWorkoutData = false, hasApiKey = true, workoutCount = 0
        )
        aiClient.response = Result.Error(ApiError.Unknown("model busy"))

        val result = useCase()("w1")

        assertEquals(Result.Error(ApiError.Unknown("model busy")), result)
    }
}
