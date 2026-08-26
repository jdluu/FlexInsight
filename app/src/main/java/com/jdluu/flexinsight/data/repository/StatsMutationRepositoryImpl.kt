package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.data.cache.CacheKeys
import com.jdluu.flexinsight.data.cache.CacheManager

/**
 * Mutation implementation for statistics: invalidates cached derived stats.
 */
class StatsMutationRepositoryImpl(
    private val cacheManager: CacheManager
) : StatsMutationRepository {
    override fun invalidateStatsCache() {
        cacheManager.invalidatePrefix(CacheKeys.WORKOUT_STATS)
        cacheManager.invalidatePrefix(CacheKeys.PRS_WITH_DETAILS)
        cacheManager.invalidatePrefix(CacheKeys.MUSCLE_GROUP_PROGRESS)
        cacheManager.invalidatePrefix(CacheKeys.WEEKLY_PROGRESS)
        cacheManager.invalidatePrefix(CacheKeys.VOLUME_TREND)
        cacheManager.invalidatePrefix(CacheKeys.DURATION_TREND)
    }
}
