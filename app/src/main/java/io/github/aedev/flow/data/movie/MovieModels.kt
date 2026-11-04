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
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val backdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

    val releaseYear: String
        get() = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""

    val ratingFormatted: String
        get() = String.format(Locale.US, "%.1f", voteAverage)
}

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
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val backdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

    val releaseYear: String
        get() = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""

    val ratingFormatted: String
        get() = String.format(Locale.US, "%.1f", voteAverage)

    val runtimeFormatted: String
        get() {
            if (runtime <= 0) return ""
            val hours = runtime / 60
            val minutes = runtime % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

@Immutable
data class MovieServer(
    val id: String,
    val name: String,
    val urlPattern: String,
) {
    fun getUrl(movieId: Long): String = urlPattern.replace("{id}", movieId.toString())

    companion object {
        val SERVERS = listOf(
            MovieServer("aura", "Aura", "https://player.videasy.net/movie/{id}"),
            MovieServer("forge", "Forge", "https://vidlink.pro/movie/{id}?autoplay=true&title=true"),
            MovieServer("supernova", "Supernova", "https://zxcstream.cc/player/movie/{id}"),
            MovieServer("pringle", "Pringle", "https://moviesapi.to/movie/{id}"),
            MovieServer("armageddon", "Armageddon", "https://vsembed.ru/embed/movie/{id}"),
            MovieServer("rabbi", "Rabbi", "https://vidrock.to/movie/{id}"),
            MovieServer("ember", "Ember", "https://vidfast.pro/movie/{id}?autoPlay=true"),
            MovieServer("atlas", "Atlas", "https://vidcore.net/movie/{id}"),
            MovieServer("pulse", "Pulse", "https://embed.smashystream.com/playere.php?tmdb={id}"),
            MovieServer("swift", "Swift", "https://vidup.to/movie/{id}?autoPlay=true"),
            MovieServer("prism", "Prism", "https://111movies.com/movie/{id}"),
            MovieServer("echo", "Echo", "https://vidsrcme.ru/embed/movie/{id}"),
            MovieServer("flux", "Flux", "https://player.vidzee.wtf/embed/movie/{id}"),
            MovieServer("haven", "Haven", "https://vidnest.fun/movie/{id}"),
            MovieServer("link", "Link", "https://www.2embed.cc/embed/{id}"),
            MovieServer("loop", "Loop", "https://multiembed.mov/?video_id={id}&tmdb=1"),
            MovieServer("prime", "Prime", "https://primesrc.me/embed/movie?tmdb={id}"),
            MovieServer("nova", "Nova", "https://vidlux.xyz/embed/movie/{id}"),
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
