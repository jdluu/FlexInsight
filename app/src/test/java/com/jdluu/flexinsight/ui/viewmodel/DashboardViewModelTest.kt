package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.core.network.NetworkState
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncCoordinator
import com.jdluu.flexinsight.domain.usecase.CalculateTrainingLoadUseCase
import com.jdluu.flexinsight.domain.usecase.DetectDeloadUseCase
import com.jdluu.flexinsight.domain.usecase.GetMuscleRecoveryUseCase
import com.jdluu.flexinsight.domain.usecase.GetWeeklyProgressUseCase
import com.jdluu.flexinsight.domain.usecase.GetWorkoutStatsUseCase
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import com.jdluu.flexinsight.fakes.FakeWorkoutRepository
import com.jdluu.flexinsight.fakes.OneShotFakeAiClient
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.TestDefaults
import com.jdluu.flexinsight.fakes.healthConnectRepositoryStub
import com.jdluu.flexinsight.fakes.networkMonitorStub
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.fakes.widgetUpdaterStub
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import com.jdluu.flexinsight.widget.WidgetUpdater
import io.mockk.coVerify
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class DashboardViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var statsRepository: FakeStatsRepository
    private lateinit var userPreferences: UserPreferencesManager
    private lateinit var syncPreferences: SyncPreferencesManager
    private lateinit var aiClient: OneShotFakeAiClient
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var networkFlow: MutableStateFlow<NetworkState>
    private lateinit var widgetUpdater: WidgetUpdater

    @Before
    fun setUp() {

        repository = ScriptableFlexRepository()
        statsRepository = FakeStatsRepository().apply {
            statsToReturn = TestDefaults.emptyStats.copy(
                totalWorkouts = 7,
                currentStreak = 3,
                longestStreak = 5
            )
            weeklyProgressToReturn = List(4) { week ->
                WeeklyProgress(
                    weekStartDate = week.toLong(),
                    totalVolume = 1000.0,
                    workoutCount = 2,
                    averageVolume = 500.0
                )
            }
            recoveryToReturn = mapOf(MuscleGroup.CHEST to 0.8f)
        }
        repository.profileInfo = ProfileInfo("Jay", 1000L, true, 7, 30)
        repository.recentWorkouts = listOf(TestDefaults.workout("w1", startTime = 2000L))
        repository.plannedForDay = { listOf(plannedWorkout("Push Day")) }

        val context = ApplicationProvider.getApplicationContext<Context>()
        userPreferences = UserPreferencesManager(context)
        syncPreferences = SyncPreferencesManager(context)
        kotlinx.coroutines.runBlocking {
            userPreferences.resetForTests()
            syncPreferences.clearAllForTests()
        }

        aiClient = OneShotFakeAiClient(available = false)
        val (monitor, flow) = networkMonitorStub(NetworkState.Unknown)
        networkMonitor = monitor
        networkFlow = flow
        widgetUpdater = widgetUpdaterStub()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(): DashboardViewModel = DashboardViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        userPreferencesManager = userPreferences,
        aiClient = aiClient,
        getWorkoutStatsUseCase = GetWorkoutStatsUseCase(statsRepository),
        getWeeklyProgressUseCase = GetWeeklyProgressUseCase(statsRepository),
        getMuscleRecoveryUseCase = GetMuscleRecoveryUseCase(statsRepository),
        calculateTrainingLoadUseCase = CalculateTrainingLoadUseCase(repository, healthConnectRepositoryStub()),
        detectDeloadUseCase = DetectDeloadUseCase(repository, healthConnectRepositoryStub()),
        syncPreferencesManager = syncPreferences,
        syncCoordinator = SyncCoordinator(FakeWorkoutRepository(), syncPreferences, healthConnectRepositoryStub()),
        widgetUpdater = widgetUpdater
    )

    // region Initialization

    @Test
    fun `init loads dashboard data and pushes widget metrics`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals("w1", state.latestWorkout?.id)
            assertEquals("Jay", state.profileInfo?.displayName)
            assertEquals(3, state.currentStreak)
            assertEquals(7, state.workoutStats?.totalWorkouts)
            assertEquals(4, state.weeklyProgress.size)
            assertEquals(0.8f, state.muscleRecovery[MuscleGroup.CHEST])
            assertNotNull(state.trainingLoad)
            assertFalse(state.deloadAlert?.shouldDeload ?: true)
            assertTrue(aiClient.prompts.isEmpty())
        }

        coVerify(exactly = 1) {
            widgetUpdater.updateFromDashboard(streak = 3, recoveryScore = 80, nextWorkoutLabel = "Push Day")
        }
    }

    @Test
    fun `init surfaces error state when recent workouts cannot be read`() = runVmTest {
        repository.recentWorkoutsError = IOException("offline")
        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState is LoadingState.Error }
            assertTrue(state.error is UiError.Network)
            assertEquals("Unable to connect to server", state.error?.message)
        }
    }

    @Test
    fun `daily insight is generated when ai client is available`() = runVmTest {
        aiClient.available = true
        aiClient.response = Result.Success("Keep the streak alive!")
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.uiState.test {
            val state = nextMatching { it.dailyInsight != null }
            assertEquals("Keep the streak alive!", state.dailyInsight)
            assertFalse(state.isGeneratingInsight)
        }
        assertEquals(1, aiClient.prompts.size)
        assertTrue(aiClient.prompts.single().contains("3 day streak"))
    }

    @Test
    fun `network state changes are reflected in ui state`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        networkFlow.value = NetworkState.Unavailable

        viewModel.uiState.test {
            val state = nextMatching { it.networkState is NetworkState.Unavailable }
            assertEquals(NetworkState.Unavailable, state.networkState)
        }
    }

    @Test
    fun `units preference changes are reflected in ui state`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        userPreferences.setUnits("Metric")

        awaitUntil { viewModel.uiState.value.units == "Metric" }
        assertEquals("Metric", viewModel.uiState.value.units)
    }

    // endregion

    // region Actions

    @Test
    fun `refresh reloads dashboard data`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        repository.recentWorkouts = listOf(TestDefaults.workout("w2", startTime = 3000L))
        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        viewModel.uiState.test {
            nextMatching { it.latestWorkout?.id == "w2" && it.loadingState == LoadingState.Success }
        }
    }

    @Test
    fun `sync success settles state, notifies coordinator, and reloads`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)
        val gate = CompletableDeferred<Unit>()
        repository.syncGate = gate

        viewModel.sync()

        viewModel.uiState.test {
            val syncing = nextMatching { it.isSyncing }
            assertEquals(LoadingState.Loading, syncing.loadingState)

            gate.complete(Unit)

            val settled = nextMatching { !it.isSyncing && it.loadingState == LoadingState.Success }
            assertEquals(LoadingState.Success, settled.loadingState)
            assertFalse(settled.isSyncing)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, repository.syncCalls)
        awaitUntil { syncPreferences.lastSyncAtFlow.firstOrNull() != null }
    }

    @Test
    fun `sync failure surfaces banner error without notifying coordinator`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)
        repository.syncResult = Result.Error(ApiError.NetworkError.NoConnection)

        viewModel.sync()

        viewModel.uiState.test {
            val settled = nextMatching { !it.isSyncing && it.loadingState is LoadingState.Error }
            assertTrue(settled.error is UiError.Network)
        }
        assertEquals(1, repository.syncCalls)

        withContext(Dispatchers.IO) { delay(100) }
        assertNull(syncPreferences.getLastSyncAt())
    }

    // endregion

    // region Helpers

    private fun plannedWorkout(name: String) = PlannedWorkout(
        id = name,
        name = name,
        duration = 45L,
        intensity = "High Intensity",
        isCompleted = false,
        routineId = "routine-1",
        exerciseCount = 5
    )

    /** Consumes emissions until one satisfies [predicate]; does not cancel the turbine. */
    private suspend fun <T> TurbineTestContext<T>.nextMatching(predicate: (T) -> Boolean): T {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
        return item
    }

    /** Resolves the init delay and waits for the initial load to settle. */
    private suspend fun awaitInitialLoad(
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        viewModel: DashboardViewModel
    ) {
        scheduler.advanceUntilIdle()
        awaitUntil("initial load") {
            viewModel.uiState.value.loadingState != LoadingState.Loading
        }
    }

    /** Polls with real time so DataStore-backed flows (real IO threads) can progress. */
    private suspend fun awaitUntil(
        label: String = "condition",
        timeoutMs: Long = 10_000,
        condition: suspend () -> Boolean
    ) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("$label not met within ${timeoutMs}ms")
            }
            withContext(Dispatchers.IO) { delay(10) }
        }
    }

    // endregion

    /**
     * Runs [block] with Dispatchers.Main bound to an [UnconfinedTestDispatcher] sharing
     * this test's scheduler, so view-model coroutines (including init delays) resolve
     * on the same virtual clock the assertions observe.
     */
    private fun runVmTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
        kotlinx.coroutines.test.runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                block()
            } finally {
                Dispatchers.resetMain()
            }
        }

}
