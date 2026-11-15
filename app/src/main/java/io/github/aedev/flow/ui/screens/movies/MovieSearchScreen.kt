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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.data.movie.MediaSearchResult
import io.github.aedev.flow.data.movie.SearchFilterType
import io.github.aedev.flow.ui.components.layout.LocalFlowBottomInsets
import io.github.aedev.flow.ui.screens.movies.components.MovieCard
import io.github.aedev.flow.ui.screens.movies.components.TvCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun MovieSearchScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    onMovieClick: (Long) -> Unit,
    onTvClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bottomInsets = LocalFlowBottomInsets.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    val displayedItems = remember(searchState.unifiedResults, searchState.selectedFilter) {
        when (searchState.selectedFilter) {
            SearchFilterType.ALL -> searchState.unifiedResults
            SearchFilterType.MOVIES -> searchState.unifiedResults.filterIsInstance<MediaSearchResult.Movie>()
            SearchFilterType.TV_SHOWS -> searchState.unifiedResults.filterIsInstance<MediaSearchResult.TvShow>()
        }
    }

    // Smooth pagination on scroll via snapshotFlow
    LaunchedEffect(gridState, searchState.hasMore, searchState.isSearching) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 4
        }
            .distinctUntilChanged()
            .filter { it && searchState.hasMore && !searchState.isSearching }
            .collect {
                viewModel.loadMoreSearchResults()
            }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.cancelSearch()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }

                        TextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = {
                                Text(stringResource(R.string.movie_search_placeholder))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = searchState.selectedFilter == SearchFilterType.ALL,
                            onClick = { viewModel.setSearchFilter(SearchFilterType.ALL) },
                            label = { Text(stringResource(R.string.search_filter_all)) },
                        )
                        FilterChip(
                            selected = searchState.selectedFilter == SearchFilterType.MOVIES,
                            onClick = { viewModel.setSearchFilter(SearchFilterType.MOVIES) },
                            label = { Text(stringResource(R.string.movie_tab_movies)) },
                        )
                        FilterChip(
                            selected = searchState.selectedFilter == SearchFilterType.TV_SHOWS,
                            onClick = { viewModel.setSearchFilter(SearchFilterType.TV_SHOWS) },
                            label = { Text(stringResource(R.string.movie_tab_tv_shows)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                searchState.isSearching && searchState.unifiedResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                searchQuery.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.movie_search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                displayedItems.isEmpty() && !searchState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.movie_search_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = bottomInsets.contentBottom + 20.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = displayedItems,
                            key = { item ->
                                when (item) {
                                    is MediaSearchResult.Movie -> "movie_${item.movie.id}"
                                    is MediaSearchResult.TvShow -> "tv_${item.show.id}"
                                }
                            },
                        ) { item ->
                            when (item) {
                                is MediaSearchResult.Movie -> {
                                    MovieCard(
                                        movie = item.movie,
                                        onClick = { onMovieClick(item.movie.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        cardWidth = 120.dp,
                                    )
                                }
                                is MediaSearchResult.TvShow -> {
                                    TvCard(
                                        show = item.show,
                                        onClick = { onTvClick(item.show.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        cardWidth = 120.dp,
                                    )
                                }
                            }
                        }

                        if (searchState.isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
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
