package io.github.aedev.flow.data.movie

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Immutable
data class MovieItem(
    val id: Long,
    val title: String,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val genreIds: List<Int> = emptyList(),
    val posterUrl: String? = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
    val backdropUrl: String? = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
    val releaseYear: String = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "",
    val ratingFormatted: String = if (voteAverage > 0) String.format(Locale.US, "%.1f", voteAverage) else "",
)

@Immutable
data class MovieGenre(
    val id: Int,
    val name: String,
)

@Immutable
data class CastMember(
    val id: Long,
    val name: String,
    val character: String = "",
    val profilePath: String? = null,
) {
    val profileUrl: String?
        get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
}

@Immutable
data class MovieDetail(
    val id: Long,
    val title: String,
    val overview: String = "",
    val tagline: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val runtime: Int = 0,
    val genres: List<MovieGenre> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val recommendations: List<MovieItem> = emptyList(),
    val posterUrl: String? = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
    val backdropUrl: String? = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
    val releaseYear: String = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "",
    val ratingFormatted: String = if (voteAverage > 0) String.format(Locale.US, "%.1f", voteAverage) else "",
    val runtimeFormatted: String = if (runtime > 0) {
        val h = runtime / 60
        val m = runtime % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    } else "",
)

// -----------------------------------------------------------------------------
// TV Show Domain Models
// -----------------------------------------------------------------------------

@Immutable
data class TvShowItem(
    val id: Long,
    val name: String,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val firstAirDate: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val genreIds: List<Int> = emptyList(),
    val posterUrl: String? = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
    val backdropUrl: String? = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
    val releaseYear: String = if (firstAirDate.length >= 4) firstAirDate.substring(0, 4) else "",
    val ratingFormatted: String = if (voteAverage > 0) String.format(Locale.US, "%.1f", voteAverage) else "",
)

@Immutable
data class TvSeasonSummary(
    val id: Long,
    val seasonNumber: Int,
    val name: String,
    val overview: String = "",
    val posterPath: String? = null,
    val episodeCount: Int = 0,
    val posterUrl: String? = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
)

@Immutable
data class TvEpisodeItem(
    val id: Long,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String = "",
    val stillPath: String? = null,
    val airDate: String = "",
    val voteAverage: Double = 0.0,
    val runtime: Int = 0,
    val stillUrl: String? = stillPath?.let { "https://image.tmdb.org/t/p/w300$it" },
    val episodeCode: String = "S%02dE%02d".format(seasonNumber, episodeNumber),
)

@Immutable
data class TvShowDetail(
    val id: Long,
    val name: String,
    val overview: String = "",
    val tagline: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val firstAirDate: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val numberOfSeasons: Int = 0,
    val numberOfEpisodes: Int = 0,
    val genres: List<MovieGenre> = emptyList(),
    val seasons: List<TvSeasonSummary> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val recommendations: List<TvShowItem> = emptyList(),
    val posterUrl: String? = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
    val backdropUrl: String? = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
    val releaseYear: String = if (firstAirDate.length >= 4) firstAirDate.substring(0, 4) else "",
    val ratingFormatted: String = if (voteAverage > 0) String.format(Locale.US, "%.1f", voteAverage) else "",
)

