package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.repository.StatsRepository
import javax.inject.Inject

class GetMuscleGroupProgressUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(weeks: Int): List<MuscleGroupProgress> =
        statsRepository.getMuscleGroupProgress(weeks)
}
