package com.jdluu.flexinsight.data.repository

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
import com.jdluu.flexinsight.domain.repository.StatsMutationRepository
import com.jdluu.flexinsight.domain.repository.StatsQueryRepository
import com.jdluu.flexinsight.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository for statistics and data analysis.
 */
class StatsRepositoryImpl(
    private val queryRepository: StatsQueryRepository,
    private val mutationRepository: StatsMutationRepository
) : StatsRepository {
    override fun invalidateStatsCache() = mutationRepository.invalidateStatsCache()

    override suspend fun calculateStats(): WorkoutStats = queryRepository.calculateStats()

    override suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats =
        queryRepository.calculateWorkoutStats(workout)

    override fun getRecentPRs(limit: Int): Flow<List<Set>> = queryRepository.getRecentPRs(limit)

    override suspend fun getPRsWithDetails(limit: Int): List<PRDetails> = queryRepository.getPRsWithDetails(limit)

    override suspend fun getAllPRsWithDetails(): List<PRDetails> = queryRepository.getAllPRsWithDetails()

    override suspend fun getMuscleGroupProgress(weeks: Int): List<MuscleGroupProgress> =
        queryRepository.getMuscleGroupProgress(weeks)

    override suspend fun calculateVolumeTrend(weeks: Int): VolumeTrend =
        queryRepository.calculateVolumeTrend(weeks)

    override suspend fun getPeriodComparison(): PeriodComparison? = queryRepository.getPeriodComparison()

    override suspend fun getWeeklyVolumeData(weeks: Int): List<WeeklyVolumeData> =
        queryRepository.getWeeklyVolumeData(weeks)

    override suspend fun getDurationTrend(weeks: Int): List<DailyDurationData> =
        queryRepository.getDurationTrend(weeks)

    override suspend fun getWeeklyGoalProgress(target: Int): WeeklyGoalProgress =
        queryRepository.getWeeklyGoalProgress(target)

    override suspend fun getWeekCalendarData(): List<DayInfo> = queryRepository.getWeekCalendarData()

    override suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> =
        queryRepository.getPlannedWorkoutsForDay(timestamp)

    override suspend fun getVolumeBalance(weeks: Int): VolumeBalance = queryRepository.getVolumeBalance(weeks)

    override suspend fun getWeeklyProgress(weeks: Int): List<WeeklyProgress> =
        queryRepository.getWeeklyProgress(weeks)

    override suspend fun getMemberSinceDate(): Long? = queryRepository.getMemberSinceDate()

    override suspend fun calculateAccountAgeDays(): Int = queryRepository.calculateAccountAgeDays()

    override suspend fun getProfileInfo(hasApiKey: Boolean, remoteWorkoutCount: Int?): ProfileInfo =
        queryRepository.getProfileInfo(hasApiKey, remoteWorkoutCount)

    override suspend fun getConsistencyData(days: Int): List<DayInfo> = queryRepository.getConsistencyData(days)

    override suspend fun getMuscleRecoveryStatus(): Map<MuscleGroup, Float> =
        queryRepository.getMuscleRecoveryStatus()
}
