package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWeeklyProgressUseCaseTest {

    @Test
    fun `passes weeks argument through to repository`() = runTest {
        val repo = FakeStatsRepository()
        repo.weeklyProgressToReturn = listOf(
            WeeklyProgress(weekStartDate = 0L, totalVolume = 1000.0, workoutCount = 2, averageVolume = 500.0),
            WeeklyProgress(weekStartDate = 604800000L, totalVolume = 2000.0, workoutCount = 3, averageVolume = 666.67)
        )
        val useCase = GetWeeklyProgressUseCase(repo)

        val result = useCase(weeks = 8)

        assertEquals(listOf(8), repo.requestedWeeklyProgressWeeks)
        assertEquals(2, result.size)
        assertEquals(2000.0, result[1].totalVolume, 0.001)
    }

    @Test
    fun `empty data returns empty list`() = runTest {
        val useCase = GetWeeklyProgressUseCase(FakeStatsRepository())

        assertEquals(emptyList<WeeklyProgress>(), useCase(weeks = 4))
    }

    @Test
    fun `zero weeks value is forwarded unchanged`() = runTest {
        val repo = FakeStatsRepository()
        val useCase = GetWeeklyProgressUseCase(repo)

        useCase(weeks = 0)

        assertEquals(listOf(0), repo.requestedWeeklyProgressWeeks)
    }
}
