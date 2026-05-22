package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface for statistics and data analysis.
 */
interface StatsRepository {
    fun invalidateStatsCache()

    suspend fun calculateStats(): WorkoutStats

    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats

    fun getRecentPRs(limit: Int = 10): Flow<List<com.jdluu.flexinsight.data.model.Set>>

    suspend fun getPRsWithDetails(limit: Int = 10): List<PRDetails>

    suspend fun getAllPRsWithDetails(): List<PRDetails>

    suspend fun getMuscleGroupProgress(weeks: Int = 4): List<MuscleGroupProgress>

    suspend fun calculateVolumeTrend(weeks: Int = 4): VolumeTrend

    /** Calendar month vs previous month stats for History comparison UI. */
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

    suspend fun getProfileInfo(hasApiKey: Boolean, remoteWorkoutCount: Int? = null): ProfileInfo

    suspend fun getConsistencyData(days: Int = 90): List<DayInfo>

    suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float>
}
