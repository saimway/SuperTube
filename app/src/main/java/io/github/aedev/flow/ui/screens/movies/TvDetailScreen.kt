package io.github.aedev.flow.ui.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.aedev.flow.R
import io.github.aedev.flow.data.movie.CastMember
import io.github.aedev.flow.data.movie.MoviePlaybackManager
import io.github.aedev.flow.data.movie.MovieServer
import io.github.aedev.flow.data.movie.TvEpisodeItem
import io.github.aedev.flow.data.movie.TvSeasonSummary
import io.github.aedev.flow.data.movie.TvShowDetail
import io.github.aedev.flow.ui.components.layout.LocalFlowBottomInsets
import io.github.aedev.flow.ui.screens.movies.components.TvCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvDetailScreen(
    tvId: Long,
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (tvId: Long, season: Int, episode: Int, serverId: String) -> Unit,
    onTvClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(tvId) {
        viewModel.loadTvDetail(tvId)
        onDispose {
            viewModel.cancelTvDetailFetch()
        }
    }

    val tvDetailState by viewModel.tvDetailState.collectAsStateWithLifecycle()
    val bottomInsets = LocalFlowBottomInsets.current
    var showServerSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tvDetailState.detail?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                tvDetailState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                tvDetailState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = tvDetailState.error ?: stringResource(R.string.movie_player_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTvDetail(tvId, forceRefresh = true) }) {
                            Text(stringResource(R.string.movie_retry))
                        }
                    }
                }

                tvDetailState.detail != null -> {
                    val detail = tvDetailState.detail!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = bottomInsets.contentBottom + 32.dp,
                        ),
                    ) {
                        // Hero Backdrop
                        item(key = "hero_backdrop") {
                            TvDetailHero(
                                detail = detail,
                                selectedServer = tvDetailState.selectedServer,
                                onWatchClick = { season, episode ->
                                    onPlayEpisode(
                                        tvId,
                                        season,
                                        episode,
                                        tvDetailState.selectedServer.id,
                                    )
                                },
                                onSwitchServerClick = { showServerSheet = true },
                            )
                        }

                        // Overview Synopsis
                        if (detail.overview.isNotBlank()) {
                            item(key = "overview") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.tv_details_synopsis),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = detail.overview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Season selector chips
                        if (detail.seasons.isNotEmpty()) {
                            item(key = "seasons_selector") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.tv_details_seasons),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SeasonChipsRow(
                                        seasons = detail.seasons,
                                        selectedSeasonNumber = tvDetailState.selectedSeasonNumber,
                                        onSeasonSelected = { seasonNumber ->
                                            viewModel.selectSeason(tvId, seasonNumber)
                                        },
                                    )
                                }
                            }
                        }

                        // Episodes list
                        item(key = "episodes_header") {
                            Text(
                                text = stringResource(R.string.tv_details_episodes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        if (tvDetailState.isEpisodesLoading) {
                            item(key = "episodes_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        } else if (tvDetailState.episodes.isEmpty()) {
                            item(key = "episodes_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.tv_no_episodes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(
                                items = tvDetailState.episodes,
                                key = { ep -> "ep_${ep.seasonNumber}_${ep.episodeNumber}_${ep.id}" },
                            ) { episode ->
                                val progressRecord = remember(tvId, episode.seasonNumber, episode.episodeNumber) {
                                    MoviePlaybackManager.getTvProgress(tvId, episode.seasonNumber, episode.episodeNumber)
                                }
                                EpisodeListItem(
                                    episode = episode,
                                    progressRecord = progressRecord,
                                    onClick = {
                                        onPlayEpisode(
                                            tvId,
                                            episode.seasonNumber,
                                            episode.episodeNumber,
                                            tvDetailState.selectedServer.id,
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }

                        // Cast Members
                        if (detail.cast.isNotEmpty()) {
                            item(key = "cast_row") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.tv_details_cast),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(detail.cast, key = { it.id }) { castMember ->
                                            CastMemberCard(castMember = castMember)
                                        }
                                    }
                                }
                            }
                        }

                        // Recommendations
                        if (detail.recommendations.isNotEmpty()) {
                            item(key = "recommendations_row") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.tv_details_recommendations),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(detail.recommendations, key = { it.id }) { show ->
                                            TvCard(
                                                show = show,
                                                onClick = { onTvClick(show.id) },
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

        // Server selection bottom sheet
        if (showServerSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showServerSheet = false },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tv_details_servers),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    MovieServer.SERVERS.forEach { server ->
                        val isSelected = server.id == tvDetailState.selectedServer.id
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectTvServer(server)
                                    showServerSheet = false
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = server.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = server.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = stringResource(R.string.tv_player_active),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TvDetailHero(
    detail: TvShowDetail,
    selectedServer: MovieServer,
    onWatchClick: (season: Int, episode: Int) -> Unit,
    onSwitchServerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val scrimBrush = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                surfaceColor.copy(alpha = 0.5f),
                surfaceColor.copy(alpha = 0.9f),
                surfaceColor,
            ),
            startY = 0f,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f),
    ) {
        val backdropUrl = detail.backdropUrl ?: detail.posterUrl
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = detail.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Metadata Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (detail.voteAverage > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = detail.ratingFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (detail.releaseYear.isNotBlank()) {
                    Text(
                        text = detail.releaseYear,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (detail.numberOfSeasons > 0) {
                    Text(
                        text = "${detail.numberOfSeasons} ${if (detail.numberOfSeasons == 1) "Season" else "Seasons"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Genres
            if (detail.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.genres.joinToString(" • ") { it.name },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Determine target episode to play: resume if last watched exists, or season 1 episode 1
                val lastWatched = MoviePlaybackManager.getLastWatched()
                val isThisShow = lastWatched?.mediaId == detail.id && lastWatched.mediaType == io.github.aedev.flow.data.movie.MediaType.TV
                val targetSeason = if (isThisShow) lastWatched.seasonNumber else 1
                val targetEpisode = if (isThisShow) lastWatched.episodeNumber else 1

                val buttonLabel = if (isThisShow) {
                    stringResource(R.string.tv_details_resume, targetSeason, targetEpisode)
                } else {
                    stringResource(R.string.tv_details_watch, targetSeason, targetEpisode)
                }

                Button(
                    onClick = { onWatchClick(targetSeason, targetEpisode) },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedButton(
                    onClick = onSwitchServerClick,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedServer.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonChipsRow(
    seasons: List<TvSeasonSummary>,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    // Put specials (season 0) at the end
    val sortedSeasons = remember(seasons) {
        seasons.sortedWith(compareBy({ if (it.seasonNumber == 0) 1 else 0 }, { it.seasonNumber }))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sortedSeasons.forEach { season ->
            val isSelected = season.seasonNumber == selectedSeasonNumber
            val label = if (season.seasonNumber == 0) {
                stringResource(R.string.tv_season_specials)
            } else {
                stringResource(R.string.tv_season_number, season.seasonNumber)
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSeasonSelected(season.seasonNumber) },
                label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
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
private fun EpisodeListItem(
    episode: TvEpisodeItem,
    progressRecord: io.github.aedev.flow.data.movie.MoviePlaybackRecord?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Still Thumbnail with Progress
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (episode.stillUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(episode.stillUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // In-progress indicator
                if (progressRecord != null && progressRecord.positionSeconds > 10) {
                    val frac = if (progressRecord.durationSeconds > 0) {
                        (progressRecord.positionSeconds.toFloat() / progressRecord.durationSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }

            // Episode Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${episode.episodeCode} • ${episode.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (episode.overview.isNotBlank()) {
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (episode.airDate.isNotBlank()) {
                    Text(
                        text = episode.airDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CastMemberCard(
    castMember: CastMember,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (castMember.profileUrl != null) {
                AsyncImage(
                    model = castMember.profileUrl,
                    contentDescription = castMember.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = castMember.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
