package com.jdluu.flexinsight.data.sync

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.WorkoutResponse
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeHevySyncSource(
    private var result: Result<Unit> = Result.Success(Unit)
) : HevySyncSource {
    var callCount = 0
        private set

    fun enqueue(value: Result<Unit>) {
        result = value
    }

    override suspend fun syncAll(): Result<Unit> {
        callCount++
        return result
    }
}

class RecordingWorkoutRepository(
    private val workoutCount: Int = 0,
    private val recentWorkouts: List<Workout> = emptyList()
) : WorkoutRepository {
    override fun invalidateApiService() {}
    override fun getWorkouts(): Flow<List<Workout>> = flowOf(emptyList())
    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> = flowOf(recentWorkouts)
    override suspend fun getWorkoutById(workoutId: String): Result<Workout> =
        Result.Error(ApiError.Unknown("not used"))

    override fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> = flowOf(null)
    override fun getWorkoutCount(): Flow<Int> = flowOf(workoutCount)
    override suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise> = emptyList()
    override suspend fun getSetsByExerciseId(exerciseId: String): List<Set> = emptyList()
    override suspend fun getRemoteWorkoutCount(): Result<Int> =
        Result.Error(ApiError.Unknown("not used"))

    override fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>> =
        flowOf(emptyList())

    override suspend fun syncWorkouts(): Result<Unit> = Result.Success(Unit)
    override suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse) {}
    override suspend fun getMostRecentSyncedTimestamp(): Long? = null
    override suspend fun updateWorkoutStatus(
        workoutId: String,
        isCompleted: Boolean,
        endTime: Long?
    ): Result<Unit> = Result.Success(Unit)

    override suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit> =
        Result.Success(Unit)
}

class RecordingSyncPreferences {
    val recordedCounts = mutableListOf<Int>()
    val manager: SyncPreferencesManager = mockk(relaxed = true)

    init {
        coEvery { manager.recordSyncSuccess(any()) } answers { recordedCounts.add(firstArg<Int>()) }
    }
}
