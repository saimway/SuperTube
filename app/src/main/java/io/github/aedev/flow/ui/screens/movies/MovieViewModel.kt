package io.github.aedev.flow.ui.screens.movies

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.data.movie.MovieDetail
import io.github.aedev.flow.data.movie.MovieGenre
import io.github.aedev.flow.data.movie.MovieItem
import io.github.aedev.flow.data.movie.MovieRepository
import io.github.aedev.flow.data.movie.MovieServer
import io.github.aedev.flow.data.movie.MovieShelf
import io.github.aedev.flow.data.movie.MovieShelfCategory
import io.github.aedev.flow.data.movie.TvEpisodeItem
import io.github.aedev.flow.data.movie.TvShelf
import io.github.aedev.flow.data.movie.TvShelfCategory
import io.github.aedev.flow.data.movie.TvShowDetail
import io.github.aedev.flow.data.movie.TvShowItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MoviesUiState {
    data object Idle : MoviesUiState
    data object Loading : MoviesUiState
    data class Success(
        val heroMovie: MovieItem?,
        val shelves: List<MovieShelf>,
        val selectedGenre: MovieGenre? = null,
        val genreMovies: List<MovieItem> = emptyList(),
        val isGenreLoading: Boolean = false,
        val genres: List<MovieGenre> = AVAILABLE_GENRES,
    ) : MoviesUiState
    data class Error(val message: String) : MoviesUiState
}

@Immutable
data class MovieSearchUiState(
    val query: String = "",
    val results: List<MovieItem> = emptyList(),
    val isSearching: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
)

@Immutable
data class MovieDetailUiState(
    val detail: MovieDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedServer: MovieServer = MovieServer.DEFAULT_SERVER,
)

sealed interface TvUiState {
    data object Idle : TvUiState
    data object Loading : TvUiState
    data class Success(
        val heroShow: TvShowItem?,
        val shelves: List<TvShelf>,
        val selectedGenre: MovieGenre? = null,
        val genreShows: List<TvShowItem> = emptyList(),
        val isGenreLoading: Boolean = false,
        val genres: List<MovieGenre> = AVAILABLE_TV_GENRES,
    ) : TvUiState
    data class Error(val message: String) : TvUiState
}

@Immutable
data class TvDetailUiState(
    val detail: TvShowDetail? = null,
    val selectedSeasonNumber: Int = 1,
    val episodes: List<TvEpisodeItem> = emptyList(),
    val isEpisodesLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedServer: MovieServer = MovieServer.DEFAULT_SERVER,
)

val AVAILABLE_GENRES = listOf(
    MovieGenre(0, "All"),
    MovieGenre(28, "Action"),
    MovieGenre(12, "Adventure"),
    MovieGenre(16, "Animation"),
    MovieGenre(35, "Comedy"),
    MovieGenre(80, "Crime"),
    MovieGenre(99, "Documentary"),
    MovieGenre(18, "Drama"),
    MovieGenre(10751, "Family"),
    MovieGenre(14, "Fantasy"),
    MovieGenre(27, "Horror"),
    MovieGenre(9648, "Mystery"),
    MovieGenre(10749, "Romance"),
    MovieGenre(878, "Sci-Fi"),
    MovieGenre(53, "Thriller"),
    MovieGenre(10752, "War"),
    MovieGenre(37, "Western"),
)

