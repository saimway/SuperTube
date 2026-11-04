package io.github.aedev.flow.ui.screens.movies

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.movie.MovieDetail
import io.github.aedev.flow.data.movie.MovieGenre
import io.github.aedev.flow.data.movie.MovieItem
import io.github.aedev.flow.data.movie.MovieRepository
import io.github.aedev.flow.data.movie.MovieServer
import io.github.aedev.flow.data.movie.MovieShelfCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: MovieRepository = mockk(relaxed = true)

    private val dummyMovie = MovieItem(
        id = 101L,
        title = "Inception",
        overview = "A thief who steals corporate secrets...",
        posterPath = "/path.jpg",
        releaseDate = "2010-07-16",
        voteAverage = 8.8,
    )

    private val dummyDetail = MovieDetail(
        id = 101L,
        title = "Inception",
        overview = "A thief who steals corporate secrets...",
        runtime = 148,
        releaseDate = "2010-07-16",
        voteAverage = 8.8,
    )

    private fun createViewModel() = MovieViewModel(repository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getTrendingMovies(any(), any()) } returns Result.success(listOf(dummyMovie))
        coEvery { repository.getPopularMovies(any(), any()) } returns Result.success(listOf(dummyMovie))
        coEvery { repository.getTopRatedMovies(any(), any()) } returns Result.success(listOf(dummyMovie))
        coEvery { repository.getUpcomingMovies(any(), any()) } returns Result.success(listOf(dummyMovie))
        coEvery { repository.getMoviesByGenre(any(), any(), any()) } returns Result.success(listOf(dummyMovie))
        coEvery { repository.getMovieDetails(any(), any()) } returns Result.success(dummyDetail)
        coEvery { repository.searchMovies(any(), any()) } returns Result.success(listOf(dummyMovie))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is Idle with zero repository calls`() = runTest {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(MoviesUiState.Idle)
        coVerify(exactly = 0) { repository.getTrendingMovies(any(), any()) }
        coVerify(exactly = 0) { repository.getPopularMovies(any(), any()) }
    }

    @Test
    fun `loadMoviesIfNeeded fetches hero and initial shelves and transitions to Success`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadMoviesIfNeeded()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MoviesUiState.Success::class.java)

        val success = state as MoviesUiState.Success
        assertThat(success.heroMovie?.id).isEqualTo(101L)
        assertThat(success.shelves).isNotEmpty()
        coVerify(atLeast = 1) { repository.getTrendingMovies(page = 1, forceRefresh = false) }
    }

    @Test
    fun `loadMoviesIfNeeded does not refetch if already in Success and not forced`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadMoviesIfNeeded()
        testScheduler.advanceUntilIdle()

        // Call again without forceRefresh
        viewModel.loadMoviesIfNeeded(forceRefresh = false)
        testScheduler.advanceUntilIdle()

        // Verify only 1 batch was requested
        coVerify(exactly = 1) { repository.getTrendingMovies(page = 1, forceRefresh = false) }
    }

    @Test
    fun `selectGenre loads movies for selected genre and resets on null or All`() = runTest {
        val viewModel = createViewModel()
        viewModel.loadMoviesIfNeeded()
        testScheduler.advanceUntilIdle()

        val actionGenre = MovieGenre(28, "Action")
        viewModel.selectGenre(actionGenre)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as MoviesUiState.Success
        assertThat(state.selectedGenre?.id).isEqualTo(28)
        assertThat(state.genreMovies).isNotEmpty()
        coVerify(atLeast = 1) { repository.getMoviesByGenre(genreIds = "28", page = 1) }

        // Reset to All
        viewModel.selectGenre(MovieGenre(0, "All"))
        val resetState = viewModel.uiState.value as MoviesUiState.Success
        assertThat(resetState.selectedGenre).isNull()
        assertThat(resetState.genreMovies).isEmpty()
    }

    @Test
    fun `loadMoreForShelf appends new movies to that shelf`() = runTest {
        val viewModel = createViewModel()
        viewModel.loadMoviesIfNeeded()
        testScheduler.advanceUntilIdle()

        val moviePage2 = MovieItem(id = 202L, title = "The Dark Knight", voteAverage = 9.0)
        coEvery { repository.getTrendingMovies(page = 2, any()) } returns Result.success(listOf(moviePage2))

        viewModel.loadMoreForShelf(MovieShelfCategory.TRENDING)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as MoviesUiState.Success
        val trendingShelf = state.shelves.first { it.category == MovieShelfCategory.TRENDING }
        assertThat(trendingShelf.page).isEqualTo(2)
        assertThat(trendingShelf.movies.map { it.id }).containsExactly(101L, 202L)
    }

    @Test
    fun `loadMovieDetail fetches movie details and updates detail state`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadMovieDetail(101L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.detail?.title).isEqualTo("Inception")
        assertThat(state.detail?.runtimeFormatted).isEqualTo("2h 28m")
    }

    @Test
    fun `selectServer updates selected server in state`() = runTest {
        val viewModel = createViewModel()
        val customServer = MovieServer.SERVERS.first { it.id == "supernova" }

        viewModel.selectServer(customServer)

        assertThat(viewModel.selectedServer.value.id).isEqualTo("supernova")
        assertThat(viewModel.detailState.value.selectedServer.id).isEqualTo("supernova")
    }

    @Test
    fun `search query debounces and updates search state`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChanged("Matrix")
        advanceTimeBy(400L)
        testScheduler.advanceUntilIdle()

        val searchState = viewModel.searchState.value
        assertThat(searchState.query).isEqualTo("Matrix")
        assertThat(searchState.results).isNotEmpty()
        coVerify(atLeast = 1) { repository.searchMovies("Matrix", page = 1) }
    }
}
