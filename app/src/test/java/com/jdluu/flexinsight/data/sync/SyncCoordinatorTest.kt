package com.jdluu.flexinsight.data.sync

import com.jdluu.flexinsight.domain.health.HealthConnectDataSource
import com.jdluu.flexinsight.fakes.TestDefaults
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorTest {

    @Test
    fun `onSyncComplete records workout count and writes recent workouts to Health Connect`() = runTest {
        val syncPreferences = RecordingSyncPreferences()
        val healthConnectDataSource = mockk<HealthConnectDataSource>(relaxed = true)
        val recent = listOf(
            TestDefaults.workout("w1", startTime = 100L),
            TestDefaults.workout("w2", startTime = 200L)
        )
        val coordinator = SyncCoordinator(
            workoutRepository = RecordingWorkoutRepository(workoutCount = 7, recentWorkouts = recent),
            syncPreferencesManager = syncPreferences.manager,
            healthConnectDataSource = healthConnectDataSource
        )

        coordinator.onSyncComplete()

        assertEquals(listOf(7), syncPreferences.recordedCounts)
        coVerify(exactly = 1) { healthConnectDataSource.writeWorkoutsToHealthConnect(recent) }
    }

    @Test
    fun `onSyncComplete with no data records zero count`() = runTest {
        val syncPreferences = RecordingSyncPreferences()
        val healthConnectDataSource = mockk<HealthConnectDataSource>(relaxed = true)
        val coordinator = SyncCoordinator(
            workoutRepository = RecordingWorkoutRepository(),
            syncPreferencesManager = syncPreferences.manager,
            healthConnectDataSource = healthConnectDataSource
        )

        coordinator.onSyncComplete()

        assertEquals(listOf(0), syncPreferences.recordedCounts)
        coVerify(exactly = 1) { healthConnectDataSource.writeWorkoutsToHealthConnect(emptyList()) }
    }
}
