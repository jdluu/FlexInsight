package com.example.flexinsight.domain.usecase

import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.model.WeeklyProgress
import com.example.flexinsight.domain.util.StatsCalculator
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import java.time.temporal.ChronoUnit

/**
 * Use case to calculate weekly volume and workout progress for a given number of weeks.
 */
class GetWeeklyProgressUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao
) {
    suspend operator fun invoke(weeks: Int): List<WeeklyProgress> {
        val now = Instant.now()
        val endDate = now.toEpochMilli()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()

        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()

        // Batch fetch all data upfront to avoid N+1 queries
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.flatMap { exerciseId ->
            setDao.getSetsByExerciseId(exerciseId)
        }

        val weekFields = WeekFields.of(Locale.getDefault())

        // Group by week and calculate progress using in-memory data
        return workouts.groupBy { workout ->
             Instant.ofEpochMilli(workout.startTime)
                .atZone(ZoneId.systemDefault())
                .get(weekFields.weekOfWeekBasedYear())
        }.map { (_, weekWorkouts) ->
            val weekStart = weekWorkouts.minOfOrNull { it.startTime } ?: 0L

            val totalVolume = StatsCalculator.calculateTotalVolume(weekWorkouts, allExercises, allSets)

            WeeklyProgress(
                weekStartDate = weekStart,
                totalVolume = totalVolume,
                workoutCount = weekWorkouts.size,
                averageVolume = if (weekWorkouts.isNotEmpty()) totalVolume / weekWorkouts.size else 0.0
            )
        }
    }
}
