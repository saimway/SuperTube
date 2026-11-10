package io.github.aedev.flow.ui.screens.movies

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.movie.MovieGenre
import io.github.aedev.flow.data.movie.MovieRepository
import io.github.aedev.flow.data.movie.TvEpisodeItem
import io.github.aedev.flow.data.movie.TvSeasonSummary
import io.github.aedev.flow.data.movie.TvShelfCategory
import io.github.aedev.flow.data.movie.TvShowDetail
import io.github.aedev.flow.data.movie.TvShowItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTvTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: MovieRepository = mockk(relaxed = true)

    private val dummyTvShow = TvShowItem(
        id = 1396L,
        name = "Breaking Bad",
        overview = "A chemistry teacher diagnosed with lung cancer...",
        posterPath = "/poster.jpg",
        firstAirDate = "2008-01-20",
        voteAverage = 8.9,
    )

    private val dummyTvDetail = TvShowDetail(
        id = 1396L,
        name = "Breaking Bad",
        overview = "A chemistry teacher diagnosed with lung cancer...",
        numberOfSeasons = 5,
        numberOfEpisodes = 62,
        firstAirDate = "2008-01-20",
        voteAverage = 8.9,
        seasons = listOf(
            TvSeasonSummary(id = 1L, seasonNumber = 1, name = "Season 1", episodeCount = 7),
            TvSeasonSummary(id = 2L, seasonNumber = 2, name = "Season 2", episodeCount = 13),
        ),
    )

    private val dummyEpisodes = listOf(
        TvEpisodeItem(
            id = 101L,
            episodeNumber = 1,
            seasonNumber = 1,
            name = "Pilot",
            airDate = "2008-01-20",
            voteAverage = 8.2,
            runtime = 58,
        ),
        TvEpisodeItem(
            id = 102L,
            episodeNumber = 2,
            seasonNumber = 1,
            name = "Cat's in the Bag...",
            airDate = "2008-01-27",
            voteAverage = 8.3,
            runtime = 48,
        ),
    )

    private fun createViewModel() = MovieViewModel(repository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getTrendingTvShows(any(), any()) } returns Result.success(listOf(dummyTvShow))
        coEvery { repository.getPopularTvShows(any(), any()) } returns Result.success(listOf(dummyTvShow))
        coEvery { repository.getTopRatedTvShows(any(), any()) } returns Result.success(listOf(dummyTvShow))
        coEvery { repository.getTvShowsByGenre(any(), any(), any()) } returns Result.success(listOf(dummyTvShow))
        coEvery { repository.getTvShowDetails(any(), any()) } returns Result.success(dummyTvDetail)
        coEvery { repository.getSeasonEpisodes(any(), any(), any()) } returns Result.success(dummyEpisodes)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial tv state is Idle with zero TV repository calls`() = runTest {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.tvUiState.value).isEqualTo(TvUiState.Idle)
        coVerify(exactly = 0) { repository.getTrendingTvShows(any(), any()) }
        coVerify(exactly = 0) { repository.getPopularTvShows(any(), any()) }
    }

    @Test
    fun `loadTvShowsIfNeeded fetches hero show and initial shelf and transitions to Success`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadTvShowsIfNeeded()
        testScheduler.advanceUntilIdle()

        val state = viewModel.tvUiState.value
        assertThat(state).isInstanceOf(TvUiState.Success::class.java)

        val success = state as TvUiState.Success
        assertThat(success.heroShow?.id).isEqualTo(1396L)
        assertThat(success.heroShow?.name).isEqualTo("Breaking Bad")
        assertThat(success.shelves).isNotEmpty()
        assertThat(success.shelves[0].category).isEqualTo(TvShelfCategory.TRENDING)
        coVerify(atLeast = 1) { repository.getTrendingTvShows(page = 1, forceRefresh = false) }
    }

    @Test
    fun `loadTvShowsIfNeeded does not refetch if already in Success and not forced`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadTvShowsIfNeeded()
        testScheduler.advanceUntilIdle()

        viewModel.loadTvShowsIfNeeded()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getTrendingTvShows(page = 1, forceRefresh = false) }
    }

    @Test
    fun `loadNextTvShelfBatch appends next TV shelf category`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadTvShowsIfNeeded()
        testScheduler.advanceUntilIdle()

        val stateBefore = viewModel.tvUiState.value as TvUiState.Success
        assertThat(stateBefore.shelves.size).isEqualTo(1)

        viewModel.loadNextTvShelfBatch()
        testScheduler.advanceUntilIdle()

        val stateAfter = viewModel.tvUiState.value as TvUiState.Success
        assertThat(stateAfter.shelves.size).isEqualTo(2)
        assertThat(stateAfter.shelves[1].category).isEqualTo(TvShelfCategory.POPULAR)
        coVerify(atLeast = 1) { repository.getPopularTvShows(page = 1, forceRefresh = false) }
    }

    @Test
    fun `selectTvGenre fetches shows by genre and updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadTvShowsIfNeeded()
        testScheduler.advanceUntilIdle()

        val dramaGenre = MovieGenre(18, "Drama")
        viewModel.selectTvGenre(dramaGenre)
        testScheduler.advanceUntilIdle()

        val state = viewModel.tvUiState.value as TvUiState.Success
        assertThat(state.selectedGenre?.id).isEqualTo(18)
        assertThat(state.genreShows).isNotEmpty()
        coVerify(atLeast = 1) { repository.getTvShowsByGenre(genreIds = "18", page = 1) }
    }

    @Test
    fun `loadTvDetail fetches detail and automatically loads initial season episodes`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadTvDetail(1396L)
        testScheduler.advanceUntilIdle()

        val detailState = viewModel.tvDetailState.value
        assertThat(detailState.detail?.id).isEqualTo(1396L)
        assertThat(detailState.selectedSeasonNumber).isEqualTo(1)
        assertThat(detailState.episodes.size).isEqualTo(2)
        assertThat(detailState.episodes[0].name).isEqualTo("Pilot")

        coVerify(exactly = 1) { repository.getTvShowDetails(1396L, false) }
        coVerify(exactly = 1) { repository.getSeasonEpisodes(1396L, 1, false) }
    }

    @Test
    fun `cancelTvActiveFetches immediately cancels active TV jobs and resets Loading to Idle`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadTvShowsIfNeeded()
        viewModel.cancelTvActiveFetches()

        // Since it was cancelled before advanceUntilIdle, state should be Idle
        assertThat(viewModel.tvUiState.value).isEqualTo(TvUiState.Idle)
    }

    @Test
    fun `cancelActiveFetches cancels both movie and tv jobs`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadMoviesIfNeeded()
        viewModel.loadTvShowsIfNeeded()
        viewModel.cancelActiveFetches()

        assertThat(viewModel.uiState.value).isEqualTo(MoviesUiState.Idle)
        assertThat(viewModel.tvUiState.value).isEqualTo(TvUiState.Idle)
    }

    @Test
    fun `loadTvDetail preserves selectedSeasonNumber if already set to greater than 1`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectSeason(1396L, 2)
        viewModel.loadTvDetail(1396L, forceRefresh = true)
        testScheduler.advanceUntilIdle()

        val detailState = viewModel.tvDetailState.value
        assertThat(detailState.selectedSeasonNumber).isEqualTo(2)
        coVerify { repository.getSeasonEpisodes(1396L, 2, any()) }
    }
}
