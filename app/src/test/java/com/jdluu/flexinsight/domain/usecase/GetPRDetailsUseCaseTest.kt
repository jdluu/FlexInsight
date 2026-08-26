package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPRDetailsUseCaseTest {

    private fun pr(name: String, weight: Double) = PRDetails(
        exerciseName = name,
        date = 1700000000000L,
        muscleGroup = "Chest",
        weight = weight,
        workoutId = "w1",
        setId = "s1"
    )

    @Test
    fun `passes limit through and returns repository data`() = runTest {
        val repo = FakeStatsRepository()
        val prs = listOf(pr("Bench Press", 100.0), pr("Squat", 140.0))
        repo.prsToReturn = prs
        val useCase = GetPRDetailsUseCase(repo)

        val result = useCase(limit = 5)

        assertEquals(listOf(5), repo.requestedPrLimits)
        assertEquals(prs, result)
    }

    @Test
    fun `no PRs returns empty list`() = runTest {
        val useCase = GetPRDetailsUseCase(FakeStatsRepository())

        assertEquals(emptyList<PRDetails>(), useCase(limit = 10))
    }

    @Test
    fun `zero limit value is forwarded unchanged`() = runTest {
        val repo = FakeStatsRepository()
        val useCase = GetPRDetailsUseCase(repo)

        useCase(limit = 0)

        assertEquals(listOf(0), repo.requestedPrLimits)
    }
}
