package io.github.aedev.flow.data.movie

import androidx.compose.runtime.Immutable
import io.github.aedev.flow.R

enum class MovieShelfCategory(val id: String, val titleRes: Int) {
    TRENDING("trending", R.string.movie_shelf_trending),
    POPULAR("popular", R.string.movie_shelf_popular),
    TOP_RATED("top_rated", R.string.movie_shelf_top_rated),
    ACTION_THRILLERS("action_thrillers", R.string.movie_shelf_action_thrillers),
    SCIFI_FANTASY("scifi_fantasy", R.string.movie_shelf_scifi_fantasy),
    COMEDY_HITS("comedy_hits", R.string.movie_shelf_comedy_hits),
    UPCOMING("upcoming", R.string.movie_shelf_upcoming),
}

@Immutable
data class MovieShelf(
    val category: MovieShelfCategory,
    val movies: List<MovieItem>,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
)

interface MovieRepository {
    suspend fun getTrendingMovies(page: Int = 1, forceRefresh: Boolean = false): Result<List<MovieItem>>
    suspend fun getPopularMovies(page: Int = 1, forceRefresh: Boolean = false): Result<List<MovieItem>>
    suspend fun getTopRatedMovies(page: Int = 1, forceRefresh: Boolean = false): Result<List<MovieItem>>
    suspend fun getUpcomingMovies(page: Int = 1, forceRefresh: Boolean = false): Result<List<MovieItem>>
    suspend fun getMoviesByGenre(genreIds: String, page: Int = 1, forceRefresh: Boolean = false): Result<List<MovieItem>>
    suspend fun searchMovies(query: String, page: Int = 1): Result<List<MovieItem>>
    suspend fun getMovieDetails(movieId: Long, forceRefresh: Boolean = false): Result<MovieDetail>
}
