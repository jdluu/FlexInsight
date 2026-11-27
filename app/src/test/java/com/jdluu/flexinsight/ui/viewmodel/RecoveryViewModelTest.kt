package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.usecase.CalculateTrainingLoadUseCase
import com.jdluu.flexinsight.domain.usecase.DetectDeloadUseCase
import com.jdluu.flexinsight.domain.usecase.GetMuscleRecoveryUseCase
import com.jdluu.flexinsight.fakes.FakeStatsRepository
import com.jdluu.flexinsight.fakes.OneShotFakeAiClient
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.TestDefaults
import com.jdluu.flexinsight.fakes.healthConnectRepositoryStub
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.ui.common.LoadingState
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class RecoveryViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var statsRepository: FakeStatsRepository
    private lateinit var userPreferences: UserPreferencesManager
    private lateinit var aiClient: OneShotFakeAiClient

    @Before
    fun setUp() {
        repository = ScriptableFlexRepository().apply {
            recentWorkouts = listOf(TestDefaults.workout("w1", startTime = 2000L))
        }
        statsRepository = FakeStatsRepository().apply {
            recoveryToReturn = mapOf(
                MuscleGroup.CHEST to 0.80f,
                MuscleGroup.LEGS to 0.40f
            )
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        userPreferences = UserPreferencesManager(context)
        kotlinx.coroutines.runBlocking {
            userPreferences.resetForTests()
        }
        aiClient = OneShotFakeAiClient(available = false)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(
        healthSnapshot: HealthConnectSnapshot = HealthConnectSnapshot()
    ): RecoveryViewModel {
        val hcStub = healthConnectRepositoryStub(
            sdkAvailable = healthSnapshot.isAvailable,
            snapshot = healthSnapshot
        )
        return RecoveryViewModel(
            getMuscleRecoveryUseCase = GetMuscleRecoveryUseCase(statsRepository),
            calculateTrainingLoadUseCase = CalculateTrainingLoadUseCase(repository, hcStub),
            detectDeloadUseCase = DetectDeloadUseCase(repository, hcStub),
            healthConnectRepository = hcStub,
            userPreferencesManager = userPreferences,
            aiClient = aiClient
        )
    }

    @Test
    fun `init loads muscle recovery score training load and health snapshot`() = runVmTest {
        val snapshot = HealthConnectSnapshot(
            isAvailable = true,
            isPermissionGranted = true,
            sleepHoursLastNight = 7.5,
            restingHeartRateBpm = 56L,
            stepsToday = 8200L
        )
        val viewModel = buildViewModel(healthSnapshot = snapshot)
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LoadingState.Success, state.loadingState)
            assertEquals(60, state.overallRecoveryScore)
            assertEquals(0.80f, state.muscleRecovery[MuscleGroup.CHEST])
            assertEquals(0.40f, state.muscleRecovery[MuscleGroup.LEGS])
            assertNotNull(state.trainingLoad)
            assertEquals(7.5, state.healthSnapshot?.sleepHoursLastNight)
        }
    }

    @Test
    fun `selectMuscle toggles selected group cleanly`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        assertNull(viewModel.uiState.value.selectedMuscle)
        viewModel.selectMuscle(MuscleGroup.LEGS)
        assertEquals(MuscleGroup.LEGS, viewModel.uiState.value.selectedMuscle)

        viewModel.selectMuscle(MuscleGroup.LEGS)
        assertNull(viewModel.uiState.value.selectedMuscle)
    }

    @Test
    fun `updateMood clamps values and updateNotes persists soreness notes`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.updateMood(14f)
        assertEquals(10f, viewModel.uiState.value.moodRating)

        viewModel.updateMood(-2f)
        assertEquals(0f, viewModel.uiState.value.moodRating)

        viewModel.updateNotes("Right hamstring slightly tight")
        assertEquals("Right hamstring slightly tight", viewModel.uiState.value.sorenessNotes)

        awaitUntil("persisted mood & notes") {
            userPreferences.recoveryMoodFlow.first() == 0f &&
                userPreferences.recoveryNotesFlow.first() == "Right hamstring slightly tight"
        }
    }

    @Test
    fun `ai coach note is generated when ai client is available`() = runVmTest {
        aiClient.available = true
        aiClient.response = Result.Success("Prioritize upper-body work while quads recover.")
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        awaitUntil("ai insight") { viewModel.uiState.value.aiInsight != null }

        val state = viewModel.uiState.value
        assertEquals("Prioritize upper-body work while quads recover.", state.aiInsight)
        assertFalse(state.isGeneratingInsight)
        assertEquals(1, aiClient.prompts.size)
        assertTrue(aiClient.prompts.single().contains("Legs"))
    }

    private suspend fun awaitInitialLoad(
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        viewModel: RecoveryViewModel
    ) {
        scheduler.advanceUntilIdle()
        awaitUntil("initial load") {
            viewModel.uiState.value.loadingState != LoadingState.Loading
        }
    }

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

    private fun runVmTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                block()
            } finally {
                Dispatchers.resetMain()
            }
        }
}
