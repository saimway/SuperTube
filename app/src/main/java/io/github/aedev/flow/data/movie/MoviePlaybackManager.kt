package io.github.aedev.flow.data.movie

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap

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
}

object MoviePlaybackManager {
    private const val PREFS_NAME = "supertube_movie_playback"
    private const val KEY_PREFIX_POS = "pos_"
    private const val KEY_PREFIX_DUR = "dur_"
    private const val KEY_PREFIX_SRV = "srv_"
    private const val KEY_PREFIX_TIME = "time_"

    private val cache = ConcurrentHashMap<Long, MoviePlaybackRecord>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveProgress(
        movieId: Long,
        serverId: String,
        positionSeconds: Long,
        durationSeconds: Long,
    ) {
        if (movieId <= 0L || positionSeconds < 0) return
        val record = MoviePlaybackRecord(
            movieId = movieId,
            serverId = serverId,
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds,
        )
        cache[movieId] = record

        prefs?.edit {
            putLong(KEY_PREFIX_POS + movieId, positionSeconds)
            putLong(KEY_PREFIX_DUR + movieId, durationSeconds)
            putString(KEY_PREFIX_SRV + movieId, serverId)
            putLong(KEY_PREFIX_TIME + movieId, record.timestamp)
        }
    }

    fun getProgress(movieId: Long): MoviePlaybackRecord? {
        if (movieId <= 0L) return null
        cache[movieId]?.let { return it }

        val p = prefs ?: return null
        val posKey = KEY_PREFIX_POS + movieId
        if (!p.contains(posKey)) return null

        val pos = p.getLong(posKey, 0L)
        val dur = p.getLong(KEY_PREFIX_DUR + movieId, 0L)
        val srv = p.getString(KEY_PREFIX_SRV + movieId, null) ?: MovieServer.DEFAULT_SERVER.id
        val time = p.getLong(KEY_PREFIX_TIME + movieId, 0L)

        val record = MoviePlaybackRecord(movieId, srv, pos, dur, time)
        cache[movieId] = record
        return record
    }

    fun getSavedServer(movieId: Long): MovieServer {
        val srvId = getProgress(movieId)?.serverId ?: prefs?.getString(KEY_PREFIX_SRV + movieId, null)
        return MovieServer.findById(srvId)
    }

    fun clearProgress(movieId: Long) {
        cache.remove(movieId)
        prefs?.edit {
            remove(KEY_PREFIX_POS + movieId)
            remove(KEY_PREFIX_DUR + movieId)
            remove(KEY_PREFIX_SRV + movieId)
            remove(KEY_PREFIX_TIME + movieId)
        }
    }
}
