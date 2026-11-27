package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.DailyDurationData
import com.jdluu.flexinsight.domain.model.DayInfo
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.ExerciseHistory
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.domain.model.PRDetails
import com.jdluu.flexinsight.domain.model.PeriodComparison
import com.jdluu.flexinsight.domain.model.PlannedWorkout
import com.jdluu.flexinsight.domain.model.ProfileInfo
import com.jdluu.flexinsight.domain.model.Routine
import com.jdluu.flexinsight.domain.model.RoutineFolder
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.SingleWorkoutStats
import com.jdluu.flexinsight.domain.model.VolumeBalance
import com.jdluu.flexinsight.domain.model.VolumeTrend
import com.jdluu.flexinsight.domain.model.WeeklyGoalProgress
import com.jdluu.flexinsight.domain.model.WeeklyProgress
import com.jdluu.flexinsight.domain.model.WeeklyVolumeData
import com.jdluu.flexinsight.domain.model.Workout
import com.jdluu.flexinsight.domain.model.WorkoutStats
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the main repository.
 * Decouples the UI and domain layers from the data layer implementation.
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
    suspend fun getExerciseHistory(templateId: String): Result<ExerciseHistory>
    suspend fun getWorkoutById(workoutId: String): Workout?
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>
    fun getWorkoutCount(): Flow<Int>
    suspend fun getExercisesByWorkoutId(workoutId: String): List<Exercise>
    suspend fun getSetsByExerciseId(exerciseId: String): List<Set>
    suspend fun updateWorkoutStatus(workoutId: String, isCompleted: Boolean): Result<Unit>
    suspend fun rescheduleWorkout(workoutId: String, newStartTime: Long): Result<Unit>

    // Statistics operations
    suspend fun calculateStats(): WorkoutStats
    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats
    fun getRecentPRs(limit: Int = 10): Flow<List<Set>>
    suspend fun getPRsWithDetails(limit: Int = 10): List<PRDetails>
    suspend fun getAllPRsWithDetails(): List<PRDetails>
    suspend fun getMuscleGroupProgress(weeks: Int = 4): List<MuscleGroupProgress>
    suspend fun calculateVolumeTrend(weeks: Int = 4): VolumeTrend
    suspend fun getPeriodComparison(): PeriodComparison?
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
    suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float>

    // Routine operations
    fun getRoutines(): Flow<List<Routine>>
    suspend fun getRoutineById(routineId: String): Routine?
    suspend fun getRoutineFolders(): List<RoutineFolder>

    // Sync operations
    suspend fun syncAllData(): Result<Unit>
    fun clearCache()
    /** Reserved for a future cloud backup backend; currently a no-op. */
    suspend fun syncWithCloud()
}
