package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.DayInfo
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.fakes.OneShotFakeAiClient
import com.jdluu.flexinsight.fakes.ScriptableExerciseRepository
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.ScriptableRoutineRepository
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.ui.common.LoadingState
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class PlannerViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var routineRepository: ScriptableRoutineRepository
    private lateinit var exerciseRepository: ScriptableExerciseRepository
    private lateinit var userPreferences: UserPreferencesManager
    private lateinit var aiClient: OneShotFakeAiClient

    /** Backing store behind repository.plannedForDay so reloads observe mutations. */
    private val plannedByDay = mutableMapOf<Long, List<PlannedWorkout>>()

    private val todayStart: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val dayMs = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {

        repository = ScriptableFlexRepository()
        repository.reschedules.clear()
        routineRepository = ScriptableRoutineRepository()
        exerciseRepository = ScriptableExerciseRepository()
        aiClient = OneShotFakeAiClient(available = false)
        userPreferences = UserPreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        kotlinx.coroutines.runBlocking { userPreferences.resetForTests() }

        repository.weekCalendar = listOf(
            DayInfo("Mon", 1, todayStart - dayMs, hasWorkout = true, isCompleted = true, workoutCount = 1),
            DayInfo("Tue", 2, todayStart, hasWorkout = true, isCompleted = false, workoutCount = 2),
            DayInfo("Wed", 3, todayStart + dayMs, hasWorkout = false, isCompleted = false, workoutCount = 0)
        )
        repository.plannedForDay = { timestamp -> plannedByDay[timestamp].orEmpty() }
        repository.volumeBalance = VolumeBalance(0.5f, 0.25f, 0.25f, 0f)
        plannedByDay[todayStart - dayMs] = listOf(plannedWorkout("pw-yesterday", "Yesterday Session"))
        plannedByDay[todayStart] = listOf(plannedWorkout("pw-today", "Today Session"))
        plannedByDay[todayStart + dayMs] = listOf(plannedWorkout("pw-tomorrow", "Tomorrow Session"))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(): PlannerViewModel = PlannerViewModel(
        repository = repository,
        routineRepository = routineRepository,
        exerciseRepository = exerciseRepository,
        aiClient = aiClient,
        userPreferencesManager = userPreferences
    )

    // region Initialization

    @Test
    fun `init loads planner data and selects today`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals(3, state.weekCalendarData.size)
            assertEquals(1, state.selectedDayIndex)
            assertEquals(listOf("pw-today"), state.selectedDayWorkouts.map { it.id })
            assertEquals(VolumeBalance(0.5f, 0.25f, 0.25f, 0f), state.volumeBalance)
            assertTrue(state.viewOnlyMode)
            assertFalse(state.hevyEditingEnabled)
            assertNull(state.error)
            assertNull(state.editBlockedMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init tolerates failures in per-day planning data`() = runVmTest {
        repository.plannedForDay = { throw java.io.IOException("planner boom") }
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertTrue(state.weekCalendarData.isNotEmpty())
            assertTrue(state.selectedDayWorkouts.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Day selection

    @Test
    fun `selectDay loads workouts for the chosen day`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.selectDay(0)

        viewModel.uiState.test {
            val state = nextMatching { it.selectedDayIndex == 0 }
            assertEquals(listOf("pw-yesterday"), state.selectedDayWorkouts.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectDay ignores out of range indexes`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.selectDay(42)

        assertEquals(1, viewModel.uiState.value.selectedDayIndex)
    }

    // endregion

    // region Workout completion (view-only gating)

    @Test
    fun `markWorkoutAsComplete is blocked in view-only mode with a message`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.markWorkoutAsComplete("pw-today", true)

        viewModel.uiState.test {
            val state = nextMatching { it.editBlockedMessage != null }
            assertTrue(state.editBlockedMessage!!.contains("Edits are disabled"))
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.statusUpdates.isEmpty())
        assertFalse(viewModel.uiState.value.selectedDayWorkouts.first().isCompleted)

        viewModel.clearEditBlockedMessage()
        assertNull(viewModel.uiState.value.editBlockedMessage)
    }

    @Test
    fun `markWorkoutAsComplete optimistically flips state then reloads on success`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)
        val gate = CompletableDeferred<Result<Unit>>()
        repository.updateStatusGate = gate

        viewModel.markWorkoutAsComplete("pw-today", true)

        viewModel.uiState.test {
            nextMatching {
                it.selectedDayWorkouts.firstOrNull()?.id == "pw-today" &&
                    it.selectedDayWorkouts.first().isCompleted
            }

            gate.complete(Result.Success(Unit))

            nextMatching {
                !it.isLoading &&
                    it.selectedDayWorkouts.firstOrNull()?.id == "pw-today" &&
                    !it.selectedDayWorkouts.first().isCompleted
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("pw-today" to true, repository.statusUpdates.single())
    }

    @Test
    fun `markWorkoutAsComplete reverts optimistic update and reports error on failure`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)
        val gate = CompletableDeferred<Result<Unit>>()
        repository.updateStatusGate = gate

        viewModel.markWorkoutAsComplete("pw-today", true)

        viewModel.uiState.test {
            nextMatching { it.selectedDayWorkouts.first().isCompleted }

            gate.complete(Result.Error(ApiError.ClientError.BadRequest))

            val reverted = nextMatching {
                !it.selectedDayWorkouts.first().isCompleted && it.error != null
            }
            assertNotNull(reverted.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Rescheduling

    @Test
    fun `rescheduleWorkout persists and reloads on success`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)

        viewModel.rescheduleWorkout("pw-today", todayStart + dayMs)

        assertEquals("pw-today" to todayStart + dayMs, repository.reschedules.single())
        awaitReady(testScheduler, viewModel, expectedViewOnly = false) // reload settled back at Success
    }

    @Test
    fun `rescheduleWorkout reports error on failure`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)
        repository.rescheduleResult = Result.Error(ApiError.ClientError.BadRequest)

        viewModel.rescheduleWorkout("pw-today", todayStart + dayMs)

        viewModel.uiState.test {
            assertNotNull(nextMatching { it.error != null }.error)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.reschedules.size)
    }

    @Test
    fun `rescheduleWorkout is blocked in view-only mode`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.rescheduleWorkout("pw-today", todayStart + dayMs)

        assertTrue(repository.reschedules.isEmpty())
        assertNotNull(viewModel.uiState.value.editBlockedMessage)
    }

    // endregion

    // region AI plan generation and push-to-Hevy

    @Test
    fun `generateAIWorkout explains unavailable ai without calling model`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)

        viewModel.generateAIWorkout()

        viewModel.uiState.test {
            val state = nextMatching { it.aiPlan != null }
            assertEquals("AI features are not available on this device.", state.aiPlan)
            assertFalse(state.isGeneratingPlan)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(aiClient.prompts.isEmpty())
    }

    @Test
    fun `generateAIWorkout includes volume balance focus in prompt and shows plan`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)
        aiClient.available = true
        aiClient.response = Result.Success("Push plan: Bench Press - 3x8")

        viewModel.generateAIWorkout()

        viewModel.uiState.test {
            val state = nextMatching { it.aiPlan != null }
            assertFalse(state.isGeneratingPlan)
            assertEquals("Push plan: Bench Press - 3x8", state.aiPlan)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(aiClient.prompts.single().contains("Push=50%"))
    }

    @Test
    fun `pushRoutineToHevy does nothing without an ai plan`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)

        viewModel.pushRoutineToHevy("My Plan")

        assertTrue(routineRepository.createdRequests.isEmpty())
        assertEquals(SaveToHevyStatus.Idle, viewModel.uiState.value.saveToHevyStatus)
    }

    @Test
    fun `pushRoutineToHevy is blocked in view-only mode even with a plan`() = runVmTest {
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = true)
        generatePlan(viewModel)

        viewModel.pushRoutineToHevy("My Plan")

        assertTrue(routineRepository.createdRequests.isEmpty())
        assertNotNull(viewModel.uiState.value.editBlockedMessage)
    }

    @Test
    fun `pushRoutineToHevy parses matched exercises and saves routine`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)
        aiClient.response = Result.Success("Push plan:\nBench Press - 3x8\nMystery Machine - 2x10")
        exerciseRepository.templateNameMapping =
            Result.Success(mapOf("tpl-bench" to "Bench Press"))
        routineRepository.createResult = Result.Success("routine-9")
        generatePlan(viewModel)

        viewModel.pushRoutineToHevy("My Plan")

        viewModel.uiState.test {
            val state = nextMatching { it.saveToHevyStatus is SaveToHevyStatus.Success }
            val status = state.saveToHevyStatus as SaveToHevyStatus.Success
            assertEquals("routine-9", status.routineId)
            assertEquals(1, status.matchedCount)
            assertEquals(listOf("Mystery Machine"), status.unmatchedNames)
            assertFalse(status.usedPlaceholder)
            cancelAndIgnoreRemainingEvents()
        }
        val request = routineRepository.createdRequests.single()
        assertEquals("My Plan", request.routine.title)
        assertEquals("tpl-bench", request.routine.exercises.single().exerciseTemplateId)
        assertEquals(3, request.routine.exercises.single().sets.size)
    }

    @Test
    fun `pushRoutineToHevy surfaces save error and clearSaveStatus resets it`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)
        aiClient.response = Result.Success("Bench Press - 3x8")
        exerciseRepository.templateNameMapping =
            Result.Success(mapOf("tpl-bench" to "Bench Press"))
        routineRepository.createResult = Result.Error(ApiError.ClientError.BadRequest)
        generatePlan(viewModel)

        viewModel.pushRoutineToHevy("My Plan")

        viewModel.uiState.test {
            val state = nextMatching { it.saveToHevyStatus is SaveToHevyStatus.Error }
            assertTrue((state.saveToHevyStatus as SaveToHevyStatus.Error).message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.clearSaveStatus()
        assertEquals(SaveToHevyStatus.Idle, viewModel.uiState.value.saveToHevyStatus)
    }

    @Test
    fun `clearAIPlan resets plan and save status`() = runVmTest {
        enableEditing()
        val viewModel = buildViewModel()
        awaitReady(testScheduler, viewModel, expectedViewOnly = false)
        generatePlan(viewModel)

        viewModel.clearAIPlan()

        assertNull(viewModel.uiState.value.aiPlan)
        assertEquals(SaveToHevyStatus.Idle, viewModel.uiState.value.saveToHevyStatus)
    }

    // endregion

    private fun plannedWorkout(id: String, name: String) = PlannedWorkout(
        id = id,
        name = name,
        duration = 60L,
        intensity = "High Intensity",
        isCompleted = false,
        routineId = "routine-1",
        exerciseCount = 4
    )

    /** Persists view-only off before the VM collects the preference flow. */
    private suspend fun enableEditing() {
        userPreferences.setViewOnlyMode(false)
    }

    /**
     * Resolves the init delay(100), then polls until the load reached Success and the
     * view-only preference has propagated (DataStore emits via real IO threads).
     */
    private suspend fun awaitReady(
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        viewModel: PlannerViewModel,
        expectedViewOnly: Boolean
    ) {
        scheduler.advanceUntilIdle()
        val start = System.currentTimeMillis()
        while (!(viewModel.uiState.value.loadingState == LoadingState.Success &&
                viewModel.uiState.value.viewOnlyMode == expectedViewOnly)
        ) {
            if (System.currentTimeMillis() - start > 10_000) {
                throw AssertionError("Planner never became ready")
            }
            kotlinx.coroutines.withContext(Dispatchers.IO) { kotlinx.coroutines.delay(10) }
        }
    }

    private suspend fun generatePlan(viewModel: PlannerViewModel) {
        aiClient.available = true
        viewModel.generateAIWorkout()
        val start = System.currentTimeMillis()
        while (viewModel.uiState.value.aiPlan == null || viewModel.uiState.value.isGeneratingPlan) {
            if (System.currentTimeMillis() - start > 10_000) {
                throw AssertionError("AI plan never settled")
            }
            kotlinx.coroutines.withContext(Dispatchers.IO) { kotlinx.coroutines.delay(10) }
        }
    }

    /** Consumes emissions until one satisfies [predicate]; does not cancel the turbine. */
    private suspend fun <T> TurbineTestContext<T>.nextMatching(predicate: (T) -> Boolean): T {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
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
