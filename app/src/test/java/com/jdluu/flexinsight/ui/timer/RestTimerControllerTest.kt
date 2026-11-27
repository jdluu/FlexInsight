package com.jdluu.flexinsight.ui.timer

import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerControllerTest {

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
    fun `startPreset counts down every second and completes at zero`() = runTest {
        val controller = RestTimerController(testDispatchers(testScheduler))

        controller.startPreset(3)
        runCurrent()

        assertTrue(controller.state.value.isRunning)
        assertTrue(controller.state.value.isVisibleInBanner)
        assertEquals(3, controller.state.value.remainingSeconds)
        assertFalse(controller.state.value.hasCompleted)

        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(2, controller.state.value.remainingSeconds)

        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(0, controller.state.value.remainingSeconds)
        assertFalse(controller.state.value.isRunning)
        assertTrue(controller.state.value.hasCompleted)
        assertEquals("00:00", controller.state.value.formattedRemaining)
    }

    @Test
    fun `togglePause freezes countdown and resumes from remaining seconds`() = runTest {
        val controller = RestTimerController(testDispatchers(testScheduler))

        controller.startPreset(10)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(8, controller.state.value.remainingSeconds)

        controller.togglePause()
        advanceTimeBy(5_000L)
        runCurrent()
        assertFalse(controller.state.value.isRunning)
        assertEquals(8, controller.state.value.remainingSeconds)

        controller.togglePause()
        advanceTimeBy(3_000L)
        runCurrent()
        assertTrue(controller.state.value.isRunning)
        assertEquals(5, controller.state.value.remainingSeconds)

        controller.reset()
        assertFalse(controller.state.value.isRunning)
        assertFalse(controller.state.value.isVisibleInBanner)
        assertEquals(10, controller.state.value.remainingSeconds)
    }

    @Test
    fun `addSeconds extends active or completed timer and dismissBanner hides banner`() = runTest {
        val controller = RestTimerController(testDispatchers(testScheduler))

        controller.startPreset(2)
        advanceTimeBy(2_000L)
        runCurrent()
        assertTrue(controller.state.value.hasCompleted)

        controller.addSeconds(15)
        assertEquals(15, controller.state.value.remainingSeconds)
        assertEquals(15, controller.state.value.totalSeconds)
        assertTrue(controller.state.value.isRunning)
        assertFalse(controller.state.value.hasCompleted)

        controller.dismissBanner()
        assertFalse(controller.state.value.isRunning)
        assertFalse(controller.state.value.isVisibleInBanner)
    }
}
