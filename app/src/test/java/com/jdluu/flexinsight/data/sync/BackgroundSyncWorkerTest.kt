package com.jdluu.flexinsight.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.fakes.TestDefaults
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class BackgroundSyncWorkerTest {

    private lateinit var syncPreferences: RecordingSyncPreferences
    private lateinit var healthConnectRepository: HealthConnectRepository

    @Before
    fun setUp() {
        syncPreferences = RecordingSyncPreferences()
        healthConnectRepository = mockk(relaxed = true)
    }

    private fun buildWorker(syncSource: HevySyncSource): BackgroundSyncWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workoutRepository = RecordingWorkoutRepository(
            workoutCount = 3,
            recentWorkouts = listOf(TestDefaults.workout("w1"), TestDefaults.workout("w2"))
        )
        val coordinator = SyncCoordinator(workoutRepository, syncPreferences.manager, healthConnectRepository)
        return TestListenableWorkerBuilder<BackgroundSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return BackgroundSyncWorker(appContext, workerParameters, syncSource, coordinator)
                }
            })
            .build()
    }

    @Test
    fun `success path runs post-sync follow-up and records success`() = runTest {
        val source = FakeHevySyncSource(Result.Success(Unit))
        val worker = buildWorker(source)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, source.callCount)
        assertEquals(listOf(3), syncPreferences.recordedCounts)
        coVerify(exactly = 1) { healthConnectRepository.writeWorkoutsToHealthConnect(any()) }
    }

    @Test
    fun `auth error fails without recording sync`() = runTest {
        val source = FakeHevySyncSource(Result.Error(ApiError.AuthError.InvalidApiKey))
        val worker = buildWorker(source)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(1, source.callCount)
        assertTrue(syncPreferences.recordedCounts.isEmpty())
        coVerify(exactly = 0) { healthConnectRepository.writeWorkoutsToHealthConnect(any()) }
    }

    @Test
    fun `offline error requests retry without crashing or recording`() = runTest {
        val source = FakeHevySyncSource(Result.Error(ApiError.NetworkError.NoConnection))
        val worker = buildWorker(source)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        assertEquals(1, source.callCount)
        assertTrue(syncPreferences.recordedCounts.isEmpty())
        coVerify(exactly = 0) { healthConnectRepository.writeWorkoutsToHealthConnect(any()) }
    }

    @Test
    fun `unknown non-retryable error fails without crashing or recording`() = runTest {
        val source = FakeHevySyncSource(Result.Error(ApiError.Unknown("boom")))
        val worker = buildWorker(source)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(1, source.callCount)
        assertTrue(syncPreferences.recordedCounts.isEmpty())
        coVerify(exactly = 0) { healthConnectRepository.writeWorkoutsToHealthConnect(any()) }
    }

    @Test
    fun `server error requests retry`() = runTest {
        val source = FakeHevySyncSource(Result.Error(ApiError.ServerError.InternalServerError))
        val worker = buildWorker(source)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        assertTrue(syncPreferences.recordedCounts.isEmpty())
    }
}
