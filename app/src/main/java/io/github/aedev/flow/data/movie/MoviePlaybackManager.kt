package io.github.aedev.flow.data.movie

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

enum class MediaType {
    MOVIE,
    TV,
}

data class MoviePlaybackRecord(
    val movieId: Long,
    val serverId: String,
    val positionSeconds: Long,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedPosition: String
        get() {
            val m = positionSeconds / 60
            val s = positionSeconds % 60
            val h = m / 60
            return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s) else "%02d:%02d".format(m, s)
        }

    val progressFraction: Float
        get() = if (durationSeconds > 0) (positionSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f) else 0f
}

data class LastWatchedMedia(
    val mediaId: Long,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val episodeTitle: String = "",
    val serverId: String = MovieServer.DEFAULT_SERVER.id,
    val positionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedPosition: String
        get() {
            val m = positionSeconds / 60
            val s = positionSeconds % 60
            val h = m / 60
            return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s) else "%02d:%02d".format(m, s)
        }

    val progressFraction: Float
        get() = if (durationSeconds > 0) (positionSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f) else 0f
}

object MoviePlaybackManager {
    private const val PREFS_NAME = "supertube_movie_playback"
    private const val KEY_PREFIX_POS = "pos_"
    private const val KEY_PREFIX_DUR = "dur_"
    private const val KEY_PREFIX_SRV = "srv_"
    private const val KEY_PREFIX_TIME = "time_"

    // TV Episode Keys
    private const val KEY_PREFIX_TV_POS = "tv_pos_"
    private const val KEY_PREFIX_TV_DUR = "tv_dur_"
    private const val KEY_PREFIX_TV_SRV = "tv_srv_"
    private const val KEY_PREFIX_TV_TIME = "tv_time_"

    // Global Last Watched Keys
    private const val KEY_LAST_MEDIA_ID = "last_media_id"
    private const val KEY_LAST_MEDIA_TYPE = "last_media_type"
    private const val KEY_LAST_TITLE = "last_media_title"
    private const val KEY_LAST_POSTER = "last_media_poster"
    private const val KEY_LAST_BACKDROP = "last_media_backdrop"
    private const val KEY_LAST_SEASON = "last_media_season"
    private const val KEY_LAST_EPISODE = "last_media_episode"
    private const val KEY_LAST_EPISODE_TITLE = "last_media_ep_title"
    private const val KEY_LAST_SERVER = "last_media_server"
    private const val KEY_LAST_POS = "last_media_pos"
    private const val KEY_LAST_DUR = "last_media_dur"
    private const val KEY_LAST_TIME = "last_media_time"

    private val movieCache = ConcurrentHashMap<Long, MoviePlaybackRecord>()
    private val tvCache = ConcurrentHashMap<String, MoviePlaybackRecord>()
    @Volatile
    private var lastWatchedCache: LastWatchedMedia? = null
    private var prefs: SharedPreferences? = null

    private val _lastWatchedFlow = MutableStateFlow<LastWatchedMedia?>(null)
    val lastWatchedFlow: StateFlow<LastWatchedMedia?> = _lastWatchedFlow.asStateFlow()

