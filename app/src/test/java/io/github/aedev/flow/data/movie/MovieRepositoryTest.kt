package io.github.aedev.flow.data.movie

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class MovieRepositoryTest {

    private val requestCount = AtomicInteger()

    private fun createRepository(
        responseProvider: (url: String) -> Pair<Int, String>,
    ): MovieRepositoryImpl {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestCount.incrementAndGet()
                val url = chain.request().url.toString()
                val (code, body) = responseProvider(url)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return MovieRepositoryImpl(client)
    }

    private val pagedMoviesJson = """
        {
          "page": 1,
          "results": [
            {
              "id": 550,
              "title": "Fight Club",
              "overview": "An insomniac office worker...",
              "poster_path": "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
              "backdrop_path": "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
              "release_date": "1999-10-15",
              "vote_average": 8.433,
              "vote_count": 26280,
              "genre_ids": [18, 53]
            }
          ],
          "total_pages": 1,
          "total_results": 1
        }
    """.trimIndent()

    private val movieDetailJson = """
        {
          "id": 550,
          "title": "Fight Club",
          "overview": "An insomniac office worker...",
          "tagline": "Mischief. Mayhem. Soap.",
          "poster_path": "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
          "backdrop_path": "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
          "release_date": "1999-10-15",
          "vote_average": 8.4,
          "vote_count": 26280,
          "runtime": 139,
          "genres": [
            { "id": 18, "name": "Drama" },
            { "id": 53, "name": "Thriller" }
          ],
          "credits": {
            "cast": [
              {
                "id": 287,
                "name": "Brad Pitt",
                "character": "Tyler Durden",
                "profile_path": "/cckcYc2v0yh1tc9QjRelptsqpHW.jpg"
              }
            ]
          },
          "recommendations": {
            "page": 1,
            "results": [],
            "total_pages": 1,
            "total_results": 0
          }
        }
    """.trimIndent()

    @Test
    fun `getTrendingMovies parses response into MovieItems and caches result`() = runTest {
        val repo = createRepository { 200 to pagedMoviesJson }

        val result1 = repo.getTrendingMovies(page = 1)
        assertThat(result1.isSuccess).isTrue()
        val movies = result1.getOrNull()
        assertThat(movies).isNotNull()
        assertThat(movies).hasSize(1)
        val movie = movies!!.first()
        assertThat(movie.id).isEqualTo(550L)
        assertThat(movie.title).isEqualTo("Fight Club")
        assertThat(movie.releaseYear).isEqualTo("1999")
        assertThat(movie.ratingFormatted).isEqualTo("8.4")
        assertThat(requestCount.get()).isEqualTo(1)

        // Second call should hit the in-memory cache with zero new network requests
        val result2 = repo.getTrendingMovies(page = 1)
        assertThat(result2.isSuccess).isTrue()
        assertThat(requestCount.get()).isEqualTo(1)

        // Force refresh should bypass the cache
        val result3 = repo.getTrendingMovies(page = 1, forceRefresh = true)
        assertThat(result3.isSuccess).isTrue()
        assertThat(requestCount.get()).isEqualTo(2)
    }

    @Test
    fun `getMovieDetails parses detailed movie metadata correctly`() = runTest {
        val repo = createRepository { 200 to movieDetailJson }

        val result = repo.getMovieDetails(550L)
        assertThat(result.isSuccess).isTrue()
        val detail = result.getOrNull()
        assertThat(detail).isNotNull()
        assertThat(detail!!.title).isEqualTo("Fight Club")
        assertThat(detail.runtimeFormatted).isEqualTo("2h 19m")
        assertThat(detail.genres).hasSize(2)
        assertThat(detail.cast).hasSize(1)
        assertThat(detail.cast.first().name).isEqualTo("Brad Pitt")
    }

    @Test
    fun `searchMovies returns parsed results`() = runTest {
        val repo = createRepository { 200 to pagedMoviesJson }

        val result = repo.searchMovies("Fight Club", page = 1)
        assertThat(result.isSuccess).isTrue()
        val movies = result.getOrNull()
        assertThat(movies).hasSize(1)
        assertThat(movies!!.first().title).isEqualTo("Fight Club")
    }
}
