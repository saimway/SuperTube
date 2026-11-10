package io.github.aedev.flow.data.movie

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class MoviePlaybackManagerTest {

    @Before
    fun setUp() {
        MoviePlaybackManager.clearLastWatched()
    }

    @Test
    fun `LastWatchedMedia formattedPosition handles minutes and hours properly`() {
        val recordUnderHour = LastWatchedMedia(
            mediaId = 1L,
            mediaType = MediaType.MOVIE,
            title = "Test Movie",
            positionSeconds = 125L, // 2m 5s
            durationSeconds = 6000L,
        )
        assertThat(recordUnderHour.formattedPosition).isEqualTo("02:05")

        val recordOverHour = LastWatchedMedia(
            mediaId = 1L,
            mediaType = MediaType.MOVIE,
            title = "Test Movie",
            positionSeconds = 3665L, // 1h 1m 5s
            durationSeconds = 7200L,
        )
        assertThat(recordOverHour.formattedPosition).isEqualTo("1:01:05")
    }

    @Test
    fun `LastWatchedMedia progressFraction calculates correct percentage`() {
        val media = LastWatchedMedia(
            mediaId = 1L,
            mediaType = MediaType.TV,
            title = "Show",
            positionSeconds = 30L,
            durationSeconds = 100L,
        )
        assertThat(media.progressFraction).isEqualTo(0.3f)
    }

    @Test
    fun `saveLastWatched and getLastWatched preserves media details in memory`() {
        val lastWatched = LastWatchedMedia(
            mediaId = 1396L,
            mediaType = MediaType.TV,
            title = "Breaking Bad",
            seasonNumber = 1,
            episodeNumber = 3,
            episodeTitle = "...And the Bag's in the River",
            serverId = "forge",
            positionSeconds = 950L,
            durationSeconds = 2800L,
        )

        MoviePlaybackManager.saveLastWatched(lastWatched)
        val retrieved = MoviePlaybackManager.getLastWatched()

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.mediaId).isEqualTo(1396L)
        assertThat(retrieved.mediaType).isEqualTo(MediaType.TV)
        assertThat(retrieved.title).isEqualTo("Breaking Bad")
        assertThat(retrieved.seasonNumber).isEqualTo(1)
        assertThat(retrieved.episodeNumber).isEqualTo(3)
        assertThat(retrieved.episodeTitle).isEqualTo("...And the Bag's in the River")
        assertThat(retrieved.serverId).isEqualTo("forge")
        assertThat(retrieved.positionSeconds).isEqualTo(950L)
        assertThat(retrieved.durationSeconds).isEqualTo(2800L)
    }

    @Test
    fun `clearLastWatched removes last watched item`() {
        val lastWatched = LastWatchedMedia(
            mediaId = 550L,
            mediaType = MediaType.MOVIE,
            title = "Fight Club",
            positionSeconds = 500L,
        )
        MoviePlaybackManager.saveLastWatched(lastWatched)
        assertThat(MoviePlaybackManager.getLastWatched()).isNotNull()

        MoviePlaybackManager.clearLastWatched()
        assertThat(MoviePlaybackManager.getLastWatched()).isNull()
    }

    @Test
    fun `saveTvProgress stores progress and updates last watched media automatically`() {
        MoviePlaybackManager.saveTvProgress(
            tvId = 200L,
            seasonNumber = 2,
            episodeNumber = 4,
            serverId = "supernova",
            positionSeconds = 450L,
            durationSeconds = 1800L,
            title = "Stranger Things",
            episodeTitle = "Chapter Four: Will the Wise",
        )

        val progress = MoviePlaybackManager.getTvProgress(tvId = 200L, seasonNumber = 2, episodeNumber = 4)
        assertThat(progress).isNotNull()
        assertThat(progress!!.movieId).isEqualTo(200L)
        assertThat(progress.serverId).isEqualTo("supernova")
        assertThat(progress.positionSeconds).isEqualTo(450L)
        assertThat(progress.durationSeconds).isEqualTo(1800L)

        // Check global last watched
        val lastWatched = MoviePlaybackManager.getLastWatched()
        assertThat(lastWatched).isNotNull()
        assertThat(lastWatched!!.mediaId).isEqualTo(200L)
        assertThat(lastWatched.mediaType).isEqualTo(MediaType.TV)
        assertThat(lastWatched.title).isEqualTo("Stranger Things")
        assertThat(lastWatched.seasonNumber).isEqualTo(2)
        assertThat(lastWatched.episodeNumber).isEqualTo(4)
        assertThat(lastWatched.episodeTitle).isEqualTo("Chapter Four: Will the Wise")
    }

    @Test
    fun `clearTvProgress removes TV episode progress`() {
        MoviePlaybackManager.saveTvProgress(
            tvId = 300L,
            seasonNumber = 1,
            episodeNumber = 1,
            serverId = "ember",
            positionSeconds = 200L,
            durationSeconds = 1500L,
            title = "Succession",
        )
        assertThat(MoviePlaybackManager.getTvProgress(300L, 1, 1)).isNotNull()

        MoviePlaybackManager.clearTvProgress(300L, 1, 1)
        assertThat(MoviePlaybackManager.getTvProgress(300L, 1, 1)).isNull()
    }

    @Test
    fun `lastWatchedFlow emits value changes reactively`() {
        val lastWatched = LastWatchedMedia(
            mediaId = 999L,
            mediaType = MediaType.TV,
            title = "Test Reactive Show",
            seasonNumber = 1,
            episodeNumber = 1,
            positionSeconds = 120L,
            durationSeconds = 2400L,
        )

        MoviePlaybackManager.saveLastWatched(lastWatched)
        assertThat(MoviePlaybackManager.lastWatchedFlow.value).isEqualTo(lastWatched)

        MoviePlaybackManager.clearLastWatched()
        assertThat(MoviePlaybackManager.lastWatchedFlow.value).isNull()
    }
}
