package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.domain.calc.TrainingLoadCalculator
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

        val volumeScore = TrainingLoadCalculator.hevyVolumeScore(
            goalCompleted = goal?.completed,
            goalTarget = goal?.target,
            totalWorkouts = stats?.totalWorkouts
        )
        val cardioScore = TrainingLoadCalculator.cardioScore(health.cardioSessionsThisWeek)
        val sleepScore = TrainingLoadCalculator.sleepScore(health.sleepHoursLastNight)

        val overall = TrainingLoadCalculator.overall(volumeScore, cardioScore, sleepScore)

        val label = TrainingLoadCalculator.label(overall)

        val detail = TrainingLoadCalculator.detail(
            volumeScore = volumeScore,
            hasHealthData = health.hasData,
            sleepHoursLastNight = health.sleepHoursLastNight,
            stepsToday = health.stepsToday,
            cardioSessionsThisWeek = health.cardioSessionsThisWeek
        )

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
