package com.jdluu.flexinsight.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

/**
 * Persists sync metadata for dashboard indicators and new-workout notifications.
 */
@Singleton
class SyncPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lastSyncAt = longPreferencesKey("last_sync_at")
    private val lastKnownWorkoutCount = intPreferencesKey("last_known_workout_count")
    private val pendingNewWorkouts = intPreferencesKey("pending_new_workouts")
    private val pendingDeletedWorkouts = intPreferencesKey("pending_deleted_workouts")

    val lastSyncAtFlow: Flow<Long?> = context.syncDataStore.data.map { it[lastSyncAt] }

    val pendingNewWorkoutsFlow: Flow<Int> = context.syncDataStore.data.map {
        it[pendingNewWorkouts] ?: 0
    }

    val pendingDeletedWorkoutsFlow: Flow<Int> = context.syncDataStore.data.map {
        it[pendingDeletedWorkouts] ?: 0
    }

    suspend fun getLastSyncAt(): Long? = context.syncDataStore.data.first()[lastSyncAt]

    suspend fun recordSyncSuccess(workoutCount: Int) {
        context.syncDataStore.edit { prefs ->
            val previous = prefs[lastKnownWorkoutCount] ?: 0
            val newPending = (workoutCount - previous).coerceAtLeast(0)
            if (newPending > 0) {
                prefs[pendingNewWorkouts] = (prefs[pendingNewWorkouts] ?: 0) + newPending
            }
            prefs[lastKnownWorkoutCount] = workoutCount
            prefs[lastSyncAt] = System.currentTimeMillis()
        }
    }

    suspend fun clearPendingNewWorkouts() {
        context.syncDataStore.edit { it[pendingNewWorkouts] = 0 }
    }

    suspend fun recordDeletedWorkout() {
        context.syncDataStore.edit { prefs ->
            prefs[pendingDeletedWorkouts] = (prefs[pendingDeletedWorkouts] ?: 0) + 1
        }
    }

    suspend fun clearPendingDeletedWorkouts() {
        context.syncDataStore.edit { it[pendingDeletedWorkouts] = 0 }
    }

    /** Clears all sync metadata (for tests). */
    internal suspend fun clearAllForTests() {
        context.syncDataStore.edit { it.clear() }
    }
}
