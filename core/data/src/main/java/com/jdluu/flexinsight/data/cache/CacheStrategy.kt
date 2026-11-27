package com.jdluu.flexinsight.data.cache

import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Strategy for handling cached data retrieval.
 * Implements the "Cache-Aside" or "Read-Through" pattern with double-checked locking optimization.
 */
class CacheStrategy @Inject constructor(
    private val cacheManager: CacheManager,
    private val dispatcherProvider: DispatcherProvider
) {

    /**
     * Tries to get value from cache. If missing/expired, executes [fetcher], caches the result, and returns it.
     * Uses [dispatcherProvider.default] for the fetch operation.
     */
    suspend fun <T> getOrFetch(
        key: String,
        ttl: Long,
        fetcher: suspend () -> T
    ): T {
        // 1. Fast path: Check cache immediately
        val cached = cacheManager.get<T>(key, ttl)
        if (cached != null) {
            return cached
        }

        // 2. Slow path: Switch context and fetch
        return withContext(dispatcherProvider.default) {
            // 3. Double-check: Another thread might have populated the cache while we were switching context
            val cachedInContext = cacheManager.get<T>(key, ttl)
            if (cachedInContext != null) {
                return@withContext cachedInContext
            }

            // 4. Fetch and Cache
            val result = fetcher()
            cacheManager.put(key, result)
            result
        }
    }
}
