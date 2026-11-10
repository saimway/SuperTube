package io.github.aedev.flow.data.movie

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : MovieRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private data class CacheEntry<T>(
        val value: T,
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(ttlMs: Long = CACHE_TTL_MS): Boolean =
            System.currentTimeMillis() - timestamp > ttlMs
    }

    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()

    private suspend fun fetchString(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("TMDB request failed with code ${response.code}: ${response.message}")
            }
            response.body?.string() ?: throw IOException("Empty response body from TMDB")
        }
    }

    private suspend inline fun <reified T> getCachedOrFetch(
        cacheKey: String,
        forceRefresh: Boolean,
        crossinline fetcher: suspend () -> T,
    ): Result<T> = runCatching {
        if (!forceRefresh) {
            @Suppress("UNCHECKED_CAST")
            val cached = cache[cacheKey] as? CacheEntry<T>
            if (cached != null && !cached.isExpired()) {
                return@runCatching cached.value
            }
        }
        val fetched = withContext(Dispatchers.Default) { fetcher() }
        cache[cacheKey] = CacheEntry(fetched)
        fetched
    }

    override suspend fun getTrendingMovies(page: Int, forceRefresh: Boolean): Result<List<MovieItem>> =
        getCachedOrFetch("trending_p$page", forceRefresh) {
            val url = "$BASE_URL/trending/movie/day?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(responseText)
            paged.results.map { it.toMovieItem() }
        }

    override suspend fun getPopularMovies(page: Int, forceRefresh: Boolean): Result<List<MovieItem>> =
        getCachedOrFetch("popular_p$page", forceRefresh) {
            val url = "$BASE_URL/movie/popular?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(responseText)
            paged.results.map { it.toMovieItem() }
        }

    override suspend fun getTopRatedMovies(page: Int, forceRefresh: Boolean): Result<List<MovieItem>> =
        getCachedOrFetch("top_rated_p$page", forceRefresh) {
            val url = "$BASE_URL/movie/top_rated?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(responseText)
            paged.results.map { it.toMovieItem() }
        }

    override suspend fun getUpcomingMovies(page: Int, forceRefresh: Boolean): Result<List<MovieItem>> =
        getCachedOrFetch("upcoming_p$page", forceRefresh) {
            val url = "$BASE_URL/movie/upcoming?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(responseText)
            paged.results.map { it.toMovieItem() }
        }

    override suspend fun getMoviesByGenre(genreIds: String, page: Int, forceRefresh: Boolean): Result<List<MovieItem>> =
        getCachedOrFetch("genre_${genreIds}_p$page", forceRefresh) {
            val url = "$BASE_URL/discover/movie?api_key=$API_KEY&with_genres=$genreIds&page=$page&sort_by=popularity.desc"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(responseText)
            paged.results.map { it.toMovieItem() }
        }

    override suspend fun searchMovies(query: String, page: Int): Result<List<MovieItem>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@runCatching emptyList()
        val encodedQuery = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        val cacheKey = "search_${encodedQuery}_p$page"
        @Suppress("UNCHECKED_CAST")
        val cached = cache[cacheKey] as? CacheEntry<List<MovieItem>>
        if (cached != null && !cached.isExpired(ttlMs = SEARCH_CACHE_TTL_MS)) {
            return@runCatching cached.value
        }
        val url = "$BASE_URL/search/movie?api_key=$API_KEY&query=$encodedQuery&page=$page"
        val responseText = fetchString(url)
        val items = withContext(Dispatchers.Default) {
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(responseText)
            paged.results.map { it.toMovieItem() }
        }
        cache[cacheKey] = CacheEntry(items)
        items
    }

    override suspend fun getMovieDetails(movieId: Long, forceRefresh: Boolean): Result<MovieDetail> =
        getCachedOrFetch("movie_detail_$movieId", forceRefresh) {
            val url = "$BASE_URL/movie/$movieId?api_key=$API_KEY&append_to_response=credits,recommendations,videos"
            val responseText = fetchString(url)
            val detailDto = json.decodeFromString<TmdbMovieDetailDto>(responseText)
            detailDto.toMovieDetail()
        }

    override suspend fun getTrendingTvShows(page: Int, forceRefresh: Boolean): Result<List<TvShowItem>> =
        getCachedOrFetch("trending_tv_p$page", forceRefresh) {
            val url = "$BASE_URL/trending/tv/day?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbTvShowDto>>(responseText)
            paged.results.map { it.toTvShowItem() }
        }

    override suspend fun getPopularTvShows(page: Int, forceRefresh: Boolean): Result<List<TvShowItem>> =
        getCachedOrFetch("popular_tv_p$page", forceRefresh) {
            val url = "$BASE_URL/tv/popular?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbTvShowDto>>(responseText)
            paged.results.map { it.toTvShowItem() }
        }

    override suspend fun getTopRatedTvShows(page: Int, forceRefresh: Boolean): Result<List<TvShowItem>> =
        getCachedOrFetch("top_rated_tv_p$page", forceRefresh) {
            val url = "$BASE_URL/tv/top_rated?api_key=$API_KEY&page=$page"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbTvShowDto>>(responseText)
            paged.results.map { it.toTvShowItem() }
        }

    override suspend fun getTvShowsByGenre(genreIds: String, page: Int, forceRefresh: Boolean): Result<List<TvShowItem>> =
        getCachedOrFetch("tv_genre_${genreIds}_p$page", forceRefresh) {
            val url = "$BASE_URL/discover/tv?api_key=$API_KEY&with_genres=$genreIds&page=$page&sort_by=popularity.desc"
            val responseText = fetchString(url)
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbTvShowDto>>(responseText)
            paged.results.map { it.toTvShowItem() }
        }

    override suspend fun searchTvShows(query: String, page: Int): Result<List<TvShowItem>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@runCatching emptyList()
        val encodedQuery = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        val cacheKey = "search_tv_${encodedQuery}_p$page"
        @Suppress("UNCHECKED_CAST")
        val cached = cache[cacheKey] as? CacheEntry<List<TvShowItem>>
        if (cached != null && !cached.isExpired(ttlMs = SEARCH_CACHE_TTL_MS)) {
            return@runCatching cached.value
        }
        val url = "$BASE_URL/search/tv?api_key=$API_KEY&query=$encodedQuery&page=$page"
        val responseText = fetchString(url)
        val items = withContext(Dispatchers.Default) {
            val paged = json.decodeFromString<TmdbPagedResponse<TmdbTvShowDto>>(responseText)
            paged.results.map { it.toTvShowItem() }
        }
        cache[cacheKey] = CacheEntry(items)
        items
    }

    override suspend fun getTvShowDetails(tvId: Long, forceRefresh: Boolean): Result<TvShowDetail> =
        getCachedOrFetch("tv_detail_$tvId", forceRefresh) {
            val url = "$BASE_URL/tv/$tvId?api_key=$API_KEY&append_to_response=credits,recommendations"
            val responseText = fetchString(url)
            val detailDto = json.decodeFromString<TmdbTvShowDetailDto>(responseText)
            detailDto.toTvShowDetail()
        }

    override suspend fun getSeasonEpisodes(tvId: Long, seasonNumber: Int, forceRefresh: Boolean): Result<List<TvEpisodeItem>> =
        getCachedOrFetch("tv_${tvId}_season_$seasonNumber", forceRefresh) {
            val url = "$BASE_URL/tv/$tvId/season/$seasonNumber?api_key=$API_KEY"
            val responseText = fetchString(url)
            val seasonDto = json.decodeFromString<TmdbTvSeasonDetailDto>(responseText)
            seasonDto.episodes.map { it.toTvEpisodeItem() }
        }

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3"
        private const val API_KEY = "297f1b91919bae59d50ed815f8d2e14c"
        private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15-minute TTL
        private const val SEARCH_CACHE_TTL_MS = 5 * 60 * 1000L // 5-minute TTL
    }
}
