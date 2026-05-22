package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.domain.model.TrainingLoadScore
import javax.inject.Inject

class CalculateTrainingLoadUseCase @Inject constructor(
    private val flexRepository: FlexRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(): TrainingLoadScore {
        val stats = runCatching { flexRepository.calculateStats() }.getOrNull()
        val goal = runCatching { flexRepository.getWeeklyGoalProgress() }.getOrNull()
        val health: HealthConnectSnapshot = healthConnectRepository.readSnapshot()

        val volumeScore = when {
            goal != null && goal.target > 0 ->
                ((goal.completed.toFloat() / goal.target) * 100).toInt().coerceIn(0, 100)
            stats != null && stats.totalWorkouts > 0 -> 65
            else -> 30
        }

        val cardioScore = (health.cardioSessionsThisWeek * 15).coerceIn(0, 100)
        val sleepScore = when (val h = health.sleepHoursLastNight) {
            null -> 50
            in 7.0..Double.MAX_VALUE -> 90
            in 6.0..7.0 -> 70
            in 5.0..6.0 -> 50
            else -> 25
        }

        val overall = ((volumeScore * 0.5f) + (cardioScore * 0.2f) + (sleepScore * 0.3f)).toInt()
            .coerceIn(0, 100)

        val label = when {
            overall >= 80 -> "High load"
            overall >= 55 -> "Moderate load"
            else -> "Recovery focus"
        }

        val detail = buildString {
            append("Hevy volume $volumeScore/100")
            if (health.hasData) {
                health.sleepHoursLastNight?.let { append(", sleep ${"%.1f".format(it)}h") }
                health.stepsToday?.let { append(", steps $it") }
                if (health.cardioSessionsThisWeek > 0) {
                    append(", ${health.cardioSessionsThisWeek} cardio sessions")
                }
            }
        }

        return TrainingLoadScore(
            overall = overall,
            hevyVolumeScore = volumeScore,
            cardioScore = cardioScore,
            sleepScore = sleepScore,
            label = label,
            detail = detail
        )
    }
}
