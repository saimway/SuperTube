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

@Immutable
data class MovieServer(
    val id: String,
    val name: String,
    val urlPattern: String,
    val badge: String = "HD Quality",
) {
    fun getUrl(movieId: Long): String = urlPattern.replace("{id}", movieId.toString())

    companion object {
        val SERVERS = listOf(
            MovieServer("supernova", "01 Supernova", "https://zxcstream.cc/player/movie/{id}", "Multi Audio & Fast"),
            MovieServer("forge", "02 Forge", "https://vidlink.pro/movie/{id}?autoplay=true&title=true", "HD 1080p Stream"),
            MovieServer("aura", "03 Aura", "https://player.videasy.net/movie/{id}", "Multi Subtitles"),
            MovieServer("ember", "04 Ember", "https://vidfast.pro/movie/{id}?autoPlay=true", "Fast CDN"),
            MovieServer("rabbi", "05 Rabbi", "https://vidrock.to/movie/{id}", "Direct Stream"),
            MovieServer("pringle", "06 Pringle", "https://moviesapi.to/movie/{id}", "HD Quality"),
            MovieServer("flux", "07 Flux", "https://player.vidzee.wtf/embed/movie/{id}", "Ultra Fast"),
            MovieServer("haven", "08 Haven", "https://vidnest.fun/movie/{id}", "Full HD"),
            MovieServer("swift", "09 Swift", "https://vidup.to/movie/{id}?autoPlay=true", "Speed Stream"),
            MovieServer("armageddon", "10 Armageddon", "https://vsembed.ru/embed/movie/{id}", "Backup Fast"),
            MovieServer("atlas", "11 Atlas", "https://vidcore.net/movie/{id}", "Standard HD"),
            MovieServer("pulse", "12 Pulse", "https://embed.smashystream.com/playere.php?tmdb={id}", "Global Stream"),
            MovieServer("prism", "13 Prism", "https://111movies.com/movie/{id}", "Multi Host"),
            MovieServer("echo", "14 Echo", "https://vidsrcme.ru/embed/movie/{id}", "Original HD"),
            MovieServer("link", "15 Link", "https://www.2embed.cc/embed/{id}", "Alternative"),
            MovieServer("loop", "16 Loop", "https://multiembed.mov/?video_id={id}&tmdb=1", "Mirror 1"),
            MovieServer("prime", "17 Prime", "https://primesrc.me/embed/movie?tmdb={id}", "Mirror 2"),
            MovieServer("nova", "18 Nova", "https://vidlux.xyz/embed/movie/{id}", "Mirror 3"),
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
