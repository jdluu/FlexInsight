package com.jdluu.flexinsight.domain.health

import com.jdluu.flexinsight.domain.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectDataSourceTest {

    @Test
    fun `default readPermissions falls back to requiredPermissions and writePermissions is empty`() {
        val fake = object : HealthConnectDataSource {
            override val requiredPermissions: Set<String> = setOf("READ_SLEEP", "READ_STEPS")
            override fun isSdkAvailable(): Boolean = true
            override suspend fun readSnapshot(): HealthConnectSnapshot = HealthConnectSnapshot()
            override suspend fun writeWorkoutsToHealthConnect(workouts: List<Workout>): Int = 0
        }

        assertEquals(setOf("READ_SLEEP", "READ_STEPS"), fake.readPermissions)
        assertTrue(fake.writePermissions.isEmpty())
    }

    @Test
    fun `HealthConnectSnapshot hasData reflects populated signals`() {
        assertFalse(HealthConnectSnapshot().hasData)
        assertTrue(HealthConnectSnapshot(sleepHoursLastNight = 7.5).hasData)
        assertTrue(HealthConnectSnapshot(restingHeartRateBpm = 58L).hasData)
        assertTrue(HealthConnectSnapshot(stepsToday = 8000L).hasData)
        assertTrue(HealthConnectSnapshot(cardioSessionsThisWeek = 1).hasData)
    }
}
