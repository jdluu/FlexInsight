package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.data.model.DailyDurationData
import com.jdluu.flexinsight.data.model.DayInfo
import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.model.PeriodComparison
import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.model.Set
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.data.model.VolumeTrend
import com.jdluu.flexinsight.data.model.WeeklyGoalProgress
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.WeeklyVolumeData
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.WorkoutStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository for statistics and data analysis.
 * Facade over [StatsQueryRepository] (reads/computations) and [StatsMutationRepository]
 * (cache invalidation), kept for backward compatibility with existing ViewModels and use cases.
 */
class StatsRepositoryImpl(
    private val queryRepository: StatsQueryRepository,
    private val mutationRepository: StatsMutationRepository
) : StatsRepository {
    override fun invalidateStatsCache() {
        mutationRepository.invalidateStatsCache()
    }

    override suspend fun calculateStats(): WorkoutStats {
        return queryRepository.calculateStats()
    }

    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats {
        return queryRepository.calculateWorkoutStats(workout)
    }

    override fun getRecentPRs(limit: Int): Flow<List<Set>> {
        return queryRepository.getRecentPRs(limit)
    }

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> {
        return queryRepository.getPRsWithDetails(limit)
    }

    override suspend fun getAllPRsWithDetails(): List<PRDetails> {
        return queryRepository.getAllPRsWithDetails()
    }

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> {
        return queryRepository.getMuscleGroupProgress(weeks)
    }

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend {
        return queryRepository.calculateVolumeTrend(weeks)
    }

    override suspend fun getPeriodComparison(): PeriodComparison? {
        return queryRepository.getPeriodComparison()
    }

    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> {
        return queryRepository.getWeeklyVolumeData(weeks)
    }

    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> {
        return queryRepository.getDurationTrend(weeks)
    }

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress {
        return queryRepository.getWeeklyGoalProgress(target)
    }

    override suspend fun getWeekCalendarData(): List<DayInfo> {
        return queryRepository.getWeekCalendarData()
    }

    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> {
        return queryRepository.getPlannedWorkoutsForDay(timestamp)
    }

    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance {
        return queryRepository.getVolumeBalance(weeks)
    }

    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> {
        return queryRepository.getWeeklyProgress(weeks)
    }

    override suspend fun getMemberSinceDate(): Long? {
        return queryRepository.getMemberSinceDate()
    }

    override suspend fun calculateAccountAgeDays(): Int {
        return queryRepository.calculateAccountAgeDays()
    }

    override suspend fun getProfileInfo(hasApiKey: Boolean, remoteWorkoutCount: Int?): ProfileInfo {
        return queryRepository.getProfileInfo(hasApiKey, remoteWorkoutCount)
    }

    override suspend fun getConsistencyData(days: Int): List<DayInfo> {
        return queryRepository.getConsistencyData(days)
    }

    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> {
        return queryRepository.getMuscleRecoveryStatus()
    }
}
