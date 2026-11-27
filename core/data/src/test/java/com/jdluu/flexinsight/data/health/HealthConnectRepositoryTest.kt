package com.jdluu.flexinsight.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.request.ReadRecordsRequest
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.model.Workout
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HealthConnectRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: UserPreferencesManager
    private lateinit var permissionController: PermissionController
    private lateinit var client: HealthConnectClient
    private lateinit var repository: HealthConnectRepository

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = Dispatchers.Unconfined
        override val io: CoroutineDispatcher get() = Dispatchers.Unconfined
        override val default: CoroutineDispatcher get() = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        permissionController = mockk()
        client = mockk(relaxed = true)
        every { client.permissionController } returns permissionController

        coEvery { prefs.getHealthConnectEnabled() } returns true
        coEvery { prefs.getHealthConnectWriteEnabled() } returns true
        coEvery { prefs.getViewOnlyMode() } returns false

        repository = spyk(
            HealthConnectRepository(
                context = context,
                userPreferencesManager = prefs,
                dispatcherProvider = testDispatchers
            )
        )
        every { repository.isSdkAvailable() } returns true
        coEvery { repository.getClientOrNull() } returns client
    }

    @Test
    fun `readSnapshot succeeds with only read permissions granted`() = runTest {
        coEvery { permissionController.getGrantedPermissions() } returns repository.readPermissions
        val emptyReadResponse = mockk<ReadRecordsResponse<Nothing>>(relaxed = true)
        every { emptyReadResponse.records } returns emptyList()
        coEvery { client.readRecords(any<ReadRecordsRequest<*>>()) } returns emptyReadResponse

        assertTrue(repository.hasReadPermissions())
        assertFalse(repository.hasWritePermissions())
        assertFalse(repository.hasAllPermissions())

        val snapshot = repository.readSnapshot()
        assertTrue(snapshot.isAvailable)
        assertTrue(snapshot.isPermissionGranted)
    }

    @Test
    fun `writeWorkoutsToHealthConnect skips when write permission is missing`() = runTest {
        coEvery { permissionController.getGrantedPermissions() } returns repository.readPermissions

        val workouts = listOf(
            Workout(
                id = "w-1",
                name = "Push Day",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_003_600_000L,
                notes = null,
                routineId = null
            )
        )

        val written = repository.writeWorkoutsToHealthConnect(workouts)
        assertEquals(0, written)
        coVerify(exactly = 0) { client.insertRecords(any()) }
    }

    @Test
    fun `writeWorkoutsToHealthConnect writes when write permission is granted`() = runTest {
        val writeOnly = setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
        coEvery { permissionController.getGrantedPermissions() } returns writeOnly
        coEvery { client.insertRecords(any()) } returns mockk<InsertRecordsResponse>(relaxed = true)

        val workouts = listOf(
            Workout(
                id = "w-2",
                name = "Pull Day",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_003_600_000L,
                notes = null,
                routineId = null
            )
        )

        val written = repository.writeWorkoutsToHealthConnect(workouts)
        assertEquals(1, written)
        coVerify(exactly = 1) { client.insertRecords(any()) }
    }
}
