package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.domain.model.WorkoutStats
import com.jdluu.flexinsight.domain.repository.StatsRepository
import javax.inject.Inject

class GetWorkoutStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): WorkoutStats = statsRepository.calculateStats()
}
