package com.example.flexinsight.domain.usecase

import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.model.WorkoutStats
import com.example.flexinsight.domain.util.StatsCalculator
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.example.flexinsight.data.repository.StatsRepository

/**
 * Use case to calculate aggregate workout statistics.
 */
class GetWorkoutStatsUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val getWeeklyProgressUseCase: GetWeeklyProgressUseCase
) {
    suspend operator fun invoke(): WorkoutStats {
        val workouts = workoutDao.getAllWorkoutsFlow().first()

        if (workouts.isEmpty()) {
            return WorkoutStats(
                totalWorkouts = 0,
                totalVolume = 0.0,
                averageVolume = 0.0,
                totalSets = 0,
                totalDuration = 0L,
                averageDuration = 0L,
                currentStreak = 0,
                longestStreak = 0,
                bestWeekVolume = 0.0,
                bestWeekDate = null
            )
        }

        // Optimize: Get all exercises and sets in batch
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }

        // Calculate stats using StatsCalculator
        val totalWorkouts = workouts.size
        val totalVolume = StatsCalculator.calculateTotalVolume(workouts, allExercises, allSets)
        val averageVolume = if (totalWorkouts > 0) totalVolume / totalWorkouts else 0.0
        val totalSets = allSets.size

        val totalDuration = StatsCalculator.calculateTotalDuration(workouts)
        val averageDuration = if (totalWorkouts > 0) totalDuration / totalWorkouts else 0L

        val currentStreak = StatsCalculator.calculateStreak(workouts)
        val longestStreak = StatsCalculator.calculateLongestStreak(workouts)

        // Calculate best week
        val weeklyProgress = getWeeklyProgressUseCase(4)
        val bestWeek = weeklyProgress.maxByOrNull { it.totalVolume }

        return WorkoutStats(
            totalWorkouts = totalWorkouts,
            totalVolume = totalVolume,
            averageVolume = averageVolume,
            totalSets = totalSets,
            totalDuration = totalDuration,
            averageDuration = averageDuration,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            bestWeekVolume = bestWeek?.totalVolume ?: 0.0,
            bestWeekDate = bestWeek?.weekStartDate
        )
    }
}