@Immutable
data class MovieServer(
    val id: String,
    val name: String,
    val urlPattern: String,
    val badge: String = "HD Quality",
    val tvUrlPattern: String? = null,
) {
    fun getUrl(movieId: Long): String = urlPattern.replace("{id}", movieId.toString())

    fun getTvUrl(tvId: Long, season: Int, episode: Int): String {
        val pattern = tvUrlPattern ?: when {
            urlPattern.contains("/movie/{id}") -> urlPattern.replace("/movie/{id}", "/tv/{id}/{season}/{episode}")
            else -> "https://vidlink.pro/tv/{id}/{season}/{episode}?autoplay=true&title=true"
        }
        return pattern
            .replace("{id}", tvId.toString())
            .replace("{season}", season.toString())
            .replace("{episode}", episode.toString())
    }

    companion object {
        val SERVERS = listOf(
            MovieServer(
                id = "supernova",
                name = "01 Supernova",
                urlPattern = "https://zxcstream.cc/player/movie/{id}",
                badge = "Multi Audio & Fast",
                tvUrlPattern = "https://zxcstream.cc/player/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "forge",
                name = "02 Forge",
                urlPattern = "https://vidlink.pro/movie/{id}?autoplay=true&title=true",
                badge = "HD 1080p Stream",
                tvUrlPattern = "https://vidlink.pro/tv/{id}/{season}/{episode}?autoplay=true&title=true",
            ),
            MovieServer(
                id = "aura",
                name = "03 Aura",
                urlPattern = "https://player.videasy.net/movie/{id}",
                badge = "Multi Subtitles",
                tvUrlPattern = "https://player.videasy.net/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "ember",
                name = "04 Ember",
                urlPattern = "https://vidfast.pro/movie/{id}?autoPlay=true",
                badge = "Fast CDN",
                tvUrlPattern = "https://vidfast.pro/tv/{id}/{season}/{episode}?autoPlay=true",
            ),
            MovieServer(
                id = "rabbi",
                name = "05 Rabbi",
                urlPattern = "https://vidrock.to/movie/{id}",
                badge = "Direct Stream",
                tvUrlPattern = "https://vidrock.to/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "pringle",
                name = "06 Pringle",
                urlPattern = "https://moviesapi.to/movie/{id}",
                badge = "HD Quality",
                tvUrlPattern = "https://moviesapi.to/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "flux",
                name = "07 Flux",
                urlPattern = "https://player.vidzee.wtf/embed/movie/{id}",
                badge = "Ultra Fast",
                tvUrlPattern = "https://player.vidzee.wtf/embed/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "haven",
                name = "08 Haven",
                urlPattern = "https://vidnest.fun/movie/{id}",
                badge = "Full HD",
                tvUrlPattern = "https://vidnest.fun/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "swift",
                name = "09 Swift",
                urlPattern = "https://vidup.to/movie/{id}?autoPlay=true",
                badge = "Speed Stream",
                tvUrlPattern = "https://vidup.to/tv/{id}/{season}/{episode}?autoPlay=true",
            ),
            MovieServer(
                id = "armageddon",
                name = "10 Armageddon",
                urlPattern = "https://vsembed.ru/embed/movie/{id}",
                badge = "Backup Fast",
                tvUrlPattern = "https://vsembed.ru/embed/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "atlas",
                name = "11 Atlas",
                urlPattern = "https://vidcore.net/movie/{id}",
                badge = "Standard HD",
                tvUrlPattern = "https://vidcore.net/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "pulse",
                name = "12 Pulse",
                urlPattern = "https://embed.smashystream.com/playere.php?tmdb={id}",
                badge = "Global Stream",
                tvUrlPattern = "https://embed.smashystream.com/playere.php?tmdb={id}&season={season}&episode={episode}",
            ),
            MovieServer(
                id = "prism",
                name = "13 Prism",
                urlPattern = "https://111movies.com/movie/{id}",
                badge = "Multi Host",
                tvUrlPattern = "https://111movies.com/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "echo",
                name = "14 Echo",
                urlPattern = "https://vidsrcme.ru/embed/movie/{id}",
                badge = "Original HD",
                tvUrlPattern = "https://vidsrcme.ru/embed/tv/{id}/{season}/{episode}",
            ),
            MovieServer(
                id = "link",
                name = "15 Link",
                urlPattern = "https://www.2embed.cc/embed/{id}",
                badge = "Alternative",
                tvUrlPattern = "https://www.2embed.cc/embedtv/{id}&s={season}&e={episode}",
            ),
            MovieServer(
                id = "loop",
                name = "16 Loop",
                urlPattern = "https://multiembed.mov/?video_id={id}&tmdb=1",
                badge = "Mirror 1",
                tvUrlPattern = "https://multiembed.mov/?video_id={id}&tmdb=1&s={season}&e={episode}",
            ),
            MovieServer(
                id = "prime",
                name = "17 Prime",
                urlPattern = "https://primesrc.me/embed/movie?tmdb={id}",
                badge = "Mirror 2",
                tvUrlPattern = "https://primesrc.me/embed/tv?tmdb={id}&season={season}&episode={episode}",
            ),
            MovieServer(
                id = "nova",
                name = "18 Nova",
                urlPattern = "https://vidlux.xyz/embed/movie/{id}",
                badge = "Mirror 3",
                tvUrlPattern = "https://vidlux.xyz/embed/tv/{id}/{season}/{episode}",
            ),
        )

        val DEFAULT_SERVER = SERVERS.first()

        fun findById(id: String?): MovieServer =
            SERVERS.firstOrNull { it.id == id } ?: DEFAULT_SERVER
    }
}

// -----------------------------------------------------------------------------
// TMDB API DTOs
// -----------------------------------------------------------------------------

