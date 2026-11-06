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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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
    "vidcore.net",
    "embed.smashystream.com",
    "smashystream.com",
    "tmdb.org",
    "vidsrc.to",
    "vidsrc.me",
    "vidsrc.pm",
    "vidsrc.net",
    "2embed.cc",
    "autoembed.cc",
    "superembed.stream",
    "multiembed.mov",
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
    "popads.net",
    "popcash.net",
    "propellerads.com",
    "propellerclick.com",
    "adsterra.com",
    "exoclick.com",
    "juicyads.com",
    "onclickpredictiv.com",
    "onclickmega.com",
    "histats.com",
    "bet365.com",
    "1xbet.com",
    "trafmag.com",
    "topcreativeformat.com",
    "highperformancegate.com",
    "coinhive.com",
    "adkeeper.co",
    "adtrue.com",
    "yadro.ru",
    "marketgid.com",
    "mgid.com",
    "hilltopads.com",
    "clickadu.com",
    "richpush.co",
    "evadav.com",
    "pushassist.com",
    "monetag.com",
    "ad-maven.com",
    "zeroredirect.com",
    "trafficfactory.biz",
    "trafficjunky.com",
    "trafficstars.com",
    "tsyndicate.com",
    "adtng.com",
    "adxadserv.com",
    "bidgear.com",
    "adcash.com",
    "rollerads.com",
    "exdynsrv.com",
    "etahub.com",
    "clksite.com",
    "m2pub.com",
    "rtbmark.com",
    "adnxs.com",
    "criteo.com",
    "taboola.com",
    "outbrain.com",
    "serving-sys.com",
    "yllix.com",
    "bidvertiser.com",
    "smartadserver.com",
    "revcontent.com",
    "adblade.com",
    "adbutler.com",
    "pubmatic.com",
    "rubiconproject.com",
)

