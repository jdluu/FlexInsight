package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.calc.PlateColorBand
import com.jdluu.flexinsight.domain.calc.WeightUnit
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.ui.timer.RestTimerController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class PlateRackViewModelTest {

    private lateinit var userPreferences: UserPreferencesManager

    @Before
    fun setUp() {
        userPreferences = UserPreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        kotlinx.coroutines.runBlocking { userPreferences.resetForTests() }
    }

    private fun testDispatchers(scheduler: TestCoroutineScheduler): DispatcherProvider {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler)
        return object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val unconfined: CoroutineDispatcher = dispatcher
        }
    }

    @Test
    fun `initializes from SavedStateHandle weight and unit arguments`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val controller = RestTimerController(testDispatchers(testScheduler))
            val viewModel = PlateRackViewModel(
                userPreferencesManager = userPreferences,
                restTimerController = controller,
                savedStateHandle = SavedStateHandle(mapOf("weight" to "315", "unit" to "LB"))
            )

            val state = viewModel.uiState.value
            assertEquals(WeightUnit.LB, state.unit)
            assertEquals(315.0, state.targetWeight, 0.001)
            assertEquals("315", state.targetWeightText)
            assertEquals(45.0, state.barWeight, 0.001)
            assertTrue(state.loadout.isExact)
            assertEquals(3, state.loadout.totalPlatesPerSide)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `syncs unit with UserPreferencesManager when no nav args are provided`() = runTest {
        kotlinx.coroutines.runBlocking { userPreferences.setUnits("Imperial") }
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val controller = RestTimerController(testDispatchers(testScheduler))
            val viewModel = PlateRackViewModel(
                userPreferencesManager = userPreferences,
                restTimerController = controller,
                savedStateHandle = SavedStateHandle()
            )

            viewModel.uiState.test {
                var state = awaitItem()
                while (state.unit != WeightUnit.LB) {
                    state = awaitItem()
                }
                assertEquals(WeightUnit.LB, state.unit)
                assertEquals(225.0, state.targetWeight, 0.001)
                assertEquals(45.0, state.barWeight, 0.001)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `stepping, plate toggling, bar selection, and warmup ramp recompute loadout`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val controller = RestTimerController(testDispatchers(testScheduler))
            val viewModel = PlateRackViewModel(
                userPreferencesManager = userPreferences,
                restTimerController = controller,
                savedStateHandle = SavedStateHandle(mapOf("weight" to "100", "unit" to "KG"))
            )

            // Step +1 -> 102.5 kg
            viewModel.stepWeight(1)
            assertEquals(102.5, viewModel.uiState.value.targetWeight, 0.001)
            assertEquals("102.5", viewModel.uiState.value.targetWeightText)

            // Disable 25s -> 102.5 kg uses 2x20 (BLUE) + 1x1.25 (CHROME)
            viewModel.togglePlate(25.0)
            assertFalse(25.0 in viewModel.uiState.value.enabledPlates)
            assertEquals(PlateColorBand.BLUE, viewModel.uiState.value.loadout.platesPerSide.first().colorBand)

            // Switch bar weight to 15 kg
            viewModel.selectBarWeight(15.0)
            assertEquals(15.0, viewModel.uiState.value.barWeight, 0.001)

            // Set target weight to 140 kg and inspect 50% warmup (70 kg)
            viewModel.setTargetWeight(140.0)
            assertEquals(3, viewModel.uiState.value.warmupRamp.size)
            assertEquals(70.0, viewModel.uiState.value.warmupRamp[0].targetWeight, 0.001)

            // Switch unit to LB converts cleanly
            viewModel.selectUnit(WeightUnit.LB)
            assertEquals(WeightUnit.LB, viewModel.uiState.value.unit)
            assertEquals(45.0, viewModel.uiState.value.barWeight, 0.001)
            assertTrue(viewModel.uiState.value.targetWeight > 250.0)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `timer actions delegate cleanly to RestTimerController`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val controller = RestTimerController(testDispatchers(testScheduler))
            val viewModel = PlateRackViewModel(
                userPreferencesManager = userPreferences,
                restTimerController = controller,
                savedStateHandle = SavedStateHandle()
            )

            viewModel.startTimerPreset(120)
            assertEquals(120, viewModel.uiState.value.selectedPresetSeconds)
            assertEquals(120, viewModel.uiState.value.timerState.totalSeconds)
            assertTrue(viewModel.uiState.value.timerState.isRunning)

            viewModel.toggleTimerPause()
            assertFalse(viewModel.uiState.value.timerState.isRunning)

            viewModel.addTimerSeconds(15)
            assertEquals(135, viewModel.uiState.value.timerState.remainingSeconds)

            viewModel.resetTimer()
            assertFalse(viewModel.uiState.value.timerState.isRunning)
            assertFalse(viewModel.uiState.value.timerState.isVisibleInBanner)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `onWeightTextChanged sanitizes input and updates loadout`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val controller = RestTimerController(testDispatchers(testScheduler))
            val viewModel = PlateRackViewModel(
                userPreferencesManager = userPreferences,
                restTimerController = controller,
                savedStateHandle = SavedStateHandle(mapOf("weight" to "100", "unit" to "KG"))
            )

            viewModel.onWeightTextChanged("14a2.5.0")
            assertEquals("142.50", viewModel.uiState.value.targetWeightText)
            assertEquals(142.5, viewModel.uiState.value.targetWeight, 0.001)
            assertTrue(viewModel.uiState.value.loadout.isExact)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