@Serializable
internal data class TmdbPagedResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
internal data class TmdbMovieDto(
    val id: Long,
    val title: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
) {
    fun toMovieItem(): MovieItem =
        MovieItem(
            id = id,
            title = title.orEmpty(),
            overview = overview.orEmpty(),
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate.orEmpty(),
            voteAverage = voteAverage,
            voteCount = voteCount,
            genreIds = genreIds,
        )
}

@Serializable
internal data class TmdbGenreDto(
    val id: Int,
    val name: String,
) {
    fun toMovieGenre(): MovieGenre = MovieGenre(id = id, name = name)
}

@Serializable
internal data class TmdbCastMemberDto(
    val id: Long,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
) {
    fun toCastMember(): CastMember =
        CastMember(
            id = id,
            name = name,
            character = character.orEmpty(),
            profilePath = profilePath,
        )
}

@Serializable
internal data class TmdbCreditsDto(
    val cast: List<TmdbCastMemberDto> = emptyList(),
)

@Serializable
internal data class TmdbMovieDetailDto(
    val id: Long,
    val title: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val runtime: Int? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val credits: TmdbCreditsDto? = null,
    val recommendations: TmdbPagedResponse<TmdbMovieDto>? = null,
) {
    fun toMovieDetail(): MovieDetail =
        MovieDetail(
            id = id,
            title = title.orEmpty(),
            overview = overview.orEmpty(),
            tagline = tagline.orEmpty(),
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate.orEmpty(),
            voteAverage = voteAverage,
            voteCount = voteCount,
            runtime = runtime ?: 0,
            genres = genres.map { it.toMovieGenre() },
            cast = credits?.cast?.take(20)?.map { it.toCastMember() } ?: emptyList(),
            recommendations = recommendations?.results?.map { it.toMovieItem() } ?: emptyList(),
        )
}

@Serializable
internal data class TmdbTvShowDto(
    val id: Long,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
) {
    fun toTvShowItem(): TvShowItem =
        TvShowItem(
            id = id,
            name = name.orEmpty(),
            overview = overview.orEmpty(),
            posterPath = posterPath,
            backdropPath = backdropPath,
            firstAirDate = firstAirDate.orEmpty(),
            voteAverage = voteAverage,
            voteCount = voteCount,
            genreIds = genreIds,
        )
}

@Serializable
internal data class TmdbTvSeasonSummaryDto(
    val id: Long = 0L,
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0,
) {
    fun toTvSeasonSummary(): TvSeasonSummary =
        TvSeasonSummary(
            id = id,
            seasonNumber = seasonNumber,
            name = name.orEmpty(),
            overview = overview.orEmpty(),
            posterPath = posterPath,
            episodeCount = episodeCount,
        )
}

@Serializable
internal data class TmdbTvEpisodeDto(
    val id: Long = 0L,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val runtime: Int? = null,
) {
    fun toTvEpisodeItem(): TvEpisodeItem =
        TvEpisodeItem(
            id = id,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            name = name.orEmpty(),
            overview = overview.orEmpty(),
            stillPath = stillPath,
            airDate = airDate.orEmpty(),
            voteAverage = voteAverage,
            runtime = runtime ?: 0,
        )
}

@Serializable
internal data class TmdbTvShowDetailDto(
    val id: Long,
    val name: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    val genres: List<TmdbGenreDto> = emptyList(),
    val seasons: List<TmdbTvSeasonSummaryDto> = emptyList(),
    val credits: TmdbCreditsDto? = null,
    val recommendations: TmdbPagedResponse<TmdbTvShowDto>? = null,
) {
    fun toTvShowDetail(): TvShowDetail =
        TvShowDetail(
            id = id,
            name = name.orEmpty(),
            overview = overview.orEmpty(),
            tagline = tagline.orEmpty(),
            posterPath = posterPath,
            backdropPath = backdropPath,
            firstAirDate = firstAirDate.orEmpty(),
            voteAverage = voteAverage,
            voteCount = voteCount,
            numberOfSeasons = numberOfSeasons,
            numberOfEpisodes = numberOfEpisodes,
            genres = genres.map { it.toMovieGenre() },
            seasons = seasons.map { it.toTvSeasonSummary() },
            cast = credits?.cast?.take(20)?.map { it.toCastMember() } ?: emptyList(),
            recommendations = recommendations?.results?.map { it.toTvShowItem() } ?: emptyList(),
        )
}

@Serializable
internal data class TmdbTvSeasonDetailDto(
    val id: Long = 0L,
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    val episodes: List<TmdbTvEpisodeDto> = emptyList(),
)
