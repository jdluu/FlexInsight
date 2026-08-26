package com.jdluu.flexinsight.ui.viewmodel

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.data.model.PeriodComparison
import com.jdluu.flexinsight.domain.usecase.CompareRoutineSessionsUseCase
import com.jdluu.flexinsight.domain.usecase.GetPRDetailsUseCase
import com.jdluu.flexinsight.domain.usecase.GetWeeklyProgressUseCase
import com.jdluu.flexinsight.domain.usecase.GetWorkoutStatsUseCase
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import com.jdluu.flexinsight.fakes.FakeWorkoutRepository
import com.jdluu.flexinsight.fakes.OneShotFakeAiClient
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.StaticAiContextProvider
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.fakes.TestDefaults
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class HistoryViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var statsRepository: FakeStatsRepository
    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var userPreferences: UserPreferencesManager
    private lateinit var aiClient: OneShotFakeAiClient

    private val now = System.currentTimeMillis()
    private val dayMs = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {

        repository = ScriptableFlexRepository().apply {
            profileInfo = com.jdluu.flexinsight.data.model.ProfileInfo(null, null, false, 7, 30)
            workouts = listOf(
                TestDefaults.workout("w-new", name = "Push Day", startTime = now - dayMs, routineId = "r1"),
                TestDefaults.workout("w-old", name = "Push Day", startTime = now - 8 * dayMs, routineId = "r1"),
                TestDefaults.workout("w-ancient", name = "Legs", startTime = now - 40 * dayMs)
            )
            periodComparison = PeriodComparison(
                currentPeriodLabel = "This Month",
                previousPeriodLabel = "Last Month",
                totalVolumeCurrent = 12000.0,
                totalVolumePrevious = 10000.0,
                totalWorkoutsCurrent = 8,
                totalWorkoutsPrevious = 7,
                avgDurationCurrent = 45L,
                avgDurationPrevious = 50L
            )
        }
        statsRepository = FakeStatsRepository().apply {
            statsToReturn = TestDefaults.emptyStats.copy(totalVolume = 120000.0, currentStreak = 4)
            prsToReturn = listOf(
                PRDetails("Bench Press", now, "Chest", 100.0, "w-new", "s1")
            )
        }

        // Two sessions of the same routine so comparisons have data to work with.
        val recentExercise = TestDefaults.exercise("e-new", "w-new", name = "Bench Press")
        val priorExercise = TestDefaults.exercise("e-old", "w-old", name = "Bench Press")
        workoutRepository = FakeWorkoutRepository().apply {
            workoutsFlow.value = listOf(
                TestDefaults.workout("w-new", startTime = now - dayMs, routineId = "r1"),
                TestDefaults.workout("w-old", startTime = now - 8 * dayMs, routineId = "r1"),
                TestDefaults.workout("w2-new", startTime = now - 2 * dayMs, routineId = "r2"),
                TestDefaults.workout("w2-old", startTime = now - 9 * dayMs, routineId = "r2")
            )
            exercisesByWorkout["w-new"] = listOf(recentExercise)
            exercisesByWorkout["w-old"] = listOf(priorExercise)
            setsByExercise["e-new"] = listOf(TestDefaults.set("s1", "e-new", weight = 120.0, reps = 5))
            setsByExercise["e-old"] = listOf(TestDefaults.set("s2", "e-old", weight = 100.0, reps = 5))
            exercisesByWorkout["w2-new"] = listOf(TestDefaults.exercise("e2-new", "w2-new", name = "Barbell Row"))
            exercisesByWorkout["w2-old"] = listOf(TestDefaults.exercise("e2-old", "w2-old", name = "Barbell Row"))
            setsByExercise["e2-new"] = listOf(TestDefaults.set("s3", "e2-new", weight = 80.0, reps = 8))
            setsByExercise["e2-old"] = listOf(TestDefaults.set("s4", "e2-old", weight = 70.0, reps = 8))
        }

        aiClient = OneShotFakeAiClient(available = false)
        userPreferences = UserPreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        kotlinx.coroutines.runBlocking { userPreferences.resetForTests() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(): HistoryViewModel = HistoryViewModel(
        repository = repository,
        userPreferencesManager = userPreferences,
        aiClient = aiClient,
        getWorkoutStatsUseCase = GetWorkoutStatsUseCase(statsRepository),
        getPRDetailsUseCase = GetPRDetailsUseCase(statsRepository),
        getWeeklyProgressUseCase = GetWeeklyProgressUseCase(statsRepository),
        compareRoutineSessionsUseCase = CompareRoutineSessionsUseCase(workoutRepository)
    )

    // region Initialization

    @Test
    fun `init loads history with stats, PRs, comparison data and routine comparison`() = runVmTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals(3, state.allWorkouts.size)
            assertEquals(3, state.workouts.size)
            assertEquals(7, state.workoutCount)
            assertEquals(120000.0, state.workoutStats?.totalVolume)
            assertEquals(1, state.prsWithDetails.size)
            assertEquals("This Month", state.compareData?.currentPeriodLabel)
            assertEquals(8, state.compareData?.totalWorkoutsCurrent)
            assertNotNull(state.routineComparison)
            assertTrue(state.routineComparison!!.summary.contains("improved"))
            assertNull(state.error)
            assertFalse(state.isGeneratingTrend)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(aiClient.prompts.isEmpty())
    }

    @Test
    fun `init falls back to defaults when optional data fails to load`() = runVmTest {
        repository.recentPrsError = RuntimeException("recent pr boom")
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals(LoadingState.Success, state.loadingState)
            assertEquals(4, state.workoutStats?.currentStreak)
            assertEquals(1, state.prsWithDetails.size)
            assertTrue(state.recentPRs.isEmpty())
            assertNull(state.error)
        }
    }

    @Test
    fun `init surfaces error state when the workout stream fails`() = runVmTest {
        repository.workoutsError = IOException("database gone")
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState is LoadingState.Error }
            assertTrue(state.error is UiError.Network)
            assertEquals("Unable to connect to server", state.error?.message)
            assertTrue(state.workouts.isEmpty())
        }
    }

    @Test
    fun `init generates trend analysis when ai client is available`() = runVmTest {
        aiClient.available = true
        aiClient.response = Result.Success("You are consistent!")
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            val state = nextMatching { it.aiTrendAnalysis != null }
            assertEquals("You are consistent!", state.aiTrendAnalysis)
            assertFalse(state.isGeneratingTrend)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, aiClient.prompts.size)
        val prompt = aiClient.prompts.single()
        assertTrue(prompt.contains("Total workouts: 7"))
        assertTrue(prompt.contains("Streak: 4 days"))
    }

    // endregion

    // region Filters

    @Test
    fun `date filter narrows visible workouts`() = runVmTest {
        val viewModel = buildViewModel()
        awaitSuccess(viewModel)

        viewModel.setDateFilter("This Week")

        viewModel.uiState.test {
            val state = nextMatching { it.dateFilter == "This Week" }
            assertEquals(listOf("w-new"), state.workouts.map { it.id })
            assertEquals(3, state.allWorkouts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all time filter restores every workout`() = runVmTest {
        val viewModel = buildViewModel()
        awaitSuccess(viewModel)
        viewModel.setDateFilter("This Week")

        viewModel.setDateFilter("All Time")

        viewModel.uiState.test {
            val state = nextMatching { it.dateFilter == "All Time" }
            assertEquals(3, state.workouts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `muscle group filter matches workout names case-insensitively`() = runVmTest {
        val viewModel = buildViewModel()
        awaitSuccess(viewModel)

        viewModel.setMuscleGroupFilter("legs")

        viewModel.uiState.test {
            val state = nextMatching { it.muscleGroupFilter == "legs" }
            assertEquals(listOf("w-ancient"), state.workouts.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.setMuscleGroupFilter(null)

        viewModel.uiState.test {
            val state = nextMatching { it.muscleGroupFilter == null }
            assertEquals(3, state.workouts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `combined date and muscle group filters intersect`() = runVmTest {
        val viewModel = buildViewModel()
        awaitSuccess(viewModel)

        viewModel.setMuscleGroupFilter("Push")
        viewModel.setDateFilter("This Week")

        viewModel.uiState.test {
            val state = nextMatching { it.dateFilter == "This Week" }
            assertEquals(listOf("w-new"), state.workouts.map { it.id })
            assertEquals("Push", state.muscleGroupFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Actions

    @Test
    fun `refresh picks up newly synced workouts`() = runVmTest {
        val viewModel = buildViewModel()
        awaitSuccess(viewModel)

        repository.workouts = repository.workouts + TestDefaults.workout("w-brand-new", startTime = now)
        viewModel.refresh()

        viewModel.uiState.test {
            val state = nextMatching { it.workouts.any { w -> w.id == "w-brand-new" } }
            assertEquals(4, state.allWorkouts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadRoutineComparison builds comparison for requested routine`() = runVmTest {
        val viewModel = buildViewModel()
        awaitSuccess(viewModel)

        viewModel.loadRoutineComparison("r2", "Pull Day")

        viewModel.uiState.test {
            val state = nextMatching { it.routineComparison?.routineName == "Pull Day" }
            assertEquals("Pull Day", state.routineComparison?.routineName)
            assertTrue(state.routineComparison!!.improvements.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    private suspend fun awaitSuccess(viewModel: HistoryViewModel) {
        viewModel.uiState.test {
            nextMatching { it.loadingState == LoadingState.Success }
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Consumes emissions until one satisfies [predicate], then discards the rest. */
    private suspend fun <T> TurbineTestContext<T>.nextMatching(predicate: (T) -> Boolean): T {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
        cancelAndIgnoreRemainingEvents()
        return item
    }

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
