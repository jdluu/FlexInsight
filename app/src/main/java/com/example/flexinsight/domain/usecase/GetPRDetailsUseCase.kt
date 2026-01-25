package com.example.flexinsight.domain.usecase

import com.example.flexinsight.data.local.dao.WorkoutDao
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.SetDao
import com.example.flexinsight.data.model.PRDetails
import com.example.flexinsight.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case to aggregate Personal Record (PR) details with exercise and workout metadata.
 */
class GetPRDetailsUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(limit: Int): List<PRDetails> {
        val prSets = setDao.getRecentPRsFlow(limit).first()
        if (prSets.isEmpty()) {
            return emptyList()
        }

        // Batch fetch exercises and workouts
        val exerciseIds = prSets.map { it.exerciseId }.distinct()
        val exercises = exerciseIds.mapNotNull { exerciseId ->
            exerciseDao.getExerciseById(exerciseId)
        }

        val workoutIds = exercises.map { it.workoutId }.distinct()
        val workouts = workoutIds.associateWith { workoutId ->
            workoutDao.getWorkoutById(workoutId)
        }

        return prSets.mapNotNull { set ->
            val exercise = exercises.find { it.id == set.exerciseId } ?: return@mapNotNull null
            val workout = workouts[exercise.workoutId] ?: return@mapNotNull null

            val weight = set.weight ?: return@mapNotNull null

            val muscleGroup = exerciseRepository.getMuscleGroupForExercise(exercise) ?: "Unknown"

            PRDetails(
                exerciseName = exercise.name,
                date = workout.startTime,
                muscleGroup = muscleGroup,
                weight = weight,
                workoutId = workout.id,
                setId = set.id
            )
        }
    }
}
