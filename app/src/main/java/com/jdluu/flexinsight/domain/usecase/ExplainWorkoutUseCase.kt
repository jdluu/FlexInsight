package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.repository.WorkoutRepository
import javax.inject.Inject

class ExplainWorkoutUseCase @Inject constructor(
    private val flexRepository: FlexRepository,
    private val workoutRepository: WorkoutRepository,
    private val aiClient: FlexAIClient,
    private val buildAiContextUseCase: BuildAiContextUseCase
) {
    suspend operator fun invoke(workoutId: String): Result<String> {
        val workout = flexRepository.getWorkoutById(workoutId)
            ?: return Result.Error(com.jdluu.flexinsight.core.errors.ApiError.Unknown("Workout not found"))

        val exercises = workoutRepository.getExercisesByWorkoutId(workoutId)
        val sb = StringBuilder()
        sb.appendLine("Explain this completed workout:")
        sb.appendLine("Title: ${workout.name}")
        exercises.forEach { ex ->
            sb.appendLine("Exercise: ${ex.name}")
            workoutRepository.getSetsByExerciseId(ex.id).forEach { set ->
                sb.appendLine("  - ${set.weight}kg x ${set.reps} reps RPE ${set.rpe ?: "-"}")
            }
        }

        val context = buildAiContextUseCase(userQuery = workout.name ?: "workout").text
        val prompt = "$context\n\n${sb}\n\nProvide a concise analysis: highlights, progression, and one actionable tip."

        return aiClient.generateResponse(prompt)
    }
}
