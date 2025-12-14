package com.example.hevyinsight.data.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized cache manager with TTL support.
 * Thread-safe cache for expensive operations like statistics calculations.
 */
class CacheManager {
    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()
    
    /**
     * Gets a cached value if it exists and is still valid
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, ttlMillis: Long): T? {
        val entry = cache[key] as? CacheEntry<T> ?: return null
        
        return if (entry.isValid(ttlMillis)) {
            entry.value
        } else {
            // Remove expired entry
            cache.remove(key)
            null
        }
    }
    
    /**
     * Puts a value in the cache
     */
    fun <T> put(key: String, value: T) {
        cache[key] = CacheEntry(value)
    }
    
    /**
     * Invalidates a specific cache entry
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }
    
    /**
     * Invalidates all cache entries matching a prefix
     */
    fun invalidatePrefix(prefix: String) {
        cache.keys.removeIf { it.startsWith(prefix) }
    }
    
    /**
     * Clears all cache entries
     */
    fun clear() {
        cache.clear()
    }
    
    /**
     * Removes expired entries (cleanup)
     */
    fun cleanup(ttlMillis: Long) {
        val keysToRemove = cache.entries
            .filter { (_, entry) -> !entry.isValid(ttlMillis) }
            .map { it.key }
        
        keysToRemove.forEach { cache.remove(it) }
    }
    
    /**
     * Gets cache statistics
     */
    fun getStats(): CacheStats {
        val totalEntries = cache.size
        val expiredEntries = cache.values.count { !it.isValid(Long.MAX_VALUE) }
        
        return CacheStats(
            totalEntries = totalEntries,
            expiredEntries = expiredEntries,
            validEntries = totalEntries - expiredEntries
        )
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val totalEntries: Int,
    val expiredEntries: Int,
    val validEntries: Int
)

/**
 * Cache key constants
 */
object CacheKeys {
    const val WORKOUT_STATS = "workout_stats"
    const val RECENT_PRS = "recent_prs"
    const val PRS_WITH_DETAILS = "prs_with_details"
    const val MUSCLE_GROUP_PROGRESS = "muscle_group_progress_"
    const val WEEKLY_PROGRESS = "weekly_progress_"
    const val VOLUME_TREND = "volume_trend"
    const val DURATION_TREND = "duration_trend_"
    const val EXERCISE_TEMPLATES = "exercise_templates"
    const val EXERCISE_TEMPLATES_FROM_EVENTS = "exercise_templates_from_events"
    const val ROUTINES = "routines"
}

/**
 * Cache TTL constants (in milliseconds)
 */
object CacheTTL {
    const val STATS = 5 * 60 * 1000L // 5 minutes
    const val PRS = 10 * 60 * 1000L // 10 minutes
    const val PROGRESS = 15 * 60 * 1000L // 15 minutes
    const val EXERCISE_TEMPLATES = 24 * 60 * 60 * 1000L // 24 hours
    const val EXERCISE_TEMPLATES_FROM_EVENTS = 24 * 60 * 60 * 1000L // 24 hours
    const val ROUTINES = 60 * 60 * 1000L // 1 hour
}
