package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMuscleGroupProgressUseCaseTest {

    @Test
    fun `passes weeks argument through and returns repository data`() = runTest {
        val repo = FakeStatsRepository()
        val data = listOf(
            MuscleGroupProgress(muscleGroup = "Chest", volume = 5000.0, sets = 20, intensity = "HI"),
            MuscleGroupProgress(muscleGroup = "Back", volume = 3000.0, sets = 15, intensity = "MD")
        )
        repo.muscleGroupProgressToReturn = data
        val useCase = GetMuscleGroupProgressUseCase(repo)

        val result = useCase(weeks = 6)

        assertEquals(listOf(6), repo.requestedMuscleGroupWeeks)
        assertEquals(data, result)
    }

    @Test
    fun `single item list is returned as-is`() = runTest {
        val repo = FakeStatsRepository()
        val single = listOf(
            MuscleGroupProgress(muscleGroup = "Legs", volume = 12000.0, sets = 40, intensity = "LO")
        )
        repo.muscleGroupProgressToReturn = single

        assertEquals(single, GetMuscleGroupProgressUseCase(repo)(weeks = 1))
    }

    @Test
    fun `no data returns empty list`() = runTest {
        val useCase = GetMuscleGroupProgressUseCase(FakeStatsRepository())

        assertEquals(emptyList<MuscleGroupProgress>(), useCase(weeks = 4))
    }
}
