package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.data.repository.StatsRepository
import javax.inject.Inject

class GetPRDetailsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(limit: Int): List<PRDetails> =
        statsRepository.getPRsWithDetails(limit)
}
