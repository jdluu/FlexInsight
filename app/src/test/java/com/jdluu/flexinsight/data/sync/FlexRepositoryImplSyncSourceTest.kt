package com.jdluu.flexinsight.data.sync

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.cache.CacheManager
import com.jdluu.flexinsight.data.preferences.ApiKeyManager
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.data.repository.ExerciseRepository
import com.jdluu.flexinsight.data.repository.FlexRepositoryImpl
import com.jdluu.flexinsight.data.repository.RoutineRepository
import com.jdluu.flexinsight.data.repository.StatsRepository
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlexRepositoryImplSyncSourceTest {

    private fun buildSyncSource(
        templates: Result<Map<String, String>> = Result.Success(emptyMap()),
        workouts: Result<Unit> = Result.Success(Unit),
        routines: Result<Unit> = Result.Success(Unit)
    ): Pair<HevySyncSource, StatsRepository> {
        val exerciseRepository = mockk<ExerciseRepository>()
        coEvery { exerciseRepository.getExerciseTemplateMapping() } returns templates
        val workoutRepository = mockk<WorkoutRepository>()
        coEvery { workoutRepository.syncWorkouts() } returns workouts
        val routineRepository = mockk<RoutineRepository>()
        coEvery { routineRepository.syncRoutines() } returns routines
        val statsRepository = mockk<StatsRepository>(relaxed = true)

        val impl = FlexRepositoryImpl(
            apiKeyManager = mockk<ApiKeyManager>(),
            networkMonitor = mockk<NetworkMonitor>(),
            cacheManager = CacheManager(),
            exerciseRepository = exerciseRepository,
            workoutRepository = workoutRepository,
            routineRepository = routineRepository,
            statsRepository = statsRepository
        )
        return impl to statsRepository
    }

    @Test
    fun `syncAll succeeds and invalidates stats cache when all sub-syncs succeed`() = runTest {
        val (source, stats) = buildSyncSource()

        val result = source.syncAll()

        assertTrue(result is Result.Success)
        verify(exactly = 1) { stats.invalidateStatsCache() }
    }

    @Test
    fun `syncAll propagates single sub-sync error unchanged`() = runTest {
        val (source, stats) = buildSyncSource(workouts = Result.Error(ApiError.NetworkError.NoConnection))

        val result = source.syncAll()

        assertTrue(result is Result.Error)
        assertEquals("No internet connection available", (result as Result.Error).error.message)
        verify(exactly = 0) { stats.invalidateStatsCache() }
    }

    @Test
    fun `syncAll aggregates multiple sub-sync errors`() = runTest {
        val (source, stats) = buildSyncSource(
            templates = Result.Error(ApiError.NetworkError.ConnectionError()),
            routines = Result.Error(ApiError.ServerError.InternalServerError)
        )

        val result = source.syncAll()

        assertTrue(result is Result.Error)
        val message = (result as Result.Error).error.message!!
        assertTrue(message.startsWith("Sync failed:"))
        assertTrue(message.contains("Unable to connect to server"))
        assertTrue(message.contains("Internal server error"))
        verify(exactly = 0) { stats.invalidateStatsCache() }
    }
}
