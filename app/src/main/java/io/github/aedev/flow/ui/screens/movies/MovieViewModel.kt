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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
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
    }

    /**
     * Cancels active movie detail fetching when user leaves the detail/player screen.
     */
    fun cancelDetailFetch() {
        detailJob?.cancel()
        detailJob = null
    }

    /**
     * Cancels active search requests when user leaves the search screen.
     */
    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
    }
}
