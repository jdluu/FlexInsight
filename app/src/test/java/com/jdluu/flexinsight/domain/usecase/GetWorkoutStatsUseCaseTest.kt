package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.fakes.FakeStatsRepository
import com.jdluu.flexinsight.fakes.TestDefaults
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GetWorkoutStatsUseCaseTest {

    @Test
    fun `returns stats calculated by repository`() = runTest {
        val repo = FakeStatsRepository()
        val stats = TestDefaults.emptyStats.copy(totalWorkouts = 7, totalVolume = 4321.0)
        repo.statsToReturn = stats
        val useCase = GetWorkoutStatsUseCase(repo)

        val result = useCase()

        assertSame(stats, result)
    }

    @Test
    fun `empty repository data yields zeroed stats`() = runTest {
        val useCase = GetWorkoutStatsUseCase(FakeStatsRepository())

        val result = useCase()

        assertEquals(TestDefaults.emptyStats, result)
    }
}
