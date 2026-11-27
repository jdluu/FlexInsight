package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.domain.model.WeeklyProgress
import com.jdluu.flexinsight.domain.repository.StatsRepository
import javax.inject.Inject

class GetWeeklyProgressUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(weeks: Int): List<WeeklyProgress> =
        statsRepository.getWeeklyProgress(weeks)
}