private val BLOCKED_URL_PATTERNS = listOf(
    "/ads/",
    "/ad-banner",
    "/popunder",
    "/popcash",
    "googleads",
    "doubleclick",
    "pagead",
    "serving-sys",
    "adnxs",
    "adroll",
    "beacon.js",
    "adservice",
    "adserver",
    "popup.js",
    "popunder.js",
    "onclick",
    "/vast",
    "/vpaid",
    "prebid",
    "openx",
    "adsystem",
    "tracker.js",
    "syndication",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviePlayerScreen(
    movieId: Long,
    initialServerId: String?,
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val movieDetail = detailState.detail

    DisposableEffect(movieId) {
        viewModel.loadMovieDetail(movieId)
        onDispose {
            viewModel.cancelDetailFetch()
        }
    }

    // Load saved playback progress and resume state
    val savedRecord = remember(movieId) { MoviePlaybackManager.getProgress(movieId) }
    val savedPosition = savedRecord?.positionSeconds ?: 0L

    var currentServer by remember {
        mutableStateOf(
            if (initialServerId != null) {
                MovieServer.findById(initialServerId)
            } else {
                MoviePlaybackManager.getSavedServer(movieId)
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
    var isFullscreen by remember { mutableStateOf(false) }
    var showResumeBadge by remember { mutableStateOf(savedPosition > 10) }

    fun switchToNextServer(reason: String) {
        val nextServer = MovieServer.SERVERS.firstOrNull { it.id !in attemptedServers }
        if (nextServer == null) {
            hasError = true
            errorMessage = "All ${MovieServer.SERVERS.size} servers attempted. None were able to stream this title."
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
            webViewRef?.apply {
                stopLoading()
                loadUrl(nextServer.getUrl(movieId))
            }
        }
    }

    // 14-second failover guard: if server hangs without streaming, failover to next server
    LaunchedEffect(currentServer) {
        delay(14000L)
        if (!isStreamPlaying && !hasError) {
            switchToNextServer("Server timed out")
        }
    }

    // Auto-dismiss resume pill after 5s of playback
    LaunchedEffect(isStreamPlaying) {
        if (isStreamPlaying && showResumeBadge) {
            delay(5000L)
            showResumeBadge = false
        }
    }

    // Screen-on & window orientation management
    DisposableEffect(activity, isFullscreen) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

    // Create and remember the Sandboxed WebView once for this screen session
    val webView = remember(context, movieId) {
        createSandboxedWebView(
            context = context,
            savedPositionSeconds = savedPosition,
            onPlaybackStarted = {
                isStreamPlaying = true
                isConnecting = false
                MoviePlaybackManager.saveProgress(
                    movieId = movieId,
                    serverId = currentServer.id,
                    positionSeconds = savedPosition,
                    durationSeconds = 0L,
                )
            },
            onProgressUpdate = { currentSec, durationSec ->
                if (currentSec > 0) {
                    MoviePlaybackManager.saveProgress(
                        movieId = movieId,
                        serverId = currentServer.id,
                        positionSeconds = currentSec.toLong(),
                        durationSeconds = durationSec.toLong(),
                    )
                }
            },
            onPageLoaded = {
                scope.launch {
                    delay(2500L)
                    if (!hasError) {
                        isStreamPlaying = true
                        isConnecting = false
                    }
                }
            },
            onErrorOccurred = { error ->
                switchToNextServer(error)
            },
        ).also { wv ->
            webViewRef = wv
            wv.loadUrl(currentServer.getUrl(movieId))
        }
    }

    // Teardown WebView safely when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }

    // Controls auto-hide timer in fullscreen mode
    LaunchedEffect(controlsVisible, isFullscreen) {
        if (isFullscreen && controlsVisible) {
            delay(4000L)
            controlsVisible = false
        }
    }

    // Handle back button: exit fullscreen first if active, otherwise normal back
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    val movieTitle = movieDetail?.title ?: stringResource(R.string.nav_movies)
    val backdropUrl = movieDetail?.backdropUrl ?: movieDetail?.posterUrl
    val scrollState = rememberScrollState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background),
    ) {
        if (isFullscreen) {
            // -------------------------------------------------------------
            // FULLSCREEN LANDSCAPE MODE
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { controlsVisible = !controlsVisible },
                    ),
            ) {
                // Re-parent the existing persistent WebView without recreating it
                AndroidView(
                    factory = {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView
                    },
                    update = {},
                    modifier = Modifier.fillMaxSize(),
                )

                // Connecting / Thumbnail Overlay
                if (!isStreamPlaying && !hasError) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (backdropUrl != null) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = movieTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                        )

                        // Connecting Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(28.dp),
                                )
                                Column {
                                    Text(
                                        text = currentServer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = statusMessage ?: "Connecting to stream… (${attemptedServers.size}/${MovieServer.SERVERS.size})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // Resume Badge
                AnimatedVisibility(
                    visible = showResumeBadge && isStreamPlaying,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Resumed from ${savedRecord?.formattedPosition ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Restart",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    showResumeBadge = false
                                    MoviePlaybackManager.clearProgress(movieId)
                                    webView.evaluateJavascript(
                                        """
                                        (function() {
                                            var v = document.querySelector('video');
                                            if (v) v.currentTime = 0;
                                        })();
                                        """.trimIndent(),
                                        null,
                                    )
                                },
                            )
                        }
                    }
                }

                // Error Overlay
                if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Text(
                                text = errorMessage ?: stringResource(R.string.movie_player_all_failed),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    attemptedServers.clear()
                                    val firstServer = MovieServer.SERVERS.first()
                                    attemptedServers.add(firstServer.id)
                                    currentServer = firstServer
                                    hasError = false
                                    isStreamPlaying = false
                                    isConnecting = true
                                    statusMessage = "Connecting to ${firstServer.name} (1/${MovieServer.SERVERS.size})…"
                                    webView.loadUrl(firstServer.getUrl(movieId))
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.movie_retry))
                                }
                                Button(onClick = { showServerSheet = true }) {
                                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.movie_player_switch_server))
                                }
                            }
                        }
                    }
                }

                // Fullscreen Playback Controls Overlay
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { isFullscreen = false }) {
                                Icon(
                                    imageVector = Icons.Rounded.CloseFullscreen,
                                    contentDescription = stringResource(R.string.back),
                                    tint = Color.White,
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = movieTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { switchToNextServer("User next") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.movie_player_try_next_server))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { showServerSheet = true },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentServer.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // -------------------------------------------------------------
            // PORTRAIT RABBITMEOW-STYLE MODE
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding + 4.dp, bottom = 4.dp, start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = movieTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 16:9 Rounded Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                ) {
                    // Re-parent the existing persistent WebView without recreating it
                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView
                        },
                        update = {},
                        modifier = Modifier.fillMaxSize(),
                    )

                    // RabbitMeow-style Backdrop & Connecting Overlay
                    if (!isStreamPlaying && !hasError) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (backdropUrl != null) {
                                AsyncImage(
                                    model = backdropUrl,
                                    contentDescription = movieTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f)),
                            )

                            // Status on right side matching RabbitMeow layout
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 20.dp),
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text(
                                    text = currentServer.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = currentServer.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = statusMessage ?: "Connecting…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    // Resume Badge in Portrait
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showResumeBadge && isStreamPlaying,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            shadowElevation = 4.dp,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "Resumed from ${savedRecord?.formattedPosition ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Restart",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        showResumeBadge = false
                                        MoviePlaybackManager.clearProgress(movieId)
                                        webView.evaluateJavascript(
                                            """
                                            (function() {
                                                var v = document.querySelector('video');
                                                if (v) v.currentTime = 0;
                                            })();
                                            """.trimIndent(),
                                            null,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Fullscreen Expand Button (bottom right of player)
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInFull,
                                    contentDescription = stringResource(R.string.fullscreen),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Error overlay in card
                    if (hasError) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Text(
                                    text = errorMessage ?: stringResource(R.string.movie_player_all_failed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        attemptedServers.clear()
                                        val firstServer = MovieServer.SERVERS.first()
                                        attemptedServers.add(firstServer.id)
                                        currentServer = firstServer
                                        hasError = false
                                        isStreamPlaying = false
                                        isConnecting = true
                                        statusMessage = "Connecting to ${firstServer.name} (1/${MovieServer.SERVERS.size})…"
                                        webView.loadUrl(firstServer.getUrl(movieId))
                                    },
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.movie_retry), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Action Buttons Row (AutoNext, Details, Shuffle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { switchToNextServer("AutoNext") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AutoNext", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = { onNavigateToDetail(movieId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Details", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            val recommendations = movieDetail?.recommendations ?: emptyList()
                            val nextMovie = recommendations.randomOrNull()
                            if (nextMovie != null) {
                                onNavigateToDetail(nextMovie.id)
                            } else {
                                switchToNextServer("Shuffle")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Server Selector Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "If the stream does not load or subtitles do not work, choose a different server",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Server Selection Pill Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showServerSheet = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentServer.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = currentServer.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }

                // Movie Details Overview
                if (movieDetail != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (movieDetail.voteAverage > 0) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = movieDetail.ratingFormatted,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            if (movieDetail.releaseYear.isNotBlank()) {
                                Text(
                                    text = movieDetail.releaseYear,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (movieDetail.runtimeFormatted.isNotBlank()) {
                                Text(
                                    text = movieDetail.runtimeFormatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (movieDetail.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                movieDetail.genres.forEach { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ) {
                                        Text(
                                            text = genre.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        if (movieDetail.overview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = movieDetail.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f,
                            )
                        }

                        // Cast Row
                        if (movieDetail.cast.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.movie_details_cast),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(movieDetail.cast.take(10), key = { it.id }) { actor ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                text = actor.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            if (actor.character.isNotBlank()) {
                                                Text(
                                                    text = actor.character,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Server Switcher Bottom Sheet
    if (showServerSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showServerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = stringResource(R.string.movie_player_switch_server),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )

                MovieServer.SERVERS.forEach { server ->
                    val isSelected = server.id == currentServer.id
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (server.id != currentServer.id) {
                                    currentServer = server
                                    hasError = false
                                    isStreamPlaying = false
                                    isConnecting = true
                                    statusMessage = "Connecting to ${server.name}…"
                                    attemptedServers.clear()
                                    attemptedServers.add(server.id)
                                    webView.stopLoading()
                                    webView.loadUrl(server.getUrl(movieId))
                                }
                                scope.launch {
                                    sheetState.hide()
                                    showServerSheet = false
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = server.badge,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates sandboxed WebView with hardware-accelerated Direct Rendering (no LAYER_TYPE_HARDWARE offscreen layer),
 * zero frame jitter, pure black background (zero white flashes), and ad-blocking sandbox.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createSandboxedWebView(
    context: Context,
    savedPositionSeconds: Long,
    onPlaybackStarted: () -> Unit,
    onProgressUpdate: (currentSeconds: Double, durationSeconds: Double) -> Unit,
    onPageLoaded: () -> Unit,
    onErrorOccurred: (String) -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        // Pure black background — completely prevents white flash during load, seek, or buffer
        setBackgroundColor(android.graphics.Color.BLACK)

        // CRITICAL: LAYER_TYPE_NONE allows Chromium's GPU compositor to render video directly to surface without jitter/shattering
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
                // Disarm popups immediately at start before third-party ad scripts execute
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

                // Block non-HTTP schemes (intent://, market://, tel://, whatsapp://, etc.)
                if (scheme != "http" && scheme != "https") {
                    return true
                }

                val host = url.host?.lowercase().orEmpty()

                // NEVER let any ad redirect or popup navigate the player away from streaming
                if (!isAllowedStreamingHost(host) && urlString != "about:blank") {
                    return true
                }

                // Block known ad domains and ad URL patterns
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

                // Inject ad neutralizing script and player event listener to detect when HTML5 video actually plays and track position
                val seekInit = if (savedPositionSeconds > 10) "var savedTime = $savedPositionSeconds;" else "var savedTime = 0;"
                val js = """
                    (function() {
                        $seekInit
                        var hasResumed = false;

                        // Inject ad neutralizing CSS rule immediately
                        try {
                            var css = 'iframe[id*="ad"],iframe[class*="ad"],iframe[src*="ad"],iframe[src*="banner"],iframe[src*="pop"],div[id*="ad-"],div[class*="ad-"],div[id*="banner"],div[class*="banner"],div[id*="pop"],div[class*="pop"],div[class*="sponsor"],div[id*="sponsor"],[id*="google_ads"],[id*="aswift"],div[style*="z-index: 2147483647"],div[style*="z-index:999999"],div[style*="z-index: 999999"],div[style*="z-index:100000"],div[style*="z-index: 100000"],div[style*="z-index:99999"],div[style*="z-index: 99999"]{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important;}';
                            var styleTag = document.createElement('style');
                            styleTag.textContent = css;
                            (document.head || document.documentElement).appendChild(styleTag);
                        } catch(e) {}

                        // Disarm popups, alerts, and new-tab navigation
                        try {
                            window.open = function() { return null; };
                            window.alert = function() {};
                            window.confirm = function() { return false; };
                            window.prompt = function() { return null; };
                        } catch(e) {}

                        // Intercept click traps on capture phase
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

                        // Remove click-jack overlay divs and ad banners
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
                // CRITICAL FIX: Only fail on true main-frame errors; NEVER fail on subresources, ad blocks, or seek aborts (ERR_ABORTED / code -1)
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
                // Only fail if the main frame itself returns 404/502/503 on its initial page load
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
                // Reject all popups, popunders, and new tab requests created by embed ads
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
