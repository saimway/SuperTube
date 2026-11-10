package io.github.aedev.flow.ui.screens.movies

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.aedev.flow.R
import io.github.aedev.flow.data.movie.MoviePlaybackManager
import io.github.aedev.flow.data.movie.MovieServer
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream

private val ALLOWED_STREAMING_DOMAINS = setOf(
    "zxcstream.cc",
    "vidlink.pro",
    "player.videasy.net",
    "videasy.net",
    "vidfast.pro",
    "vidrock.to",
    "moviesapi.to",
    "player.vidzee.wtf",
    "vidzee.wtf",
    "vidnest.fun",
    "vidup.to",
    "vsembed.ru",
    "peachify.net",
    "vidcore.net",
    "embed.smashystream.com",
    "smashystream.com",
    "111movies.com",
    "vidsrcme.ru",
    "2embed.cc",
    "multiembed.mov",
    "primesrc.me",
    "vidlux.xyz",
    "rabbitmeow.online",
    "rabbitmeow.fun",
    "tmdb.org",
    "vidsrc.to",
    "vidsrc.me",
    "vidsrc.pm",
    "vidsrc.net",
    "autoembed.cc",
    "superembed.stream",
    "rabbitstream.net",
    "megacloud.tv",
    "cloudstream.to",
)

private fun isAllowedStreamingHost(host: String): Boolean {
    if (host.isBlank()) return false
    return ALLOWED_STREAMING_DOMAINS.any { allowed ->
        host.equals(allowed, ignoreCase = true) || host.endsWith(".$allowed", ignoreCase = true)
    }
}

private val BLOCKED_AD_HOSTS = setOf(
    "aarems.org",
    "doubleclick.net",
    "adservice.google.com",
    "googlesyndication.com",
    "adnxs.com",
    "popads.net",
    "popcash.net",
    "propellerads.com",
    "exoclick.com",
    "trafficjunky.com",
    "adcash.com",
    "hilltopads.net",
    "clickadu.com",
    "yadro.ru",
    "adriver.ru",
    "bet365.com",
    "1xbet.com",
    "parimatch.com",
    "mostbet.com",
    "vavada.com",
    "pin-up.team",
    "joycasino.com",
    "spinbetter.com",
    "melbet.com",
    "linebet.com",
    "megapari.com",
    "1win.pro",
    "betwinner.com",
    "monetag.com",
    "rollerads.com",
    "richpush.co",
    "adsterra.com",
    "clickaine.com",
    "evadav.com",
    "yllix.com",
    "bidvertiser.com",
    "infolinks.com",
    "buysellads.com",
    "adblade.com",
    "revcontent.com",
    "taboola.com",
    "outbrain.com",
    "mgid.com",
    "zergnet.com",
    "adkernel.com",
    "adbutler.com",
    "admaven.com",
    "galaksion.com",
    "daisycon.com",
    "tradedoubler.com",
    "awin1.com",
    "cj.com",
    "rakuten.com",
    "impact.com",
    "shareasale.com",
    "flexoffers.com",
)

