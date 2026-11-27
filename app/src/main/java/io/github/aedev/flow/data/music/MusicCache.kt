package io.github.aedev.flow.data.music

import io.github.aedev.flow.data.music.model.MusicTrack
import java.util.concurrent.ConcurrentHashMap

/**
 * Bounded, lock-free memory cache for music data to enable instant loading without global mutex
 * contention or unbounded memory growth.
 */
object MusicCache {
    // Cache with timestamp for expiration
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
    private const val MAX_TRENDING_ENTRIES = 8
    private const val MAX_GENRE_ENTRIES = 32
    private const val MAX_SEARCH_ENTRIES = 64
    private const val MAX_RELATED_ENTRIES = 64

    private val trendingCache = ConcurrentHashMap<String, CacheEntry<List<MusicTrack>>>()
    private val genreCache = ConcurrentHashMap<String, CacheEntry<List<MusicTrack>>>()
    private val searchCache = ConcurrentHashMap<String, CacheEntry<List<MusicTrack>>>()
    private val relatedCache = ConcurrentHashMap<String, CacheEntry<List<MusicTrack>>>()

    private fun <T> CacheEntry<T>.isValid(now: Long = System.currentTimeMillis()): Boolean = now - timestamp < CACHE_EXPIRY_MS

    private fun <T> getValid(
        cache: ConcurrentHashMap<String, CacheEntry<T>>,
        key: String,
    ): T? {
        val entry = cache[key] ?: return null
        val now = System.currentTimeMillis()
        if (!entry.isValid(now)) {
            cache.remove(key, entry)
            return null
        }
        return entry.data
    }

    private fun <T> putBounded(
        cache: ConcurrentHashMap<String, CacheEntry<T>>,
        key: String,
        data: T,
        maxEntries: Int,
    ) {
        val now = System.currentTimeMillis()
        cache[key] = CacheEntry(data, now)
        if (cache.size <= maxEntries) return

        // Drop expired entries first, then oldest entries until within cap.
        cache.entries.removeIf { !it.value.isValid(now) }
        if (cache.size > maxEntries) {
            val overflow = cache.size - maxEntries
            cache.entries
                .sortedBy { it.value.timestamp }
                .take(overflow)
                .forEach { cache.remove(it.key, it.value) }
        }
    }

    // Trending music
    suspend fun getTrendingMusic(limit: Int): List<MusicTrack>? = getValid(trendingCache, "trending_$limit")

    suspend fun cacheTrendingMusic(
        limit: Int,
        tracks: List<MusicTrack>,
    ) = putBounded(trendingCache, "trending_$limit", tracks, MAX_TRENDING_ENTRIES)

    // Genre tracks
    suspend fun getGenreTracks(
        genre: String,
        limit: Int,
    ): List<MusicTrack>? = getValid(genreCache, "${genre}_$limit")

    suspend fun cacheGenreTracks(
        genre: String,
        limit: Int,
        tracks: List<MusicTrack>,
    ) = putBounded(genreCache, "${genre}_$limit", tracks, MAX_GENRE_ENTRIES)

    // Search results
    suspend fun getSearchResults(query: String): List<MusicTrack>? = getValid(searchCache, query.lowercase().trim())

    suspend fun cacheSearchResults(
        query: String,
        tracks: List<MusicTrack>,
    ) = putBounded(searchCache, query.lowercase().trim(), tracks, MAX_SEARCH_ENTRIES)

    // Related music
    suspend fun getRelatedMusic(videoId: String): List<MusicTrack>? = getValid(relatedCache, videoId)

    suspend fun cacheRelatedMusic(
        videoId: String,
        tracks: List<MusicTrack>,
    ) = putBounded(relatedCache, videoId, tracks, MAX_RELATED_ENTRIES)

    // Clear specific cache
    suspend fun clearTrendingCache() {
        trendingCache.clear()
    }

    suspend fun clearGenreCache() {
        genreCache.clear()
    }

    suspend fun clearSearchCache() {
        searchCache.clear()
    }

    suspend fun clearRelatedCache() {
        relatedCache.clear()
    }

    // Clear all caches
    suspend fun clearAll() {
        trendingCache.clear()
        genreCache.clear()
        searchCache.clear()
        relatedCache.clear()
    }

    // Get cache stats for debugging
    fun getCacheStats(): CacheStats =
        CacheStats(
            trendingCount = trendingCache.size,
            genreCount = genreCache.size,
            searchCount = searchCache.size,
            relatedCount = relatedCache.size,
        )
}

data class CacheStats(
    val trendingCount: Int,
    val genreCount: Int,
    val searchCount: Int,
    val relatedCount: Int,
)
