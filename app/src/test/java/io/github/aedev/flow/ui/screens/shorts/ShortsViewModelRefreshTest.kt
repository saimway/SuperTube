package io.github.aedev.flow.ui.screens.shorts

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.engagement.VideoEngagementUseCase
import io.github.aedev.flow.data.local.PlaylistRepository
import io.github.aedev.flow.data.local.prefs.PlayerPreferences
import io.github.aedev.flow.data.model.ShortVideo
import io.github.aedev.flow.data.recommendation.BlockedChannelNames
import io.github.aedev.flow.data.repository.YouTubeRepository
import io.github.aedev.flow.data.shorts.ShortsFeedRepository
import io.github.aedev.flow.data.shorts.ShortsMetadataRepository
import io.github.aedev.flow.data.shorts.ShortsStreamResolver
import io.github.aedev.flow.data.shorts.ShortsWatchHistory
import io.github.aedev.flow.data.shorts.queue.ShortsQueueController
import io.github.aedev.flow.data.shorts.queue.ShortsQueueLoader
import io.github.aedev.flow.data.shorts.queue.ShortsQueueLoaderFactory
import io.github.aedev.flow.data.shorts.queue.ShortsQueuePage
import io.github.aedev.flow.data.shorts.queue.ShortsQueueSource
import io.github.aedev.flow.data.stats.VideoStatsRecorder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ShortsViewModelRefreshTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: Context = mockk(relaxed = true)
    private val repository: YouTubeRepository = mockk(relaxed = true)
    private val feed: ShortsFeedRepository = mockk(relaxed = true)
    private val streams: ShortsStreamResolver = mockk(relaxed = true)
    private val metadata: ShortsMetadataRepository = mockk(relaxed = true)
    private val engagement: VideoEngagementUseCase = mockk(relaxed = true)
    private val playlistRepository: PlaylistRepository = mockk(relaxed = true)
    private val watchHistory: ShortsWatchHistory = mockk(relaxed = true)
    private val queueFactory: ShortsQueueLoaderFactory = mockk()
    private val playerPreferences: PlayerPreferences = mockk(relaxed = true)
    private val videoStats: VideoStatsRecorder = mockk(relaxed = true)
    private val blockedNames: BlockedChannelNames = mockk(relaxed = true)

    private val initialShort1 = ShortVideo(id = "init_1", thumbnailUrl = "", title = "Initial 1", channelId = "channel_A", channelName = "Channel A")
    private val initialShort2 = ShortVideo(id = "init_2", thumbnailUrl = "", title = "Initial 2", channelId = "channel_B", channelName = "Channel B")

    private val freshShort1 = ShortVideo(id = "fresh_1", thumbnailUrl = "", title = "Fresh 1", channelId = "channel_C", channelName = "Channel C")
    private val freshShort2 = ShortVideo(id = "fresh_2", thumbnailUrl = "", title = "Fresh 2", channelId = "channel_D", channelName = "Channel D")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playlistRepository.getSavedShortsFlow() } returns flowOf(emptyList())
        every { queueFactory.resolve(any()) } returns ShortsQueueSource.Feed
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): ShortsViewModel =
        ShortsViewModel(
            context = context,
            repository = repository,
            feed = feed,
            streams = streams,
            metadata = metadata,
            engagement = engagement,
            playlistRepository = playlistRepository,
            watchHistory = watchHistory,
            queueFactory = queueFactory,
            playerPreferences = playerPreferences,
            videoStats = videoStats,
            blockedNames = blockedNames,
        )

    @Test
    fun `refreshFeed clears caches and updates shorts queue with fresh items and index zero`() =
        runTest(testDispatcher) {
            val initialLoader = mockk<ShortsQueueLoader> {
                coEvery { initial() } returns ShortsQueuePage(items = listOf(initialShort1, initialShort2), exhausted = true)
                coEvery { more(any()) } returns ShortsQueuePage(items = emptyList(), exhausted = true)
            }
            val initialController = ShortsQueueController(primary = initialLoader)

            val freshLoader = mockk<ShortsQueueLoader> {
                coEvery { initial() } returns ShortsQueuePage(items = listOf(freshShort1, freshShort2), exhausted = true)
                coEvery { more(any()) } returns ShortsQueuePage(items = emptyList(), exhausted = true)
            }
            val freshController = ShortsQueueController(primary = freshLoader)

            every { queueFactory.create(any()) } returns initialController andThen freshController

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.load(ShortsQueueSource.Feed)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.shorts.map { it.id }).containsExactly("init_1", "init_2").inOrder()

            // Act: Refresh feed
            viewModel.refreshFeed(ShortsQueueSource.Feed)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: feed.clearCaches was called
            verify(atLeast = 1) { feed.clearCaches() }

            // Assert: Fresh items are published and index is 0
            val state = viewModel.uiState.value
            assertThat(state.shorts.map { it.id }).containsExactly("fresh_1", "fresh_2").inOrder()
            assertThat(state.currentIndex).isEqualTo(0)
            assertThat(state.error).isNull()
            assertThat(state.isRefreshing).isFalse()
        }

    @Test
    fun `refreshFeed suppresses errors and preserves existing shorts when network fails`() =
        runTest(testDispatcher) {
            val initialLoader = mockk<ShortsQueueLoader> {
                coEvery { initial() } returns ShortsQueuePage(items = listOf(initialShort1, initialShort2), exhausted = true)
                coEvery { more(any()) } returns ShortsQueuePage(items = emptyList(), exhausted = true)
            }
            val initialController = ShortsQueueController(primary = initialLoader)

            val failingLoader = mockk<ShortsQueueLoader> {
                coEvery { initial() } throws IOException("Network timeout")
            }
            val failingController = ShortsQueueController(primary = failingLoader)

            every { queueFactory.create(any()) } returns initialController andThen failingController

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.load(ShortsQueueSource.Feed)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.shorts.map { it.id }).containsExactly("init_1", "init_2")

            // Act: Refresh feed with failing network
            viewModel.refreshFeed(ShortsQueueSource.Feed)
            // Advance through exponential backoff delays: 1s, 2s, 4s
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: Error is suppressed, existing shorts remain intact, snackbar is not emitted
            val state = viewModel.uiState.value
            assertThat(state.shorts.map { it.id }).containsExactly("init_1", "init_2").inOrder()
            assertThat(state.error).isNull()
            assertThat(state.isRefreshing).isFalse()
            assertThat(viewModel.snackbarMessage.value).isNull()
        }
}
