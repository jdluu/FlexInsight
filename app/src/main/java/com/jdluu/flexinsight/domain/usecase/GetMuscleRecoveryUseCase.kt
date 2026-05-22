package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.MuscleGroup
import com.jdluu.flexinsight.data.repository.StatsRepository
import javax.inject.Inject

class GetMuscleRecoveryUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Map<MuscleGroup, Float> =
        statsRepository.getMuscleRecoveryStatus()
}
