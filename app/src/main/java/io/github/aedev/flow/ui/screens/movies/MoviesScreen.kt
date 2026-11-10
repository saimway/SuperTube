package io.github.aedev.flow.ui.screens.movies

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.data.movie.MovieGenre
import io.github.aedev.flow.data.movie.MoviePlaybackManager
import io.github.aedev.flow.ui.components.layout.LocalFlowBottomInsets
import io.github.aedev.flow.ui.components.layout.topbar.FlowGlobalActionsMode
import io.github.aedev.flow.ui.components.layout.topbar.FlowTopBar
import io.github.aedev.flow.ui.screens.movies.components.MovieCard
import io.github.aedev.flow.ui.screens.movies.components.MovieCardSkeleton
import io.github.aedev.flow.ui.screens.movies.components.MovieHeroSection
import io.github.aedev.flow.ui.screens.movies.components.MovieShelfRow
import io.github.aedev.flow.ui.screens.movies.components.RecentlyWatchedShelfRow
import io.github.aedev.flow.ui.screens.movies.components.TvCard
import io.github.aedev.flow.ui.screens.movies.components.TvHeroSection
import io.github.aedev.flow.ui.screens.movies.components.TvShelfRow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    viewModel: MovieViewModel,
    onMovieClick: (Long) -> Unit,
    onPlayMovie: (Long) -> Unit,
    onTvClick: (Long) -> Unit = {},
    onPlayTv: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val recentMovies by MoviePlaybackManager.recentMoviesFlow.collectAsStateWithLifecycle()
    val recentTv by MoviePlaybackManager.recentTvFlow.collectAsStateWithLifecycle()

    // Strict lazy loading: fetch ONLY when this screen enters composition,
    // and immediately abort all active network calls when user leaves the Movies tab.
    DisposableEffect(viewModel, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            viewModel.loadMoviesIfNeeded()
        } else {
            viewModel.loadTvShowsIfNeeded()
        }
        onDispose {
            viewModel.cancelActiveFetches()
        }
    }

    val movieUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tvUiState by viewModel.tvUiState.collectAsStateWithLifecycle()
    val bottomInsets = LocalFlowBottomInsets.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.nav_movies),
                globalActions = FlowGlobalActionsMode.Auto,
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.nav_search),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Segmented Tab Control: Movies vs TV Shows
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                SegmentedButton(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.movie_tab_movies)) },
                )
                SegmentedButton(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.movie_tab_tv_shows)) },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    // Movies Content
                    when (val state = movieUiState) {
                        is MoviesUiState.Idle, is MoviesUiState.Loading -> {
                            MoviesLoadingView()
                        }

                        is MoviesUiState.Error -> {
                            MoviesErrorView(
                                message = state.message,
                                onRetry = { viewModel.loadMoviesIfNeeded(forceRefresh = true) },
                            )
                        }

                        is MoviesUiState.Success -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                GenreChipsBar(
                                    genres = state.genres,
                                    selectedGenre = state.selectedGenre,
                                    onGenreSelected = viewModel::selectGenre,
                                )

                                if (state.selectedGenre != null && state.selectedGenre.id != 0) {
                                    if (state.isGenreLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 120.dp),
                                            contentPadding = PaddingValues(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 8.dp,
                                                bottom = bottomInsets.contentBottom + 16.dp,
                                            ),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            items(state.genreMovies, key = { it.id }) { movie ->
                                                MovieCard(
                                                    movie = movie,
                                                    onClick = { onMovieClick(movie.id) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    cardWidth = 120.dp,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val listState = rememberLazyListState()
                                    val currentViewModel by rememberUpdatedState(viewModel)

                                    LaunchedEffect(listState) {
                                        snapshotFlow {
                                            val layoutInfo = listState.layoutInfo
                                            val total = layoutInfo.totalItemsCount
                                            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                            total > 0 && last >= total - 2
                                        }
                                            .distinctUntilChanged()
                                            .filter { it }
                                            .collect {
                                                currentViewModel.loadNextShelfBatch()
                                            }
                                    }

                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(
                                            bottom = bottomInsets.contentBottom + 16.dp,
                                        ),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        if (state.heroMovie != null) {
                                            item(key = "movie_hero_section", contentType = "hero") {
                                                MovieHeroSection(
                                                    movie = state.heroMovie,
                                                    onPlayClick = { onPlayMovie(state.heroMovie.id) },
                                                    onDetailsClick = { onMovieClick(state.heroMovie.id) },
                                                )
                                            }
                                        }

                                        if (recentMovies.isNotEmpty()) {
                                            item(key = "recently_watched_movies_shelf", contentType = "shelf_row") {
                                                RecentlyWatchedShelfRow(
                                                    title = stringResource(R.string.movie_shelf_recently_watched),
                                                    items = recentMovies,
                                                    onItemClick = { item -> onPlayMovie(item.mediaId) },
                                                    onDeleteItem = { item -> MoviePlaybackManager.clearProgress(item.mediaId) },
                                                    onClearAll = { MoviePlaybackManager.clearMovieHistory() },
                                                )
                                            }
                                        }

                                        items(
                                            items = state.shelves,
                                            key = { shelf -> shelf.category.id },
                                            contentType = { "shelf_row" },
                                        ) { shelf ->
                                            MovieShelfRow(
                                                shelf = shelf,
                                                onMovieClick = onMovieClick,
                                                onLoadMore = { viewModel.loadMoreForShelf(shelf.category) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TV Shows Content
                    when (val state = tvUiState) {
                        is TvUiState.Idle, is TvUiState.Loading -> {
                            MoviesLoadingView()
                        }

                        is TvUiState.Error -> {
                            MoviesErrorView(
                                message = state.message,
                                onRetry = { viewModel.loadTvShowsIfNeeded(forceRefresh = true) },
                            )
                        }

                        is TvUiState.Success -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                GenreChipsBar(
                                    genres = state.genres,
                                    selectedGenre = state.selectedGenre,
                                    onGenreSelected = viewModel::selectTvGenre,
                                )

                                if (state.selectedGenre != null && state.selectedGenre.id != 0) {
                                    if (state.isGenreLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 120.dp),
                                            contentPadding = PaddingValues(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 8.dp,
                                                bottom = bottomInsets.contentBottom + 16.dp,
                                            ),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            items(state.genreShows, key = { it.id }) { show ->
                                                TvCard(
                                                    show = show,
                                                    onClick = { onTvClick(show.id) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    cardWidth = 120.dp,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val listState = rememberLazyListState()
                                    val currentViewModel by rememberUpdatedState(viewModel)

                                    LaunchedEffect(listState) {
                                        snapshotFlow {
                                            val layoutInfo = listState.layoutInfo
                                            val total = layoutInfo.totalItemsCount
                                            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                            total > 0 && last >= total - 2
                                        }
                                            .distinctUntilChanged()
                                            .filter { it }
                                            .collect {
                                                currentViewModel.loadNextTvShelfBatch()
                                            }
                                    }

                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(
                                            bottom = bottomInsets.contentBottom + 16.dp,
                                        ),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        if (state.heroShow != null) {
                                            item(key = "tv_hero_section", contentType = "hero") {
                                                TvHeroSection(
                                                    show = state.heroShow,
                                                    onWatchClick = { onTvClick(state.heroShow.id) },
                                                    onDetailsClick = { onTvClick(state.heroShow.id) },
                                                )
                                            }
                                        }

                                        if (recentTv.isNotEmpty()) {
                                            item(key = "recently_watched_tv_shelf", contentType = "tv_shelf_row") {
                                                RecentlyWatchedShelfRow(
                                                    title = stringResource(R.string.tv_shelf_recently_watched),
                                                    items = recentTv,
                                                    onItemClick = { item -> onPlayTv(item.mediaId, item.seasonNumber, item.episodeNumber) },
                                                    onDeleteItem = { item -> MoviePlaybackManager.clearTvProgress(item.mediaId, item.seasonNumber, item.episodeNumber) },
                                                    onClearAll = { MoviePlaybackManager.clearTvHistory() },
                                                )
                                            }
                                        }

                                        items(
                                            items = state.shelves,
                                            key = { shelf -> shelf.category.id },
                                            contentType = { "tv_shelf_row" },
                                        ) { shelf ->
                                            TvShelfRow(
                                                shelf = shelf,
                                                onTvClick = onTvClick,
                                                onLoadMore = { viewModel.loadMoreForTvShelf(shelf.category) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChipsBar(
    genres: List<MovieGenre>,
    selectedGenre: MovieGenre?,
    onGenreSelected: (MovieGenre?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.forEach { genre ->
            val isSelected = (genre.id == 0 && selectedGenre == null) || (selectedGenre?.id == genre.id)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (genre.id == 0) {
                        onGenreSelected(null)
                    } else {
                        onGenreSelected(if (isSelected) null else genre)
                    }
                },
                label = {
                    Text(
                        text = if (genre.id == 0) stringResource(R.string.movie_genre_all) else genre.name,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun MoviesLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp),
        ) {
            MovieCardSkeleton(
                modifier = Modifier.fillMaxSize(),
                cardWidth = 400.dp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(3) {
                MovieCardSkeleton()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(3) {
                MovieCardSkeleton()
            }
        }
    }
}

@Composable
private fun MoviesErrorView(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(stringResource(R.string.movie_retry))
        }
    }
}
