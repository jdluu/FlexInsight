package com.jdluu.flexinsight.domain.health

import com.jdluu.flexinsight.domain.model.Workout

/**
 * Port for reading Health Connect recovery signals and exporting strength workouts.
 */
interface HealthConnectDataSource {
    val requiredPermissions: kotlin.collections.Set<String>
    val readPermissions: kotlin.collections.Set<String>
        get() = requiredPermissions
    val writePermissions: kotlin.collections.Set<String>
        get() = emptySet()

    fun isSdkAvailable(): Boolean

    suspend fun readSnapshot(): HealthConnectSnapshot

    suspend fun writeWorkoutsToHealthConnect(workouts: List<Workout>): Int
}