private val BLOCKED_URL_PATTERNS = listOf(
    "/banner",
    "/popunder",
    "/popup",
    "/vast",
    "/vpaid",
    "/advert",
    "/ad-",
    "/ads-",
    "/ad.",
    "/ads.",
    "/tracking",
    "/pixel",
    "/analytics",
    "/affiliate",
    "/partner",
    "/promo",
    "/telemetry",
    "/beacon",
    "/count.",
    "/stat.",
    "/metrics",
    "gamble",
    "casino",
    "betting",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvPlayerScreen(
    tvId: Long,
    initialSeason: Int,
    initialEpisode: Int,
    initialServerId: String?,
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var currentSeason by remember(tvId, initialSeason) { mutableIntStateOf(initialSeason) }
    var currentEpisode by remember(tvId, initialEpisode) { mutableIntStateOf(initialEpisode) }

    val tvDetailState by viewModel.tvDetailState.collectAsStateWithLifecycle()
    val showDetail = tvDetailState.detail

    DisposableEffect(tvId) {
        viewModel.loadTvDetail(tvId)
        onDispose {
            viewModel.cancelTvDetailFetch()
        }
    }

    LaunchedEffect(tvDetailState.detail, currentSeason) {
        if (tvDetailState.detail != null && tvDetailState.selectedSeasonNumber != currentSeason) {
            viewModel.selectSeason(tvId, currentSeason)
        }
    }

    val savedRecord = remember(tvId, currentSeason, currentEpisode) {
        MoviePlaybackManager.getTvProgress(tvId, currentSeason, currentEpisode)
    }
    val savedPosition = savedRecord?.positionSeconds ?: 0L

    var currentServer by remember {
        mutableStateOf(
            if (initialServerId != null) {
                MovieServer.findById(initialServerId)
            } else {
                MoviePlaybackManager.getSavedTvServer(tvId, currentSeason, currentEpisode)
            },
        )
    }

    val attemptedServers = remember { mutableStateListOf(currentServer.id) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isStreamPlaying by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showResumeBadge by remember { mutableStateOf(savedPosition > 10) }
    var currentPositionSec by remember { mutableStateOf(0.0) }
    var currentDurationSec by remember { mutableStateOf(0.0) }
    var autoNextCountdown by remember { mutableIntStateOf(-1) }
    var autoNextDismissedForEpisode by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val autoNextDismissed = autoNextDismissedForEpisode == (currentSeason to currentEpisode)

    fun playEpisode(season: Int, episode: Int) {
        currentSeason = season
        currentEpisode = episode
        isStreamPlaying = false
        isConnecting = true
        hasError = false
        autoNextCountdown = -1
        showResumeBadge = false

        attemptedServers.clear()
        attemptedServers.add(currentServer.id)

        val newRecord = MoviePlaybackManager.getTvProgress(tvId, season, episode)
        val pos = newRecord?.positionSeconds ?: 0L
        showResumeBadge = pos > 10

        val epTitle = tvDetailState.episodes.firstOrNull {
            it.seasonNumber == season && it.episodeNumber == episode
        }?.name ?: ""
        MoviePlaybackManager.saveTvProgress(
            tvId = tvId,
            seasonNumber = season,
            episodeNumber = episode,
            serverId = currentServer.id,
            positionSeconds = pos,
            durationSeconds = newRecord?.durationSeconds ?: 0L,
            title = showDetail?.name ?: "",
            episodeTitle = epTitle,
            posterUrl = showDetail?.posterUrl,
            backdropUrl = showDetail?.backdropUrl,
        )

        val url = currentServer.getTvUrl(tvId, season, episode)
        webViewRef?.post {
            webViewRef?.stopLoading()
            webViewRef?.loadUrl(url)
        }
    }

    fun switchToNextServer(reason: String) {
        val nextServer = MovieServer.SERVERS.firstOrNull { it.id !in attemptedServers }
        if (nextServer == null) {
            hasError = true
            errorMessage = "All ${MovieServer.SERVERS.size} servers attempted. None were able to stream this episode."
            isConnecting = false
            return
        }

        attemptedServers.add(nextServer.id)
        val oldServerName = currentServer.name
        currentServer = nextServer
        hasError = false
        isStreamPlaying = false
        isConnecting = true
        statusMessage = "Server $oldServerName unavailable. Switching to ${nextServer.name} (${attemptedServers.size}/${MovieServer.SERVERS.size})…"

        webViewRef?.post {
            webViewRef?.stopLoading()
            webViewRef?.loadUrl(nextServer.getTvUrl(tvId, currentSeason, currentEpisode))
        }
    }

    // Server timeout failover guard
    LaunchedEffect(currentServer, currentSeason, currentEpisode) {
        delay(14000L)
        if (!isStreamPlaying && !hasError) {
            switchToNextServer("Server timed out")
        }
    }

    // Auto-dismiss resume pill after 5s
    LaunchedEffect(isStreamPlaying) {
        if (isStreamPlaying && showResumeBadge) {
            delay(5000L)
            showResumeBadge = false
        }
    }

    // AutoNext countdown loop when video reaches end; else reset on rewind
    LaunchedEffect(currentPositionSec, currentDurationSec) {
        // Trigger countdown near end; else cancel if rewound
        val hasNextEp = tvDetailState.episodes.any { it.episodeNumber == currentEpisode + 1 }
        if (currentDurationSec > 60 && currentPositionSec > 0 && currentPositionSec >= currentDurationSec - 15) {
            if (autoNextCountdown == -1 && !autoNextDismissed && hasNextEp) {
                autoNextCountdown = 10
            }
        } else {
            if (autoNextCountdown > 0) {
                autoNextCountdown = -1
            }
        }
    }

    LaunchedEffect(autoNextCountdown) {
        if (autoNextCountdown > 0) {
            delay(1000L)
            autoNextCountdown -= 1
        } else if (autoNextCountdown == 0) {
            // Find next episode
            val episodes = tvDetailState.episodes
            val nextEp = episodes.firstOrNull { it.episodeNumber == currentEpisode + 1 }
            if (nextEp != null) {
                playEpisode(currentSeason, nextEp.episodeNumber)
            } else {
                autoNextCountdown = -1
            }
        }
    }

    // Screen-on & fullscreen orientation handling
    DisposableEffect(activity, isFullscreen) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Create Sandboxed WebView
    val webView = remember(context, tvId) {
        createSandboxedWebView(
            context = context,
            savedPositionSeconds = {
                MoviePlaybackManager.getTvProgress(tvId, currentSeason, currentEpisode)?.positionSeconds ?: 0L
            },
            getSavedPositionSeconds = {
                MoviePlaybackManager.getTvProgress(tvId, currentSeason, currentEpisode)?.positionSeconds ?: 0L
            },
            onPlaybackStarted = {
                isStreamPlaying = true
                isConnecting = false
                val epTitle = tvDetailState.episodes.firstOrNull {
                    it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode
                }?.name ?: ""
                MoviePlaybackManager.saveTvProgress(
                    tvId = tvId,
                    seasonNumber = currentSeason,
                    episodeNumber = currentEpisode,
                    serverId = currentServer.id,
                    positionSeconds = 1L,
                    durationSeconds = 0L,
                    title = showDetail?.name ?: "",
                    episodeTitle = epTitle,
                    posterUrl = showDetail?.posterUrl,
                    backdropUrl = showDetail?.backdropUrl,
                )
            },
            onProgressUpdate = { currentSec, durationSec ->
                currentPositionSec = currentSec
                currentDurationSec = durationSec
                if (currentSec > 0) {
                    isStreamPlaying = true
                    isConnecting = false
                }
                val epTitle = tvDetailState.episodes.firstOrNull {
                    it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode
                }?.name ?: ""
                MoviePlaybackManager.saveTvProgress(
                    tvId = tvId,
                    seasonNumber = currentSeason,
                    episodeNumber = currentEpisode,
                    serverId = currentServer.id,
                    positionSeconds = currentSec.toLong(),
                    durationSeconds = durationSec.toLong(),
                    title = showDetail?.name ?: "",
                    episodeTitle = epTitle,
                    posterUrl = showDetail?.posterUrl,
                    backdropUrl = showDetail?.backdropUrl,
                )
            },
            onPageLoaded = {
                if (!isStreamPlaying) {
                    statusMessage = "Connecting to stream…"
                }
            },
            onErrorOccurred = { error ->
                switchToNextServer("Playback error: $error")
            },
        ).also { wv ->
            webViewRef = wv
            wv.loadUrl(currentServer.getTvUrl(tvId, currentSeason, currentEpisode))
        }
    }

    // Strict WebView lifecycle disposal
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
            webViewRef = null
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Direct Rendering WebView
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
        )

        // Tap-to-toggle overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    controlsVisible = !controlsVisible
                },
        )

        // Status / Connecting Overlay
        if (isConnecting && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = statusMessage ?: stringResource(R.string.movie_player_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Text(
                        text = currentServer.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Error Overlay
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = errorMessage ?: stringResource(R.string.movie_player_all_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { switchToNextServer("Manual retry") }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.movie_player_try_next_server))
                        }
                        FilledTonalButton(onClick = { showServerSheet = true }) {
                            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.movie_player_switch_server))
                        }
                    }
                }
            }
        }

        // AutoNext Overlay
        if (autoNextCountdown > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 80.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tv_player_auto_next_countdown, autoNextCountdown),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        onClick = {
                            val nextEp = tvDetailState.episodes.firstOrNull { it.episodeNumber == currentEpisode + 1 }
                            if (nextEp != null) {
                                playEpisode(currentSeason, nextEp.episodeNumber)
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(stringResource(R.string.tv_player_next_episode))
                    }
                    IconButton(onClick = {
                        autoNextDismissedForEpisode = currentSeason to currentEpisode
                        autoNextCountdown = -1
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            }
        }

        // Resume Pill
        if (showResumeBadge && savedPosition > 10) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val m = savedPosition / 60
                    val s = savedPosition % 60
                    Text(
                        text = stringResource(R.string.movie_resumed_from, "%02d:%02d".format(m, s)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.movie_restart),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            showResumeBadge = false
                            webView.evaluateJavascript(
                                "var v = document.querySelector('video'); if (v) v.currentTime = 0;",
                                null,
                            )
                        },
                    )
                }
            }
        }

        // Overlay Controls (Header + Bottom Bar)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDetail?.let { onNavigateToDetail(it.id) } },
                    ) {
                        Text(
                            text = showDetail?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val epCode = "S%02dE%02d".format(currentSeason, currentEpisode)
                        val currentEpTitle = tvDetailState.episodes.firstOrNull { it.episodeNumber == currentEpisode }?.name
                        Text(
                            text = if (!currentEpTitle.isNullOrBlank()) "$epCode • $currentEpTitle" else epCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showEpisodeSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = stringResource(R.string.tv_player_episodes),
                                tint = Color.White,
                            )
                        }

                        IconButton(onClick = { showServerSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = stringResource(R.string.movie_player_switch_server),
                                tint = Color.White,
                            )
                        }

                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.fullscreen),
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Bottom Control Bar (Episode navigation & stepper)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Previous Episode Button
                    val episodes = tvDetailState.episodes
                    val prevEp = episodes.firstOrNull { it.episodeNumber == currentEpisode - 1 }
                    IconButton(
                        onClick = {
                            if (prevEp != null) playEpisode(currentSeason, prevEp.episodeNumber)
                        },
                        enabled = prevEp != null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.tv_player_previous_episode),
                            tint = if (prevEp != null) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }

                    // Episodes Drawer Opener Pill
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { showEpisodeSheet = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.tv_player_episodes_count, tvDetailState.episodes.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }

                    // Next Episode Button
                    val nextEp = episodes.firstOrNull { it.episodeNumber == currentEpisode + 1 }
                    IconButton(
                        onClick = {
                            if (nextEp != null) playEpisode(currentSeason, nextEp.episodeNumber)
                        },
                        enabled = nextEp != null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.tv_player_next_episode),
                            tint = if (nextEp != null) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        // Episodes Drawer Bottom Sheet
        if (showEpisodeSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showEpisodeSheet = false },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tv_player_episodes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(tvDetailState.episodes, key = { it.id }) { ep ->
                            val isCurrent = ep.episodeNumber == currentEpisode
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showEpisodeSheet = false
                                        playEpisode(currentSeason, ep.episodeNumber)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    ) {
                                        if (ep.stillUrl != null) {
                                            AsyncImage(
                                                model = ep.stillUrl,
                                                contentDescription = ep.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${ep.episodeCode} • ${ep.name}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (ep.overview.isNotBlank()) {
                                            Text(
                                                text = ep.overview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
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

        // Server Switcher Bottom Sheet
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
                        text = stringResource(R.string.movie_player_switch_server),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    MovieServer.SERVERS.forEach { server ->
                        val isSelected = server.id == currentServer.id
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    currentServer = server
                                    showServerSheet = false
                                    isStreamPlaying = false
                                    isConnecting = true
                                    hasError = false
                                    statusMessage = "Loading ${server.name}…"
                                    webView.stopLoading()
                                    webView.loadUrl(server.getTvUrl(tvId, currentSeason, currentEpisode))
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

@SuppressLint("SetJavaScriptEnabled")
private fun createSandboxedWebView(
    context: Context,
    getSavedPositionSeconds: () -> Long = { 0L },
    savedPositionSeconds: (() -> Long)? = null,
    onPlaybackStarted: () -> Unit,
    onProgressUpdate: (currentSeconds: Double, durationSeconds: Double) -> Unit,
    onPageLoaded: () -> Unit,
    onErrorOccurred: (String) -> Unit,
): WebView {
    val positionProvider = savedPositionSeconds ?: getSavedPositionSeconds
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        setBackgroundColor(android.graphics.Color.BLACK)
        setLayerType(View.LAYER_TYPE_NONE, null)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Sandbox security: disable window popups and malicious intent injection
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // Bridge to catch iframe video play and time updates
        addJavascriptInterface(
            object {
                @JavascriptInterface
                fun onStreamPlaying() {
                    post {
                        onPlaybackStarted()
                    }
                }

                @JavascriptInterface
                fun onProgress(currentSec: Double, durationSec: Double) {
                    post {
                        onProgressUpdate(currentSec, durationSec)
                    }
                }
            },
            "SuperTubeBridge",
        )

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val disarmJs = """
                    (function() {
                        try {
                            window.open = function() { return null; };
                            window.alert = function() {};
                            window.confirm = function() { return false; };
                            window.prompt = function() { return null; };
                        } catch(e) {}
                    })();
                """.trimIndent()
                view?.evaluateJavascript(disarmJs, null)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val urlString = url.toString()
                val scheme = url.scheme?.lowercase()

                if (scheme != "http" && scheme != "https") {
                    return true
                }

                val host = url.host?.lowercase().orEmpty()

                if (!isAllowedStreamingHost(host) && urlString != "about:blank") {
                    return true
                }

                val isBlockedHost = BLOCKED_AD_HOSTS.any { host.contains(it) }
                val isBlockedPattern = BLOCKED_URL_PATTERNS.any { urlString.contains(it, ignoreCase = true) }
                if (isBlockedHost || isBlockedPattern) {
                    return true
                }

                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                val urlString = url.toString().lowercase()
                val host = url.host?.lowercase().orEmpty()

                val isBlockedHost = BLOCKED_AD_HOSTS.any { host.contains(it) }
                val isBlockedPattern = BLOCKED_URL_PATTERNS.any { urlString.contains(it) }

                if (isBlockedHost || isBlockedPattern) {
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPageLoaded()

                val currentSaved = positionProvider()
                val seekInit = if (currentSaved > 10) "var savedTime = $currentSaved;" else "var savedTime = 0;"
                val js = """
                    (function() {
                        $seekInit
                        var hasResumed = false;

                        try {
                            var css = 'iframe[id*="ad"],iframe[class*="ad"],iframe[src*="ad"],iframe[src*="banner"],iframe[src*="pop"],div[id*="ad-"],div[class*="ad-"],div[id*="banner"],div[class*="banner"],div[id*="pop"],div[class*="pop"],div[class*="sponsor"],div[id*="sponsor"],[id*="google_ads"],[id*="aswift"],div[style*="z-index: 2147483647"],div[style*="z-index:999999"],div[style*="z-index: 999999"],div[style*="z-index:100000"],div[style*="z-index: 100000"],div[style*="z-index:99999"],div[style*="z-index: 99999"]{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important;}';
                            var styleTag = document.createElement('style');
                            styleTag.textContent = css;
                            (document.head || document.documentElement).appendChild(styleTag);
                        } catch(e) {}

                        try {
                            window.open = function() { return null; };
                            window.alert = function() {};
                            window.confirm = function() { return false; };
                            window.prompt = function() { return null; };
                        } catch(e) {}

                        window.addEventListener('click', function(e) {
                            var t = e.target;
                            while (t && t !== document.body) {
                                if (t.tagName === 'A') {
                                    if (t.target === '_blank' || (t.href && !t.href.startsWith(window.location.origin))) {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        e.stopImmediatePropagation();
                                        return false;
                                    }
                                }
                                t = t.parentElement;
                            }
                        }, true);

                        var removeAdElements = function() {
                            var selectors = [
                                'div[id*="ad-"]', 'div[class*="ad-"]',
                                'div[id*="banner"]', 'div[class*="banner"]',
                                'div[id*="pop"]', 'div[class*="pop"]',
                                'iframe[src*="ad"]', 'iframe[src*="banner"]',
                                'iframe[src*="pop"]',
                                'div[style*="z-index: 2147483647"]',
                                'div[style*="z-index: 999999"]', 'div[style*="z-index:999999"]',
                                'div[style*="z-index: 100000"]', 'div[style*="z-index:100000"]',
                                'div[style*="z-index: 99999"]', 'div[style*="z-index:99999"]'
                            ];
                            selectors.forEach(function(s) {
                                try {
                                    var els = document.querySelectorAll(s);
                                    for (var i = 0; i < els.length; i++) {
                                        var el = els[i];
                                        if (el.tagName !== 'VIDEO' && !el.querySelector('video') && !el.classList.contains('jwplayer') && !el.classList.contains('plyr') && !el.classList.contains('vjs-tech')) {
                                            el.remove();
                                        }
                                    }
                                } catch(e) {}
                            });
                        };
                        removeAdElements();
                        try {
                            var obs = new MutationObserver(removeAdElements);
                            obs.observe(document.documentElement, { childList: true, subtree: true });
                        } catch(e) {}
                        setInterval(removeAdElements, 600);

                        window.addEventListener('message', function(e) {
                            try {
                                var data = typeof e.data === 'string' ? JSON.parse(e.data) : e.data;
                                if (data) {
                                    if (data.event === 'play' || data.event === 'playing' || data.event === 'timeupdate' || data.type === 'PLAYER_PLAYING') {
                                        window.SuperTubeBridge && window.SuperTubeBridge.onStreamPlaying();
                                    }
                                    var cur = data.currentTime || data.time || data.progress;
                                    var dur = data.duration || 0;
                                    if (cur && cur > 0) {
                                        window.SuperTubeBridge && window.SuperTubeBridge.onProgress(cur, dur);
                                    }
                                }
                            } catch(err) {}
                        });
                        var checkVideo = function() {
                            var v = document.querySelector('video');
                            if (v) {
                                v.addEventListener('playing', function() {
                                    window.SuperTubeBridge && window.SuperTubeBridge.onStreamPlaying();
                                });
                                v.addEventListener('timeupdate', function() {
                                    if (v.currentTime > 0) {
                                        window.SuperTubeBridge && window.SuperTubeBridge.onProgress(v.currentTime, v.duration || 0);
                                    }
                                });
                                if (!v.paused && v.currentTime > 0) {
                                    window.SuperTubeBridge && window.SuperTubeBridge.onStreamPlaying();
                                }
                                if (savedTime > 10 && !hasResumed && (v.duration > 0 || v.readyState >= 1)) {
                                    hasResumed = true;
                                    try {
                                        v.currentTime = savedTime;
                                    } catch(err) {}
                                }
                            }
                        };
                        setInterval(checkVideo, 1000);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString().orEmpty()
                    val code = error?.errorCode ?: 0
                    val isAbort = code == -1 ||
                        desc.contains("ERR_ABORTED", ignoreCase = true) ||
                        desc.contains("ERR_CACHE_MISS", ignoreCase = true)
                    if (!isAbort && code != WebViewClient.ERROR_UNKNOWN) {
                        onErrorOccurred(desc.ifBlank { "Main frame error ($code)" })
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val status = errorResponse?.statusCode ?: 200
                    if (status == 404 || status == 502 || status == 503) {
                        onErrorOccurred("HTTP error $status")
                    }
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                return false
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                result?.cancel()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                result?.cancel()
                return true
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                result?.cancel()
                return true
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
        }
    }
}
