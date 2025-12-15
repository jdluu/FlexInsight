package com.example.flexinsight.data.cache

/**
 * Represents a cache entry with a value and timestamp
 */
data class CacheEntry<T>(
    val value: T,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this cache entry is still valid based on TTL
     */
    fun isValid(ttlMillis: Long): Boolean {
        val age = System.currentTimeMillis() - timestamp
        return age < ttlMillis
    }

    /**
     * Gets the age of this cache entry in milliseconds
     */
    fun getAge(): Long = System.currentTimeMillis() - timestamp
}
