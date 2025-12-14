package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.model.Workout
import com.example.flexinsight.data.model.WorkoutResponse
import kotlinx.coroutines.flow.Flow

/**
 * Interface for workout-related operations.
 */
interface WorkoutRepository {
    fun invalidateApiService()
    
    fun getWorkouts(): Flow<List<Workout>>
    
    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>>
    
    suspend fun getWorkoutById(workoutId: String): Result<Workout>
    
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>
    
    fun getWorkoutCount(): Flow<Int>
    
    fun getWorkoutsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>>
    
    suspend fun syncWorkouts(): Result<Unit>
    
    suspend fun saveWorkoutWithExercisesAndSets(workoutResponse: WorkoutResponse)
    
    suspend fun getMostRecentSyncedTimestamp(): Long?
    
    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean, endTime: Long?): Result<Unit>
    
    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit>
}
