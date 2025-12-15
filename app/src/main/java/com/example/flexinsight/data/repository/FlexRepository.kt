package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the main repository.
 * Decouples the UI layer from the data layer implementation.
 */
interface FlexRepository {
    /**
     * Invalidate the API service (useful when API key is updated)
     */
    fun invalidateApiService()

    // Workout operations
    fun getWorkouts(): Flow<List<Workout>>
    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>>

    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistoryResponse>
    suspend fun getWorkoutById(workoutId: String): Workout?
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>
    fun getWorkoutCount(): Flow<Int>
    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit>
    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit>

    // Statistics operations
    suspend fun calculateStats(): WorkoutStats
    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats
    fun getRecentPRs(limit: Int = 10): Flow<List<com.example.flexinsight.data.model.Set>>
    suspend fun getPRsWithDetails(limit: Int = 10): List<PRDetails>
    suspend fun getAllPRsWithDetails(): List<PRDetails>
    suspend fun getMuscleGroupProgress(weeks: Int = 4): List<MuscleGroupProgress>
    suspend fun calculateVolumeTrend(weeks: Int = 4): VolumeTrend
    suspend fun getWeeklyVolumeData(weeks: Int = 4): List<WeeklyVolumeData>
    suspend fun getDurationTrend(weeks: Int = 6): List<DailyDurationData>
    suspend fun getWeeklyGoalProgress(target: Int = 5): WeeklyGoalProgress
    suspend fun getWeekCalendarData(): List<DayInfo>
    suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout>
    suspend fun getVolumeBalance(weeks: Int = 4): VolumeBalance
    suspend fun getWeeklyProgress(weeks: Int = 4): List<WeeklyProgress>
    suspend fun getMemberSinceDate(): Long?
    suspend fun calculateAccountAgeDays(): Int
    suspend fun getProfileInfo(): ProfileInfo
    suspend fun getConsistencyData(days: Int = 90): List<DayInfo>

    // Routine operations
    fun getRoutines(): Flow<List<Routine>>
    suspend fun getRoutineById(routineId: String): Routine?
    suspend fun getRoutineFolders(): List<RoutineFolder>

    // Sync operations
    suspend fun syncAllData()
    fun clearCache()
    suspend fun syncWithCloud()
}
