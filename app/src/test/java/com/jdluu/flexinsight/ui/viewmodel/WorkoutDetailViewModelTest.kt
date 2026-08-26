package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import com.jdluu.flexinsight.domain.usecase.BuildAiContextUseCase
import com.jdluu.flexinsight.domain.usecase.ExplainWorkoutUseCase
import com.jdluu.flexinsight.fakes.FakeWorkoutRepository
import com.jdluu.flexinsight.fakes.OneShotFakeAiClient
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.TestDefaults
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
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
class WorkoutDetailViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var userPreferences: UserPreferencesManager
    private lateinit var aiClient: OneShotFakeAiClient

    @Before
    fun setUp() {

        repository = ScriptableFlexRepository()
        workoutRepository = FakeWorkoutRepository()
        userPreferences = UserPreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        kotlinx.coroutines.runBlocking { userPreferences.resetForTests() }
        aiClient = OneShotFakeAiClient(available = false)

        repository.workoutById = TestDefaults.workout("w1", startTime = 0L, endTime = 3600_000L)
        repository.workoutStatsResult = SingleWorkoutStats(60, 9, 1200.0)
        repository.exercisesByWorkoutId = mapOf(
            "w1" to listOf(
                TestDefaults.exercise("e1", "w1", name = "Bench Press"),
                TestDefaults.exercise("e2", "w1", name = "Row")
            )
        )
        repository.setsByExerciseId = mapOf(
            "e1" to listOf(
                TestDefaults.set("s1", "e1", weight = 100.0, reps = 5),
                TestDefaults.set("s2", "e1", weight = 102.5, reps = 5)
            ),
            "e2" to listOf(TestDefaults.set("s3", "e2", weight = 70.0, reps = 8))
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(savedStateHandle: SavedStateHandle): WorkoutDetailViewModel =
        WorkoutDetailViewModel(
            repository = repository,
            userPreferencesManager = userPreferences,
            aiClient = aiClient,
            explainWorkoutUseCase = ExplainWorkoutUseCase(
                flexRepository = repository,
                workoutRepository = workoutRepository,
                aiClient = aiClient,
                buildAiContextUseCase = BuildAiContextUseCase(StaticProvider)
            ),
            savedStateHandle = savedStateHandle
        )

    // region Initialization

    @Test
    fun `missing workout id surfaces immediate error without loading`() = runVmTest {
        val viewModel = buildViewModel(SavedStateHandle())

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState is LoadingState.Error }
            assertEquals("No workout ID provided", state.error?.message)
            assertNull(state.workout)
            assertTrue(aiClient.prompts.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init loads workout with stats, exercises and sets`() = runVmTest {
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "w1")))

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals("w1", state.workout?.id)
            assertEquals(60L, state.workoutStats?.durationMinutes)
            assertEquals(1200.0, state.workoutStats?.totalVolume)
            assertEquals(2, state.exercisesWithSets.size)
            assertEquals(2, state.exercisesWithSets.first { it.exercise.id == "e1" }.sets.size)
            assertEquals(1, state.exercisesWithSets.first { it.exercise.id == "e2" }.sets.size)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(aiClient.prompts.isEmpty())
    }

    @Test
    fun `unknown workout id surfaces not-found error`() = runVmTest {
        repository.workoutById = null
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "ghost")))

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState is LoadingState.Error }
            assertEquals("Workout not found", state.error?.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository failure surfaces mapped error state`() = runVmTest {
        repository.exercisesError = IOException("exercise read boom")
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "w1")))

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState is LoadingState.Error }
            assertTrue(state.error is UiError.Network)
            assertEquals("Unable to connect to server", state.error?.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Actions

    @Test
    fun `ai reflection generated when client is available`() = runVmTest {
        aiClient.available = true
        aiClient.response = Result.Success("Great intensity overall.")
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "w1")))

        viewModel.uiState.test {
            val state = nextMatching { it.aiReflection != null }
            assertEquals("Great intensity overall.", state.aiReflection)
            assertFalse(state.isGeneratingReflection)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(aiClient.prompts.single().contains("Bench Press (2 sets)"))
    }

    @Test
    fun `explainWorkout shows generated explanation on success`() = runVmTest {
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "w1")))
        awaitSuccess(viewModel)
        aiClient.response = Result.Success("Highlights: solid volume.")

        viewModel.explainWorkout()

        viewModel.uiState.test {
            val state = nextMatching { it.workoutExplanation != null && !it.isExplainingWorkout }
            assertEquals("Highlights: solid volume.", state.workoutExplanation)
            assertFalse(state.isExplainingWorkout)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `explainWorkout falls back to error message on failure`() = runVmTest {
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "w1")))
        awaitSuccess(viewModel)
        aiClient.response = Result.Error(ApiError.Unknown("model busy"))

        viewModel.explainWorkout()

        viewModel.uiState.test {
            val state = nextMatching { it.workoutExplanation != null && !it.isExplainingWorkout }
            assertEquals("model busy", state.workoutExplanation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads workout data from repository`() = runVmTest {
        val viewModel = buildViewModel(SavedStateHandle(mapOf("workoutId" to "w1")))
        awaitSuccess(viewModel)

        repository.setsByExerciseId = mapOf(
            "e1" to listOf(
                TestDefaults.set("s1", "e1", weight = 105.0, reps = 5),
                TestDefaults.set("s2", "e1", weight = 107.5, reps = 5),
                TestDefaults.set("s4", "e1", weight = 110.0, reps = 3)
            )
        )
        viewModel.refresh()

        viewModel.uiState.test {
            val state = nextMatching {
                it.exercisesWithSets.firstOrNull { e -> e.exercise.id == "e1" }?.sets?.size == 3
            }
            assertEquals(3, state.exercisesWithSets.first { it.exercise.id == "e1" }.sets.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    private suspend fun awaitSuccess(viewModel: WorkoutDetailViewModel) {
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

    private object StaticProvider : AiContextProvider {
        override suspend fun buildContext(userQuery: String?): HevyAiDataAccessor.ContextSnapshot =
            HevyAiDataAccessor.ContextSnapshot(
                text = "System Context",
                hasWorkoutData = true,
                hasApiKey = true,
                workoutCount = 1,
                usesLiveExerciseHistory = false
            )
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