val AVAILABLE_TV_GENRES = listOf(
    MovieGenre(0, "All"),
    MovieGenre(10759, "Action & Adventure"),
    MovieGenre(16, "Animation"),
    MovieGenre(35, "Comedy"),
    MovieGenre(80, "Crime"),
    MovieGenre(99, "Documentary"),
    MovieGenre(18, "Drama"),
    MovieGenre(10751, "Family"),
    MovieGenre(10762, "Kids"),
    MovieGenre(9648, "Mystery"),
    MovieGenre(10763, "News"),
    MovieGenre(10764, "Reality"),
    MovieGenre(10765, "Sci-Fi & Fantasy"),
    MovieGenre(10766, "Soap"),
    MovieGenre(10767, "Talk"),
    MovieGenre(10768, "War & Politics"),
    MovieGenre(37, "Western"),
)

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository,
) : ViewModel() {

    // Main movies feed state — initialized to Idle so ZERO network calls happen in init
    private val _uiState = MutableStateFlow<MoviesUiState>(MoviesUiState.Idle)
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    // Movie detail state
    private val _detailState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState())
    val detailState: StateFlow<MovieDetailUiState> = _detailState.asStateFlow()

    // TV feed state — initialized to Idle so ZERO network calls happen in init
    private val _tvUiState = MutableStateFlow<TvUiState>(TvUiState.Idle)
    val tvUiState: StateFlow<TvUiState> = _tvUiState.asStateFlow()

    // TV detail state
    private val _tvDetailState = MutableStateFlow(TvDetailUiState())
    val tvDetailState: StateFlow<TvDetailUiState> = _tvDetailState.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow(MovieSearchUiState())
    val searchState: StateFlow<MovieSearchUiState> = _searchState.asStateFlow()

    // Selected global streaming server
    private val _selectedServer = MutableStateFlow(MovieServer.DEFAULT_SERVER)
    val selectedServer: StateFlow<MovieServer> = _selectedServer.asStateFlow()

    private var feedJob: Job? = null
    private var shelfJob: Job? = null
    private var genreJob: Job? = null
    private var pagingJob: Job? = null
    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var tvFeedJob: Job? = null
    private var tvShelfJob: Job? = null
    private var tvGenreJob: Job? = null
    private var tvPagingJob: Job? = null
    private var tvDetailJob: Job? = null
    private var tvEpisodesJob: Job? = null

    // Ordered TV shelf categories to load on demand
    private val allTvCategories = listOf(
        TvShelfCategory.TRENDING,
        TvShelfCategory.POPULAR,
        TvShelfCategory.TOP_RATED,
        TvShelfCategory.SCIFI_FANTASY,
        TvShelfCategory.DRAMA,
        TvShelfCategory.COMEDY,
    )
    private var isLoadingNextTvShelf = false
    private val activeTvShelfPaging = java.util.concurrent.ConcurrentHashMap.newKeySet<TvShelfCategory>()

    init {
        // Observe search query with debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(350L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.trim().isEmpty()) {
                        _searchState.value = MovieSearchUiState()
                    } else {
                        performSearch(query, page = 1)
                    }
                }
        }
    }

    // Ordered shelf categories to load on demand as the user scrolls
    private val allCategories = listOf(
        MovieShelfCategory.TRENDING,
        MovieShelfCategory.POPULAR,
        MovieShelfCategory.TOP_RATED,
        MovieShelfCategory.ACTION_THRILLERS,
        MovieShelfCategory.SCIFI_FANTASY,
        MovieShelfCategory.COMEDY_HITS,
        MovieShelfCategory.UPCOMING,
    )

    private var isLoadingNextShelf = false
    private val activeShelfPaging = java.util.concurrent.ConcurrentHashMap.newKeySet<MovieShelfCategory>()

    /**
     * Strict Lazy Loading Contract:
     * Only fetches the Hero movie and Top (Trending) shelf when user opens Movies tab.
     * Subsequent shelves are loaded strictly on demand as user scrolls.
     * Cancels immediately if user navigates away.
     */
    fun loadMoviesIfNeeded(forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (current is MoviesUiState.Success && !forceRefresh) return
        if (current is MoviesUiState.Loading && !forceRefresh && feedJob?.isActive == true) return

        feedJob?.cancel()
        _uiState.value = MoviesUiState.Loading

        feedJob = viewModelScope.launch {
            try {
                // Fetch ONLY top shelf (Trending) initially to ensure zero lag / instantaneous rendering
                val trendingResult = repository.getTrendingMovies(page = 1, forceRefresh)
                val trendingList = trendingResult.getOrDefault(emptyList())

                if (trendingList.isEmpty()) {
                    _uiState.value = MoviesUiState.Error("No movies found. Check network connection.")
                    return@launch
                }

                val heroMovie = trendingList.firstOrNull()
                val initialShelves = listOf(
                    MovieShelf(MovieShelfCategory.TRENDING, trendingList),
                )

                _uiState.value = MoviesUiState.Success(
                    heroMovie = heroMovie,
                    shelves = initialShelves,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = MoviesUiState.Error(e.message ?: "Failed to load movies")
            }
        }
    }

    /**
     * Loads the next shelf in the sequence only when user scrolls near the bottom.
     */
    fun loadNextShelfBatch(forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (current !is MoviesUiState.Success || isLoadingNextShelf || shelfJob?.isActive == true) return

        val loadedCategories = current.shelves.map { it.category }.toSet()
        val nextCategory = allCategories.firstOrNull { it !in loadedCategories } ?: return

        isLoadingNextShelf = true
        shelfJob = viewModelScope.launch {
            try {
                val result = when (nextCategory) {
                    MovieShelfCategory.TRENDING -> repository.getTrendingMovies(page = 1, forceRefresh)
                    MovieShelfCategory.POPULAR -> repository.getPopularMovies(page = 1, forceRefresh)
                    MovieShelfCategory.TOP_RATED -> repository.getTopRatedMovies(page = 1, forceRefresh)
                    MovieShelfCategory.ACTION_THRILLERS -> repository.getMoviesByGenre(genreIds = "28,53", page = 1, forceRefresh)
                    MovieShelfCategory.SCIFI_FANTASY -> repository.getMoviesByGenre(genreIds = "878,14", page = 1, forceRefresh)
                    MovieShelfCategory.COMEDY_HITS -> repository.getMoviesByGenre(genreIds = "35", page = 1, forceRefresh)
                    MovieShelfCategory.UPCOMING -> repository.getUpcomingMovies(page = 1, forceRefresh)
                }

                val movies = result.getOrDefault(emptyList())
                if (movies.isNotEmpty()) {
                    val currentState = _uiState.value
                    if (currentState is MoviesUiState.Success) {
                        val newShelf = MovieShelf(nextCategory, movies)
                        _uiState.value = currentState.copy(
                            shelves = (currentState.shelves + newShelf).distinctBy { it.category },
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                isLoadingNextShelf = false
            }
        }
    }

    fun selectGenre(genre: MovieGenre?) {
        val current = _uiState.value
        if (current !is MoviesUiState.Success) return

        genreJob?.cancel()
        if (genre == null || genre.id == 0) {
            _uiState.value = current.copy(selectedGenre = null, genreMovies = emptyList(), isGenreLoading = false)
            return
        }

        _uiState.value = current.copy(selectedGenre = genre, isGenreLoading = true)

        genreJob = viewModelScope.launch {
            try {
                val result = repository.getMoviesByGenre(genreIds = genre.id.toString(), page = 1)
                val currentState = _uiState.value
                if (currentState is MoviesUiState.Success && currentState.selectedGenre?.id == genre.id) {
                    _uiState.value = currentState.copy(
                        genreMovies = result.getOrDefault(emptyList()),
                        isGenreLoading = false,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun loadMoreForShelf(category: MovieShelfCategory) {
        val current = _uiState.value
        if (current !is MoviesUiState.Success) return

        val shelf = current.shelves.firstOrNull { it.category == category } ?: return
        if (!shelf.hasMore || shelf.isLoadingMore || !activeShelfPaging.add(category)) return

        // Mark shelf loading
        _uiState.value = current.copy(
            shelves = current.shelves.map {
                if (it.category == category) it.copy(isLoadingMore = true) else it
            },
        )

        pagingJob = viewModelScope.launch {
            try {
                val nextPage = shelf.page + 1
                val result = when (category) {
                    MovieShelfCategory.TRENDING -> repository.getTrendingMovies(page = nextPage)
                    MovieShelfCategory.POPULAR -> repository.getPopularMovies(page = nextPage)
                    MovieShelfCategory.TOP_RATED -> repository.getTopRatedMovies(page = nextPage)
                    MovieShelfCategory.ACTION_THRILLERS -> repository.getMoviesByGenre(genreIds = "28,53", page = nextPage)
                    MovieShelfCategory.SCIFI_FANTASY -> repository.getMoviesByGenre(genreIds = "878,14", page = nextPage)
                    MovieShelfCategory.COMEDY_HITS -> repository.getMoviesByGenre(genreIds = "35", page = nextPage)
                    MovieShelfCategory.UPCOMING -> repository.getUpcomingMovies(page = nextPage)
                }

                val newMovies = result.getOrDefault(emptyList())
                val currentState = _uiState.value
                if (currentState is MoviesUiState.Success) {
                    _uiState.value = currentState.copy(
                        shelves = currentState.shelves.map {
                            if (it.category == category) {
                                val combined = (it.movies + newMovies).distinctBy { movie -> movie.id }
                                it.copy(
                                    movies = combined,
                                    page = nextPage,
                                    hasMore = newMovies.isNotEmpty(),
                                    isLoadingMore = false,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
            } finally {
                activeShelfPaging.remove(category)
            }
        }
    }

    fun loadMovieDetail(movieId: Long, forceRefresh: Boolean = false) {
        val current = _detailState.value
        if (!forceRefresh && current.detail?.id == movieId) {
            return
        }
        detailJob?.cancel()
        _detailState.value = MovieDetailUiState(isLoading = true, selectedServer = _selectedServer.value)

        detailJob = viewModelScope.launch {
            val result = repository.getMovieDetails(movieId, forceRefresh)
            result.fold(
                onSuccess = { detail ->
                    _detailState.value = MovieDetailUiState(
                        detail = detail,
                        isLoading = false,
                        selectedServer = _selectedServer.value,
                    )
                },
                onFailure = { error ->
                    _detailState.value = MovieDetailUiState(
                        isLoading = false,
                        error = error.message ?: "Failed to load movie details",
                        selectedServer = _selectedServer.value,
                    )
                },
            )
        }
    }

    fun selectServer(server: MovieServer) {
        _selectedServer.value = server
        _detailState.value = _detailState.value.copy(selectedServer = server)
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun performSearch(query: String, page: Int) {
        searchJob?.cancel()
        _searchState.value = _searchState.value.copy(isSearching = true, error = null)

        searchJob = viewModelScope.launch {
            val result = repository.searchMovies(query, page)
            result.fold(
                onSuccess = { movies ->
                    _searchState.value = _searchState.value.copy(
                        query = query,
                        results = if (page == 1) movies else (_searchState.value.results + movies).distinctBy { it.id },
                        isSearching = false,
                        page = page,
                        hasMore = movies.isNotEmpty(),
                    )
                },
                onFailure = { error ->
                    _searchState.value = _searchState.value.copy(
                        isSearching = false,
                        error = error.message ?: "Search failed",
                    )
                },
            )
        }
    }

    fun loadMoreSearchResults() {
        val state = _searchState.value
        if (!state.hasMore || state.isSearching || state.query.isBlank()) return
        performSearch(state.query, state.page + 1)
    }

    /**
     * Strict Lazy Loading Contract for TV:
     * Only fetches the Hero TV show and Top (Trending) shelf when user opens TV shows tab.
     * Subsequent shelves are loaded strictly on demand as user scrolls.
     * Cancels immediately if user navigates away.
     */
    fun loadTvShowsIfNeeded(forceRefresh: Boolean = false) {
        val current = _tvUiState.value
        if (current is TvUiState.Success && !forceRefresh) return
        if (current is TvUiState.Loading && !forceRefresh && tvFeedJob?.isActive == true) return

        tvFeedJob?.cancel()
        _tvUiState.value = TvUiState.Loading

        tvFeedJob = viewModelScope.launch {
            try {
                // Fetch ONLY top shelf (Trending) initially to ensure zero lag / instantaneous rendering
                val trendingResult = repository.getTrendingTvShows(page = 1, forceRefresh)
                val trendingList = trendingResult.getOrDefault(emptyList())

                if (trendingList.isEmpty()) {
                    _tvUiState.value = TvUiState.Error("No TV shows found. Check network connection.")
                    return@launch
                }

                val heroShow = trendingList.firstOrNull()
                val initialShelves = listOf(
                    TvShelf(TvShelfCategory.TRENDING, trendingList),
                )

                _tvUiState.value = TvUiState.Success(
                    heroShow = heroShow,
                    shelves = initialShelves,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _tvUiState.value = TvUiState.Error(e.message ?: "Failed to load TV shows")
            }
        }
    }

    /**
     * Loads the next TV shelf in the sequence only when user scrolls near the bottom.
     */
    fun loadNextTvShelfBatch(forceRefresh: Boolean = false) {
        val current = _tvUiState.value
        if (current !is TvUiState.Success || isLoadingNextTvShelf || tvShelfJob?.isActive == true) return

        val loadedCategories = current.shelves.map { it.category }.toSet()
        val nextCategory = allTvCategories.firstOrNull { it !in loadedCategories } ?: return

        isLoadingNextTvShelf = true
        tvShelfJob = viewModelScope.launch {
            try {
                val result = when (nextCategory) {
                    TvShelfCategory.TRENDING -> repository.getTrendingTvShows(page = 1, forceRefresh)
                    TvShelfCategory.POPULAR -> repository.getPopularTvShows(page = 1, forceRefresh)
                    TvShelfCategory.TOP_RATED -> repository.getTopRatedTvShows(page = 1, forceRefresh)
                    TvShelfCategory.SCIFI_FANTASY -> repository.getTvShowsByGenre(genreIds = "10765", page = 1, forceRefresh)
                    TvShelfCategory.DRAMA -> repository.getTvShowsByGenre(genreIds = "18", page = 1, forceRefresh)
                    TvShelfCategory.COMEDY -> repository.getTvShowsByGenre(genreIds = "35", page = 1, forceRefresh)
                }

                val shows = result.getOrDefault(emptyList())
                if (shows.isNotEmpty()) {
                    val currentState = _tvUiState.value
                    if (currentState is TvUiState.Success) {
                        val newShelf = TvShelf(nextCategory, shows)
                        _tvUiState.value = currentState.copy(
                            shelves = (currentState.shelves + newShelf).distinctBy { it.category },
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                isLoadingNextTvShelf = false
            }
        }
    }

    fun selectTvGenre(genre: MovieGenre?) {
        val current = _tvUiState.value
        if (current !is TvUiState.Success) return

        tvGenreJob?.cancel()
        if (genre == null || genre.id == 0) {
            _tvUiState.value = current.copy(selectedGenre = null, genreShows = emptyList(), isGenreLoading = false)
            return
        }

        _tvUiState.value = current.copy(selectedGenre = genre, isGenreLoading = true)

        tvGenreJob = viewModelScope.launch {
            try {
                val result = repository.getTvShowsByGenre(genreIds = genre.id.toString(), page = 1)
                val currentState = _tvUiState.value
                if (currentState is TvUiState.Success && currentState.selectedGenre?.id == genre.id) {
                    _tvUiState.value = currentState.copy(
                        genreShows = result.getOrDefault(emptyList()),
                        isGenreLoading = false,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun loadMoreForTvShelf(category: TvShelfCategory) {
        val current = _tvUiState.value
        if (current !is TvUiState.Success) return

        val shelf = current.shelves.firstOrNull { it.category == category } ?: return
        if (!shelf.hasMore || shelf.isLoadingMore || !activeTvShelfPaging.add(category)) return

        _tvUiState.value = current.copy(
            shelves = current.shelves.map {
                if (it.category == category) it.copy(isLoadingMore = true) else it
            },
        )

        tvPagingJob = viewModelScope.launch {
            try {
                val nextPage = shelf.page + 1
                val result = when (category) {
                    TvShelfCategory.TRENDING -> repository.getTrendingTvShows(page = nextPage)
                    TvShelfCategory.POPULAR -> repository.getPopularTvShows(page = nextPage)
                    TvShelfCategory.TOP_RATED -> repository.getTopRatedTvShows(page = nextPage)
                    TvShelfCategory.SCIFI_FANTASY -> repository.getTvShowsByGenre(genreIds = "10765", page = nextPage)
                    TvShelfCategory.DRAMA -> repository.getTvShowsByGenre(genreIds = "18", page = nextPage)
                    TvShelfCategory.COMEDY -> repository.getTvShowsByGenre(genreIds = "35", page = nextPage)
                }

                val newShows = result.getOrDefault(emptyList())
                val currentState = _tvUiState.value
                if (currentState is TvUiState.Success) {
                    _tvUiState.value = currentState.copy(
                        shelves = currentState.shelves.map {
                            if (it.category == category) {
                                val combined = (it.shows + newShows).distinctBy { show -> show.id }
                                it.copy(
                                    shows = combined,
                                    page = nextPage,
                                    hasMore = newShows.isNotEmpty(),
                                    isLoadingMore = false,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
            } finally {
                activeTvShelfPaging.remove(category)
            }
        }
    }

    fun loadTvDetail(tvId: Long, forceRefresh: Boolean = false) {
        val current = _tvDetailState.value
        if (!forceRefresh && current.detail?.id == tvId) {
            return
        }
        val preservedSeason = if (current.selectedSeasonNumber > 0) current.selectedSeasonNumber else 0
        tvDetailJob?.cancel()
        _tvDetailState.value = TvDetailUiState(
            isLoading = true,
            selectedSeasonNumber = if (preservedSeason > 0) preservedSeason else 1,
            selectedServer = _selectedServer.value,
        )

        tvDetailJob = viewModelScope.launch {
            val result = repository.getTvShowDetails(tvId, forceRefresh)
            result.fold(
                onSuccess = { detail ->
                    val seasonToSelect = if (preservedSeason > 0 && detail.seasons.any { it.seasonNumber == preservedSeason }) {
                        preservedSeason
                    } else if (_tvDetailState.value.selectedSeasonNumber > 0 && detail.seasons.any { it.seasonNumber == _tvDetailState.value.selectedSeasonNumber }) {
                        _tvDetailState.value.selectedSeasonNumber
                    } else {
                        detail.seasons.firstOrNull { it.seasonNumber > 0 }?.seasonNumber
                            ?: detail.seasons.firstOrNull()?.seasonNumber ?: 1
                    }

                    _tvDetailState.value = TvDetailUiState(
                        detail = detail,
                        selectedSeasonNumber = seasonToSelect,
                        isLoading = false,
                        selectedServer = _selectedServer.value,
                    )
                    loadSeasonEpisodes(tvId, seasonToSelect, forceRefresh)
                },
                onFailure = { error ->
                    _tvDetailState.value = TvDetailUiState(
                        isLoading = false,
                        error = error.message ?: "Failed to load TV details",
                        selectedServer = _selectedServer.value,
                    )
                },
            )
        }
    }

    fun selectSeason(tvId: Long, seasonNumber: Int, forceRefresh: Boolean = false) {
        _tvDetailState.value = _tvDetailState.value.copy(selectedSeasonNumber = seasonNumber)
        loadSeasonEpisodes(tvId, seasonNumber, forceRefresh)
    }

    fun loadSeasonEpisodes(tvId: Long, seasonNumber: Int, forceRefresh: Boolean = false) {
        tvEpisodesJob?.cancel()
        _tvDetailState.value = _tvDetailState.value.copy(isEpisodesLoading = true)

        tvEpisodesJob = viewModelScope.launch {
            val result = repository.getSeasonEpisodes(tvId, seasonNumber, forceRefresh)
            result.fold(
                onSuccess = { episodes ->
                    _tvDetailState.value = _tvDetailState.value.copy(
                        episodes = episodes,
                        isEpisodesLoading = false,
                    )
                },
                onFailure = {
                    _tvDetailState.value = _tvDetailState.value.copy(
                        isEpisodesLoading = false,
                    )
                },
            )
        }
    }

    fun selectTvServer(server: MovieServer) {
        _selectedServer.value = server
        _tvDetailState.value = _tvDetailState.value.copy(selectedServer = server)
    }

    /**
     * Immediately cancels all active network/coroutine jobs for the main feed,
     * shelves, and genre filtering so zero background processing or network
     * usage occurs when the user leaves the Movies screen.
     */
    fun cancelActiveFetches() {
        feedJob?.cancel()
        feedJob = null
        shelfJob?.cancel()
        shelfJob = null
        genreJob?.cancel()
        genreJob = null
        pagingJob?.cancel()
        pagingJob = null
        activeShelfPaging.clear()
        isLoadingNextShelf = false
        if (_uiState.value is MoviesUiState.Loading) {
            _uiState.value = MoviesUiState.Idle
        }
        cancelTvActiveFetches()
    }

    /**
     * Immediately cancels all active network/coroutine jobs for TV feeds,
     * shelves, and genre filtering.
     */
    fun cancelTvActiveFetches() {
        tvFeedJob?.cancel()
        tvFeedJob = null
        tvShelfJob?.cancel()
        tvShelfJob = null
        tvGenreJob?.cancel()
        tvGenreJob = null
        tvPagingJob?.cancel()
        tvPagingJob = null
        activeTvShelfPaging.clear()
        isLoadingNextTvShelf = false
        if (_tvUiState.value is TvUiState.Loading) {
            _tvUiState.value = TvUiState.Idle
        }
    }

    /**
     * Cancels active movie detail fetching when user leaves the detail/player screen.
     */
    fun cancelDetailFetch() {
        detailJob?.cancel()
        detailJob = null
    }

    /**
     * Cancels active TV show detail and episodes fetching when user leaves detail/player screen.
     */
    fun cancelTvDetailFetch() {
        tvDetailJob?.cancel()
        tvDetailJob = null
        tvEpisodesJob?.cancel()
        tvEpisodesJob = null
    }

    /**
     * Cancels active search requests when user leaves the search screen.
     */
    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
    }
}
