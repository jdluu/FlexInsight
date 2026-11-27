package com.jdluu.flexinsight.domain.repository

/**
 * Mutation concerns for statistics: invalidating cached derived stats.
 */
interface StatsMutationRepository {
    fun invalidateStatsCache()
}
