package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMuscleRecoveryUseCaseTest {

    @Test
    fun `returns recovery map from repository`() = runTest {
        val repo = FakeStatsRepository()
        val recovery = mapOf(
            MuscleGroup.CHEST to 0.25f,
            MuscleGroup.BACK to 0.9f,
            MuscleGroup.LEGS to 1.0f
        )
        repo.recoveryToReturn = recovery
        val useCase = GetMuscleRecoveryUseCase(repo)

        assertEquals(recovery, useCase())
    }

    @Test
    fun `empty recovery map is returned when no data`() = runTest {
        val useCase = GetMuscleRecoveryUseCase(FakeStatsRepository())

        assertEquals(emptyMap<MuscleGroup, Float>(), useCase())
    }
}
