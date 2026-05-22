package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.domain.model.DeloadAlert
import javax.inject.Inject

class DetectDeloadUseCase @Inject constructor(
    private val flexRepository: FlexRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(): DeloadAlert {
        val trend = runCatching { flexRepository.calculateVolumeTrend(weeks = 4) }.getOrNull()
        val health = healthConnectRepository.readSnapshot()
        val consistency = runCatching { flexRepository.getConsistencyData(14) }.getOrDefault(emptyList())

        val volumeRising = trend != null && trend.percentageChange > 5.0
        val sessionsHigh = consistency.count { it.hasWorkout } >= 8
        val poorSleep = health.sleepHoursLastNight != null && health.sleepHoursLastNight < 6.0
        val lowRestingHr = health.restingHeartRateBpm != null && health.restingHeartRateBpm > 75

        val shouldDeload = volumeRising && sessionsHigh && (poorSleep || lowRestingHr)

        val message = if (shouldDeload) {
            "Volume is up ${trend?.percentageChange?.let { "%.0f".format(it) } ?: ""}% while recovery signals are down. " +
                "Consider a deload week: reduce intensity 30–40% and prioritize sleep."
        } else {
            ""
        }

        return DeloadAlert(shouldDeload = shouldDeload, message = message)
    }
}
