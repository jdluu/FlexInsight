package com.example.flexinsight.domain.usecase

import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.repository.ExerciseRepository
import com.example.flexinsight.domain.util.StatsCalculator
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Use case to calculate muscle group distribution and intensity for a given period.
 */
class GetMuscleGroupProgressUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(weeks: Int): List<MuscleGroupProgress> {
        val now = Instant.now()
        val endDate = now.toEpochMilli()
        val startDate = now.minus(weeks.toLong() * 7, ChronoUnit.DAYS).toEpochMilli()

        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()

        if (workouts.isEmpty()) {
            return emptyList()
        }

        // Batch fetch all exercises and sets
        val workoutIds = workouts.map { it.id }
        val allExercises = workoutIds.flatMap { workoutId ->
            exerciseDao.getExercisesByWorkoutId(workoutId)
        }
        val exerciseIds = allExercises.map { it.id }
        val allSets = exerciseIds.associateBy(
            { it },
            { exerciseId -> setDao.getSetsByExerciseId(exerciseId) }
        )

        // Map to track volume and sets per muscle group
        val muscleGroupData = mutableMapOf<String, Pair<Double, Int>>()

        allExercises.forEach { exercise ->
            val muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: return@forEach
            val sets = allSets[exercise.id] ?: emptyList()

            val exerciseVolume = sets.sumOf { set ->
                (set.weight ?: 0.0) * (set.reps ?: 0)
            }

            val current = muscleGroupData[muscleGroup] ?: (0.0 to 0)
            muscleGroupData[muscleGroup] = (current.first + exerciseVolume) to (current.second + sets.size)
        }

        // Calculate average volume for intensity determination
        val totalVolume = muscleGroupData.values.sumOf { it.first }
        val averageVolume = if (muscleGroupData.isNotEmpty()) totalVolume / muscleGroupData.size else 0.0

        // Convert to MuscleGroupProgress and determine intensity
        return muscleGroupData.map { (muscleGroup, data) ->
            val (volume, sets) = data
            val intensity = StatsCalculator.calculateRelativeIntensity(volume, averageVolume)
            MuscleGroupProgress(
                muscleGroup = muscleGroup,
                volume = volume,
                sets = sets,
                intensity = intensity
            )
        }.sortedByDescending { it.volume }
    }
}
