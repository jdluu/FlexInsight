package com.example.flexinsight.domain.usecase

import com.example.flexinsight.data.model.MuscleGroup
import com.example.flexinsight.data.repository.ExerciseRepository
import com.example.flexinsight.data.local.dao.ExerciseDao
import com.example.flexinsight.data.local.dao.WorkoutDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.example.flexinsight.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Use case to calculate muscle recovery status based on workout recency.
 * 0.0 = Just trained, 1.0 = Fully recovered (72 hours elapsed)
 */
class GetMuscleRecoveryUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseRepository: ExerciseRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): Map<MuscleGroup, Float> = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        val recoveryTimeMs = 72 * 60 * 60 * 1000L // 72 hours
        
        // Get all workouts from last 7 days (enough to find last session for most)
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val workouts = workoutDao.getWorkoutsSinceFlow(sevenDaysAgo).first()
        
        val lastTrainedMap = mutableMapOf<MuscleGroup, Long>()
        
        // Process workouts from newest to oldest
        workouts.sortedByDescending { it.startTime }.forEach { workout ->
             val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
             exercises.mapNotNull { MuscleGroup.fromString(exerciseRepository.getMuscleGroupForExercise(it)) }
                 .distinct()
                 .forEach { muscleGroup ->
                      if (!lastTrainedMap.containsKey(muscleGroup)) {
                          lastTrainedMap[muscleGroup] = workout.startTime
                      }
                 }
        }
        
        // Calculate recovery percentage
        MuscleGroup.values().associateWith { group ->
            val lastTrained = lastTrainedMap[group] ?: 0L
            if (lastTrained == 0L) return@associateWith 1.0f
            
            val elapsed = now - lastTrained
            val percentage = (elapsed.toFloat() / recoveryTimeMs.toFloat()).coerceIn(0f, 1f)
            percentage
        }
    }
}
