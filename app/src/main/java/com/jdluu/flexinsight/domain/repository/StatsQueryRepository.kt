package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.domain.model.DailyDurationData
import com.jdluu.flexinsight.domain.model.DayInfo
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.domain.model.PRDetails
import com.jdluu.flexinsight.domain.model.PeriodComparison
import com.jdluu.flexinsight.domain.model.PlannedWorkout
import com.jdluu.flexinsight.domain.model.ProfileInfo
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
 * Read/query concerns for statistics: computing and caching derived stats.
 */
interface StatsQueryRepository {
    suspend fun calculateStats(): WorkoutStats

    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats

    fun getRecentPRs(limit: Int = 10): Flow<List<Set>>

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
