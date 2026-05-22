package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.repository.WorkoutRepository
import com.jdluu.flexinsight.domain.model.RoutineComparison
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CompareRoutineSessionsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(routineId: String?, routineName: String): RoutineComparison? {
        if (routineId == null) return null

        val workouts = workoutRepository.getWorkouts().first()
            .filter { it.routineId == routineId && !it.isDeleted }
            .sortedByDescending { it.startTime }
            .take(2)

        if (workouts.size < 2) return null

        val recent = workouts[0]
        val previous = workouts[1]

        val recentExercises = workoutRepository.getExercisesByWorkoutId(recent.id)
        val prevExercises = workoutRepository.getExercisesByWorkoutId(previous.id)

        val regressions = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        recentExercises.forEach { rex ->
            val prev = prevExercises.find {
                it.name.equals(rex.name, ignoreCase = true)
            } ?: return@forEach
            val recentSets = workoutRepository.getSetsByExerciseId(rex.id)
            val prevSets = workoutRepository.getSetsByExerciseId(prev.id)
            val recentBest = recentSets.maxOfOrNull { (it.weight ?: 0.0) * (it.reps ?: 0) } ?: 0.0
            val prevBest = prevSets.maxOfOrNull { (it.weight ?: 0.0) * (it.reps ?: 0) } ?: 0.0
            when {
                recentBest > prevBest * 1.02 -> improvements.add("${rex.name}: volume up")
                recentBest < prevBest * 0.98 -> regressions.add("${rex.name}: volume down")
            }
        }

        val summary = when {
            regressions.isNotEmpty() && improvements.isEmpty() ->
                "Latest $routineName session shows regressions vs the prior run."
            improvements.isNotEmpty() && regressions.isEmpty() ->
                "Latest $routineName session improved across key lifts."
            regressions.isNotEmpty() ->
                "Mixed performance on $routineName — review flagged lifts."
            else -> "Performance stable between last two $routineName sessions."
        }

        return RoutineComparison(
            routineName = routineName,
            regressions = regressions,
            improvements = improvements,
            summary = summary
        )
    }
}
