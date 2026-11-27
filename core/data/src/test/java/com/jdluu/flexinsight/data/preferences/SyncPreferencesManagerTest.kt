package com.jdluu.flexinsight.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.jdluu.flexinsight.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class SyncPreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SyncPreferencesManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = SyncPreferencesManager(context)
        runBlocking { manager.clearAllForTests() }
    }

    @Test
    fun recordSyncSuccess_incrementsPendingNewWorkouts() = runTest {
        manager.recordSyncSuccess(workoutCount = 5)
        assertEquals(5, manager.pendingNewWorkoutsFlow.first())

        manager.recordSyncSuccess(workoutCount = 8)
        assertEquals(8, manager.pendingNewWorkoutsFlow.first())
    }

    @Test
    fun recordDeletedWorkout_incrementsPendingDeleted() = runTest {
        manager.recordDeletedWorkout()
        manager.recordDeletedWorkout()

        assertEquals(2, manager.pendingDeletedWorkoutsFlow.first())
    }

    @Test
    fun clearPendingCounters_resetsFlows() = runTest {
        manager.recordSyncSuccess(workoutCount = 3)
        manager.recordDeletedWorkout()
        manager.clearPendingNewWorkouts()
        manager.clearPendingDeletedWorkouts()

        assertEquals(0, manager.pendingNewWorkoutsFlow.first())
        assertEquals(0, manager.pendingDeletedWorkoutsFlow.first())
    }

    @Test
    fun recordSyncSuccess_setsLastSyncAt() = runTest {
        manager.recordSyncSuccess(workoutCount = 1)

        assertNotNull(manager.getLastSyncAt())
    }
}
