package io.github.aedev.flow.ui.screens.shorts

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.engagement.FeedInvalidationBus
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

@OptIn(ExperimentalCoroutinesApi::class)
class ShortsViewModelFeedInvalidationTest {

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

    private val short1 = ShortVideo(id = "short_1", thumbnailUrl = "", title = "Short 1", channelId = "channel_A", channelName = "Channel A")
    private val short2 = ShortVideo(id = "short_2", thumbnailUrl = "", title = "Short 2", channelId = "channel_B", channelName = "Channel B")
    private val short3 = ShortVideo(id = "short_3", thumbnailUrl = "", title = "Short 3", channelId = "channel_C", channelName = "Channel C")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playlistRepository.getSavedShortsFlow() } returns flowOf(emptyList())
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
    fun `FeedInvalidationBus NotInterested event removes video from active shorts queue`() =
        runTest(testDispatcher) {
            // Arrange
            val queueLoader = mockk<ShortsQueueLoader> {
                coEvery { initial() } returns ShortsQueuePage(items = listOf(short1, short2, short3), exhausted = true)
                coEvery { more(any()) } returns ShortsQueuePage(items = emptyList(), exhausted = true)
            }
            val controller = ShortsQueueController(primary = queueLoader)
            every { queueFactory.resolve(any()) } returns ShortsQueueSource.Feed
            every { queueFactory.create(any()) } returns controller

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.load(ShortsQueueSource.Feed)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.shorts.map { it.id }).containsExactly("short_1", "short_2", "short_3").inOrder()

            // Act: emit NotInterested event for short_1
            FeedInvalidationBus.emit(FeedInvalidationBus.Event.NotInterested(short1.id, short1.channelId))
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: short_1 was immediately removed from the UI state
            val remainingIds = viewModel.uiState.value.shorts.map { it.id }
            assertThat(remainingIds).containsExactly("short_2", "short_3").inOrder()
            assertThat(remainingIds).doesNotContain("short_1")
        }

    @Test
    fun `FeedInvalidationBus ChannelBlocked event drops all videos from blocked channel`() =
        runTest(testDispatcher) {
            // Arrange
            val shortB2 = ShortVideo(id = "short_2b", thumbnailUrl = "", title = "Short 2B", channelId = "channel_B", channelName = "Channel B")
            val queueLoader = mockk<ShortsQueueLoader> {
                coEvery { initial() } returns ShortsQueuePage(items = listOf(short1, short2, shortB2, short3), exhausted = true)
                coEvery { more(any()) } returns ShortsQueuePage(items = emptyList(), exhausted = true)
            }
            val controller = ShortsQueueController(primary = queueLoader)
            every { queueFactory.resolve(any()) } returns ShortsQueueSource.Feed
            every { queueFactory.create(any()) } returns controller

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.load(ShortsQueueSource.Feed)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.shorts.map { it.id }).containsExactly("short_1", "short_2", "short_2b", "short_3")

            // Act: block channel_B
            FeedInvalidationBus.emit(FeedInvalidationBus.Event.ChannelBlocked("channel_B", "short_2"))
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: both short_2 and short_2b from channel_B are dropped
            val remainingIds = viewModel.uiState.value.shorts.map { it.id }
            assertThat(remainingIds).containsExactly("short_1", "short_3").inOrder()
            assertThat(remainingIds).doesNotContain("short_2")
            assertThat(remainingIds).doesNotContain("short_2b")
        }
}