    private fun tvKey(tvId: Long, season: Int, episode: Int): String = "${tvId}_s${season}_e$episode"

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _lastWatchedFlow.value = getLastWatched()
        }
    }

    // -------------------------------------------------------------------------
    // Global Last Watched
    // -------------------------------------------------------------------------

    fun saveLastWatched(record: LastWatchedMedia) {
        lastWatchedCache = record
        _lastWatchedFlow.value = record
        prefs?.edit {
            putLong(KEY_LAST_MEDIA_ID, record.mediaId)
            putString(KEY_LAST_MEDIA_TYPE, record.mediaType.name)
            putString(KEY_LAST_TITLE, record.title)
            putString(KEY_LAST_POSTER, record.posterUrl)
            putString(KEY_LAST_BACKDROP, record.backdropUrl)
            putInt(KEY_LAST_SEASON, record.seasonNumber)
            putInt(KEY_LAST_EPISODE, record.episodeNumber)
            putString(KEY_LAST_EPISODE_TITLE, record.episodeTitle)
            putString(KEY_LAST_SERVER, record.serverId)
            putLong(KEY_LAST_POS, record.positionSeconds)
            putLong(KEY_LAST_DUR, record.durationSeconds)
            putLong(KEY_LAST_TIME, record.timestamp)
        }
    }

    fun getLastWatched(): LastWatchedMedia? {
        lastWatchedCache?.let { return it }

        val p = prefs ?: return null
        if (!p.contains(KEY_LAST_MEDIA_ID)) return null

        val mediaId = p.getLong(KEY_LAST_MEDIA_ID, 0L)
        if (mediaId <= 0L) return null

        val typeStr = p.getString(KEY_LAST_MEDIA_TYPE, MediaType.MOVIE.name) ?: MediaType.MOVIE.name
        val mediaType = try {
            MediaType.valueOf(typeStr)
        } catch (_: Exception) {
            MediaType.MOVIE
        }

        val title = p.getString(KEY_LAST_TITLE, "") ?: ""
        val poster = p.getString(KEY_LAST_POSTER, null)
        val backdrop = p.getString(KEY_LAST_BACKDROP, null)
        val season = p.getInt(KEY_LAST_SEASON, 1)
        val episode = p.getInt(KEY_LAST_EPISODE, 1)
        val epTitle = p.getString(KEY_LAST_EPISODE_TITLE, "") ?: ""
        val server = p.getString(KEY_LAST_SERVER, MovieServer.DEFAULT_SERVER.id) ?: MovieServer.DEFAULT_SERVER.id
        val pos = p.getLong(KEY_LAST_POS, 0L)
        val dur = p.getLong(KEY_LAST_DUR, 0L)
        val time = p.getLong(KEY_LAST_TIME, 0L)

        val record = LastWatchedMedia(
            mediaId = mediaId,
            mediaType = mediaType,
            title = title,
            posterUrl = poster,
            backdropUrl = backdrop,
            seasonNumber = season,
            episodeNumber = episode,
            episodeTitle = epTitle,
            serverId = server,
            positionSeconds = pos,
            durationSeconds = dur,
            timestamp = time,
        )
        lastWatchedCache = record
        return record
    }

    fun clearLastWatched() {
        lastWatchedCache = null
        _lastWatchedFlow.value = null
        prefs?.edit {
            remove(KEY_LAST_MEDIA_ID)
            remove(KEY_LAST_MEDIA_TYPE)
            remove(KEY_LAST_TITLE)
            remove(KEY_LAST_POSTER)
            remove(KEY_LAST_BACKDROP)
            remove(KEY_LAST_SEASON)
            remove(KEY_LAST_EPISODE)
            remove(KEY_LAST_EPISODE_TITLE)
            remove(KEY_LAST_SERVER)
            remove(KEY_LAST_POS)
            remove(KEY_LAST_DUR)
            remove(KEY_LAST_TIME)
        }
    }

    // -------------------------------------------------------------------------
    // Movie Playback Progress
    // -------------------------------------------------------------------------

    fun saveProgress(
        movieId: Long,
        serverId: String,
        positionSeconds: Long,
        durationSeconds: Long,
        title: String = "",
        posterUrl: String? = null,
        backdropUrl: String? = null,
    ) {
        if (movieId <= 0L || positionSeconds < 0) return
        val record = MoviePlaybackRecord(
            movieId = movieId,
            serverId = serverId,
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds,
        )
        movieCache[movieId] = record

        prefs?.edit {
            putLong(KEY_PREFIX_POS + movieId, positionSeconds)
            putLong(KEY_PREFIX_DUR + movieId, durationSeconds)
            putString(KEY_PREFIX_SRV + movieId, serverId)
            putLong(KEY_PREFIX_TIME + movieId, record.timestamp)
        }

        // Update last watched record
        val existingLast = lastWatchedCache
        val effectiveTitle = if (title.isNotBlank()) {
            title
        } else if (existingLast?.mediaId == movieId && existingLast.mediaType == MediaType.MOVIE) {
            existingLast.title
        } else {
            ""
        }

        if (effectiveTitle.isNotBlank() || existingLast?.mediaId == movieId) {
            saveLastWatched(
                LastWatchedMedia(
                    mediaId = movieId,
                    mediaType = MediaType.MOVIE,
                    title = effectiveTitle,
                    posterUrl = posterUrl ?: existingLast?.posterUrl,
                    backdropUrl = backdropUrl ?: existingLast?.backdropUrl,
                    serverId = serverId,
                    positionSeconds = positionSeconds,
                    durationSeconds = durationSeconds,
                    timestamp = record.timestamp,
                ),
            )
        }
    }

    fun getProgress(movieId: Long): MoviePlaybackRecord? {
        if (movieId <= 0L) return null
        movieCache[movieId]?.let { return it }

        val p = prefs ?: return null
        val posKey = KEY_PREFIX_POS + movieId
        if (!p.contains(posKey)) return null

        val pos = p.getLong(posKey, 0L)
        val dur = p.getLong(KEY_PREFIX_DUR + movieId, 0L)
        val srv = p.getString(KEY_PREFIX_SRV + movieId, null) ?: MovieServer.DEFAULT_SERVER.id
        val time = p.getLong(KEY_PREFIX_TIME + movieId, 0L)

        val record = MoviePlaybackRecord(movieId, srv, pos, dur, time)
        movieCache[movieId] = record
        return record
    }

    fun getSavedServer(movieId: Long): MovieServer {
        val srvId = getProgress(movieId)?.serverId ?: prefs?.getString(KEY_PREFIX_SRV + movieId, null)
        return MovieServer.findById(srvId)
    }

    fun clearProgress(movieId: Long) {
        movieCache.remove(movieId)
        prefs?.edit {
            remove(KEY_PREFIX_POS + movieId)
            remove(KEY_PREFIX_DUR + movieId)
            remove(KEY_PREFIX_SRV + movieId)
            remove(KEY_PREFIX_TIME + movieId)
        }
        if (lastWatchedCache?.mediaId == movieId && lastWatchedCache?.mediaType == MediaType.MOVIE) {
            clearLastWatched()
        }
    }

    // -------------------------------------------------------------------------
    // TV Episode Playback Progress
    // -------------------------------------------------------------------------

    fun saveTvProgress(
        tvId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
        serverId: String,
        positionSeconds: Long,
        durationSeconds: Long,
        title: String = "",
        episodeTitle: String = "",
        posterUrl: String? = null,
        backdropUrl: String? = null,
    ) {
        if (tvId <= 0L || positionSeconds < 0) return
        val key = tvKey(tvId, seasonNumber, episodeNumber)
        val record = MoviePlaybackRecord(
            movieId = tvId,
            serverId = serverId,
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds,
        )
        tvCache[key] = record

        prefs?.edit {
            putLong(KEY_PREFIX_TV_POS + key, positionSeconds)
            putLong(KEY_PREFIX_TV_DUR + key, durationSeconds)
            putString(KEY_PREFIX_TV_SRV + key, serverId)
            putLong(KEY_PREFIX_TV_TIME + key, record.timestamp)
        }

        val existingLast = lastWatchedCache
        val effectiveTitle = if (title.isNotBlank()) {
            title
        } else if (existingLast?.mediaId == tvId && existingLast.mediaType == MediaType.TV) {
            existingLast.title
        } else {
            ""
        }

        saveLastWatched(
            LastWatchedMedia(
                mediaId = tvId,
                mediaType = MediaType.TV,
                title = effectiveTitle,
                posterUrl = posterUrl ?: existingLast?.posterUrl,
                backdropUrl = backdropUrl ?: existingLast?.backdropUrl,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                serverId = serverId,
                positionSeconds = positionSeconds,
                durationSeconds = durationSeconds,
                timestamp = record.timestamp,
            ),
        )
    }

    fun getTvProgress(tvId: Long, seasonNumber: Int, episodeNumber: Int): MoviePlaybackRecord? {
        if (tvId <= 0L) return null
        val key = tvKey(tvId, seasonNumber, episodeNumber)
        tvCache[key]?.let { return it }

        val p = prefs ?: return null
        val posKey = KEY_PREFIX_TV_POS + key
        if (!p.contains(posKey)) return null

        val pos = p.getLong(posKey, 0L)
        val dur = p.getLong(KEY_PREFIX_TV_DUR + key, 0L)
        val srv = p.getString(KEY_PREFIX_TV_SRV + key, null) ?: MovieServer.DEFAULT_SERVER.id
        val time = p.getLong(KEY_PREFIX_TV_TIME + key, 0L)

        val record = MoviePlaybackRecord(tvId, srv, pos, dur, time)
        tvCache[key] = record
        return record
    }

    fun getSavedTvServer(tvId: Long, seasonNumber: Int, episodeNumber: Int): MovieServer {
        val srvId = getTvProgress(tvId, seasonNumber, episodeNumber)?.serverId
            ?: prefs?.getString(KEY_PREFIX_TV_SRV + tvKey(tvId, seasonNumber, episodeNumber), null)
        return MovieServer.findById(srvId)
    }

    fun clearTvProgress(tvId: Long, seasonNumber: Int, episodeNumber: Int) {
        val key = tvKey(tvId, seasonNumber, episodeNumber)
        tvCache.remove(key)
        prefs?.edit {
            remove(KEY_PREFIX_TV_POS + key)
            remove(KEY_PREFIX_TV_DUR + key)
            remove(KEY_PREFIX_TV_SRV + key)
            remove(KEY_PREFIX_TV_TIME + key)
        }
        if (lastWatchedCache?.mediaId == tvId &&
            lastWatchedCache?.mediaType == MediaType.TV &&
            lastWatchedCache?.seasonNumber == seasonNumber &&
            lastWatchedCache?.episodeNumber == episodeNumber
        ) {
            clearLastWatched()
        }
    }
}
