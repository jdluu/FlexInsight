package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.domain.model.PRDetails
import com.jdluu.flexinsight.domain.repository.StatsRepository
import javax.inject.Inject

class GetPRDetailsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(limit: Int): List<PRDetails> =
        statsRepository.getPRsWithDetails(limit)
}
