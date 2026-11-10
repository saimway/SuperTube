package io.github.aedev.flow.data.engagement

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.local.PlaylistRepository
import io.github.aedev.flow.data.local.ViewHistory
import io.github.aedev.flow.data.local.WatchLaterCleanup
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.recommendation.BlockedChannelNames
import io.github.aedev.flow.data.recommendation.DismissedVideoStore
import io.github.aedev.flow.data.recommendation.FlowNeuroEngine
import io.github.aedev.flow.data.repository.YouTubeRepository
import io.github.aedev.flow.data.stats.LedgerAction
import io.github.aedev.flow.data.stats.VideoStatsRecorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class VideoFeedbackUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val repository: YouTubeRepository = mockk(relaxed = true)
    private val playlistRepository: PlaylistRepository = mockk(relaxed = true)
    private val viewHistory: ViewHistory = mockk(relaxed = true)
    private val videoStats: VideoStatsRecorder = mockk(relaxed = true)
    private val watchLaterCleanup: WatchLaterCleanup = mockk(relaxed = true)
    private val blockedNames: BlockedChannelNames = mockk(relaxed = true)
    private val dismissedStore: DismissedVideoStore = mockk(relaxed = true)

    private lateinit var useCase: VideoFeedbackUseCase

    private val sampleVideo =
        Video(
            id = "test_vid_123",
            title = "Awesome Video",
            channelName = "Awesome Channel",
            channelId = "UC_awesome",
            thumbnailUrl = "https://thumb/test.jpg",
            duration = 180,
            viewCount = 5000L,
            uploadDate = "2026-02-01",
        )

    @Before
    fun setUp() {
        mockkObject(FlowNeuroEngine)
        coEvery { FlowNeuroEngine.markNotInterested(any<Context>(), any()) } returns Unit
        coEvery { FlowNeuroEngine.unsuppressVideo(any<Context>(), any()) } returns Unit

        useCase =
            VideoFeedbackUseCase(
                context = context,
                repository = repository,
                playlistRepository = playlistRepository,
                viewHistory = viewHistory,
                videoStats = videoStats,
                watchLaterCleanup = watchLaterCleanup,
                blockedNames = blockedNames,
                dismissedStore = dismissedStore,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `markNotInterested informs engine, records to store, records stats and emits event`() =
        runTest {
            // Arrange
            val emittedEvents = mutableListOf<FeedInvalidationBus.Event>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                FeedInvalidationBus.events.collect { emittedEvents += it }
            }

            // Act
            useCase.markNotInterested(sampleVideo)

            // Assert
            coVerify(exactly = 1) { FlowNeuroEngine.markNotInterested(context, sampleVideo) }
            coVerify(exactly = 1) { dismissedStore.record(sampleVideo) }
            coVerify(exactly = 1) { videoStats.onAction(LedgerAction.NOT_INTERESTED) }

            val notInterestedEvent = emittedEvents.filterIsInstance<FeedInvalidationBus.Event.NotInterested>().lastOrNull()
            assertThat(notInterestedEvent).isNotNull()
            assertThat(notInterestedEvent?.videoId).isEqualTo("test_vid_123")
            assertThat(notInterestedEvent?.channelId).isEqualTo("UC_awesome")

            job.cancel()
        }

    @Test
    fun `undoNotInterested unsuppresses in engine and forgets from store`() =
        runTest {
            // Arrange
            val videoId = "test_vid_undo"

            // Act
            useCase.undoNotInterested(videoId)

            // Assert
            coVerify(exactly = 1) { FlowNeuroEngine.unsuppressVideo(context, videoId) }
            coVerify(exactly = 1) { dismissedStore.forget(videoId) }
        }
}
