package com.jdluu.flexinsight.data.sync

import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val syncPreferencesManager: SyncPreferencesManager,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend fun onSyncComplete() {
        val count = workoutRepository.getWorkoutCount().first()
        syncPreferencesManager.recordSyncSuccess(count)
        val recent = workoutRepository.getRecentWorkouts(limit = 5).first()
        healthConnectRepository.writeWorkoutsToHealthConnect(recent)
    }
}
