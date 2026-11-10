package io.github.aedev.flow.data.movie

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovieModelsTest {

    @Test
    fun `MovieServer SERVERS list contains all 18 streaming mirrors`() {
        val servers = MovieServer.SERVERS
        assertThat(servers.size).isEqualTo(18)

        val serverIds = servers.map { it.id }.toSet()
        val expectedIds = setOf(
            "supernova", "forge", "aura", "ember", "rabbi",
            "pringle", "flux", "haven", "swift", "armageddon",
            "atlas", "pulse", "prism", "echo", "link",
            "loop", "prime", "nova",
        )
        assertThat(serverIds).containsExactlyElementsIn(expectedIds)
    }

    @Test
    fun `MovieServer getTvUrl replaces id, season, and episode correctly for all 18 servers`() {
        val tvId = 1396L // Breaking Bad
        val season = 2
        val episode = 5

        for (server in MovieServer.SERVERS) {
            val tvUrl = server.getTvUrl(tvId, season, episode)
            assertThat(tvUrl).isNotEmpty()
            assertThat(tvUrl).contains("1396")
            assertThat(tvUrl).contains("2")
            assertThat(tvUrl).contains("5")
            assertThat(tvUrl).doesNotContain("{id}")
            assertThat(tvUrl).doesNotContain("{season}")
            assertThat(tvUrl).doesNotContain("{episode}")
        }
    }

    @Test
    fun `MovieServer getTvUrl produces expected URL for specific servers`() {
        val supernova = MovieServer.findById("supernova")
        assertThat(supernova.getTvUrl(100L, 1, 2))
            .isEqualTo("https://zxcstream.cc/player/tv/100/1/2")

        val forge = MovieServer.findById("forge")
        assertThat(forge.getTvUrl(100L, 1, 2))
            .isEqualTo("https://vidlink.pro/tv/100/1/2?autoplay=true&title=true")

        val pulse = MovieServer.findById("pulse")
        assertThat(pulse.getTvUrl(100L, 1, 2))
            .isEqualTo("https://embed.smashystream.com/playere.php?tmdb=100&season=1&episode=2")
    }

    @Test
    fun `TvShowItem formats releaseYear, rating, and posterUrl properly`() {
        val item = TvShowItem(
            id = 1396L,
            name = "Breaking Bad",
            overview = "A chemistry teacher diagnosed with lung cancer...",
            posterPath = "/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
            backdropPath = "/tsRy63Mu5cu8etL1X7ZLyf7UP19.jpg",
            firstAirDate = "2008-01-20",
            voteAverage = 8.892,
            voteCount = 14500,
        )

        assertThat(item.releaseYear).isEqualTo("2008")
        assertThat(item.ratingFormatted).isEqualTo("8.9")
        assertThat(item.posterUrl).isEqualTo("https://image.tmdb.org/t/p/w342/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg")
        assertThat(item.backdropUrl).isEqualTo("https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP19.jpg")
    }

    @Test
    fun `TvEpisodeItem formats episodeCode and stillUrl properly`() {
        val episode = TvEpisodeItem(
            id = 62085L,
            episodeNumber = 7,
            seasonNumber = 1,
            name = "A No-Rough-Stuff-Type Deal",
            stillPath = "/still123.jpg",
            airDate = "2008-03-09",
            voteAverage = 8.8,
            runtime = 48,
        )

        assertThat(episode.episodeCode).isEqualTo("S01E07")
        assertThat(episode.stillUrl).isEqualTo("https://image.tmdb.org/t/p/w300/still123.jpg")
    }

    @Test
    fun `TmdbTvShowDto converts to TvShowItem properly`() {
        val dto = TmdbTvShowDto(
            id = 999L,
            name = "Severance",
            overview = "Mark leads a team of office workers...",
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            firstAirDate = "2022-02-18",
            voteAverage = 8.5,
            voteCount = 1200,
            genreIds = listOf(18, 9648),
        )

        val item = dto.toTvShowItem()
        assertThat(item.id).isEqualTo(999L)
        assertThat(item.name).isEqualTo("Severance")
        assertThat(item.releaseYear).isEqualTo("2022")
        assertThat(item.ratingFormatted).isEqualTo("8.5")
        assertThat(item.genreIds).containsExactly(18, 9648).inOrder()
    }

    @Test
    fun `TmdbTvEpisodeDto converts to TvEpisodeItem properly`() {
        val dto = TmdbTvEpisodeDto(
            id = 555L,
            episodeNumber = 1,
            seasonNumber = 1,
            name = "Good News About Hell",
            stillPath = "/still.jpg",
            airDate = "2022-02-18",
            voteAverage = 8.7,
            runtime = 57,
        )

        val item = dto.toTvEpisodeItem()
        assertThat(item.id).isEqualTo(555L)
        assertThat(item.episodeCode).isEqualTo("S01E01")
        assertThat(item.name).isEqualTo("Good News About Hell")
        assertThat(item.runtime).isEqualTo(57)
    }

    @Test
    fun `TmdbTvShowDetailDto converts to TvShowDetail with seasons and cast`() {
        val dto = TmdbTvShowDetailDto(
            id = 100L,
            name = "Arcane",
            overview = "Set in the utopian region of Piltover...",
            firstAirDate = "2021-11-06",
            voteAverage = 9.0,
            numberOfSeasons = 2,
            numberOfEpisodes = 18,
            genres = listOf(TmdbGenreDto(16, "Animation"), TmdbGenreDto(10765, "Sci-Fi & Fantasy")),
            seasons = listOf(
                TmdbTvSeasonSummaryDto(id = 1L, seasonNumber = 1, name = "Season 1", episodeCount = 9),
                TmdbTvSeasonSummaryDto(id = 2L, seasonNumber = 2, name = "Season 2", episodeCount = 9),
            ),
        )

        val detail = dto.toTvShowDetail()
        assertThat(detail.id).isEqualTo(100L)
        assertThat(detail.name).isEqualTo("Arcane")
        assertThat(detail.numberOfSeasons).isEqualTo(2)
        assertThat(detail.seasons.size).isEqualTo(2)
        assertThat(detail.seasons[0].seasonNumber).isEqualTo(1)
        assertThat(detail.seasons[0].episodeCount).isEqualTo(9)
        assertThat(detail.genres.map { it.name }).containsExactly("Animation", "Sci-Fi & Fantasy").inOrder()
    }
}
