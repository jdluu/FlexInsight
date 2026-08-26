package com.jdluu.flexinsight.data.repository

/**
 * Mutation concerns for statistics: invalidating cached derived stats.
 */
interface StatsMutationRepository {
    fun invalidateStatsCache()
}
