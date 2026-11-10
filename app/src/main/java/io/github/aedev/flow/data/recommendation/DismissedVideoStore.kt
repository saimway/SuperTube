package io.github.aedev.flow.data.recommendation

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.profile.ProfileFiles
import io.github.aedev.flow.data.profile.StoreScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DismissedVideoRecord(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String,
    val dismissedAt: Long,
    val topics: List<String> = emptyList(),
)

@Serializable
data class DismissedVideoIndex(
    val records: Map<String, DismissedVideoRecord> = emptyMap(),
)

/**
 * Persistent store for videos marked "Not interested".
 * Preserves dismissal metadata (title, channel, thumbnail, timestamp, topics) for UI inspection,
 * undo capabilities, and persistent suppression, capped at 1,000 entries LRU.
 */
@Singleton
class DismissedVideoStore(
    private val dataStoreProvider: () -> DataStore<DismissedVideoIndex>,
) {
    @Inject
    constructor(
        @ApplicationContext appContext: Context,
    ) : this({
        DataStoreFactory.create(
            serializer = IndexSerializer,
            produceFile = {
                ProfileFiles.dataStore(
                    appContext,
                    StoreScope.PROFILE_PRIVATE,
                    "flow_dismissed_videos_v1.json",
                )
            },
        )
    })

    internal constructor(dataStore: DataStore<DismissedVideoIndex>) : this({ dataStore })

    companion object {
        private const val TAG = "DismissedVideoStore"
        const val MAX_ENTRIES = 1000
    }

    object IndexSerializer : Serializer<DismissedVideoIndex> {
        override val defaultValue: DismissedVideoIndex = DismissedVideoIndex()

        override suspend fun readFrom(input: InputStream): DismissedVideoIndex =
            try {
                val text = input.bufferedReader().readText()
                if (text.isBlank()) defaultValue else Json { ignoreUnknownKeys = true }.decodeFromString<DismissedVideoIndex>(text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read dismissed videos", e)
                defaultValue
            }

        override suspend fun writeTo(
            t: DismissedVideoIndex,
            output: OutputStream,
        ) {
            withContext(Dispatchers.IO) {
                output.write(Json.encodeToString(t).toByteArray())
            }
        }
    }

    private val dismissedDataStore: DataStore<DismissedVideoIndex> by lazy {
        dataStoreProvider()
    }

    /**
     * Records a video as dismissed. Drops older entries when size exceeds [MAX_ENTRIES] (LRU).
     */
    suspend fun record(
        video: Video,
        topics: List<String> = emptyList(),
    ) {
        if (video.id.isBlank()) return
        val now = System.currentTimeMillis()
        val record =
            DismissedVideoRecord(
                videoId = video.id,
                title = video.title,
                channelId = video.channelId,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                dismissedAt = now,
                topics = topics.ifEmpty { video.tags },
            )
        update { current ->
            val updated = current + (record.videoId to record)
            if (updated.size <= MAX_ENTRIES) {
                updated
            } else {
                updated.values
                    .sortedByDescending { it.dismissedAt }
                    .take(MAX_ENTRIES)
                    .associateBy { it.videoId }
            }
        }
    }

    /**
     * Forgets a previously dismissed video by id (e.g. on Undo or unsuppress).
     */
    suspend fun forget(videoId: String) {
        if (videoId.isBlank()) return
        update { it - videoId }
    }

    /**
     * Observes all dismissed videos, ordered most-recent first.
     */
    fun observeAll(): Flow<List<DismissedVideoRecord>> =
        dismissedDataStore.data
            .catch { e ->
                Log.e(TAG, "Error observing dismissed videos", e)
                emit(DismissedVideoIndex())
            }
            .map { index ->
                index.records.values.sortedByDescending { it.dismissedAt }
            }

    /**
     * Returns a snapshot of all dismissed videos, ordered most-recent first.
     */
    suspend fun all(): List<DismissedVideoRecord> =
        withContext(Dispatchers.IO) {
            runCatching {
                dismissedDataStore.data
                    .first()
                    .records
                    .values
                    .sortedByDescending { it.dismissedAt }
            }.getOrDefault(emptyList())
        }

    /**
     * Clears all dismissed video records.
     */
    suspend fun clear() {
        update { emptyMap() }
    }

    private suspend fun update(transform: (Map<String, DismissedVideoRecord>) -> Map<String, DismissedVideoRecord>) =
        withContext(Dispatchers.IO) {
            runCatching {
                dismissedDataStore.updateData { index ->
                    DismissedVideoIndex(transform(index.records))
                }
            }.onFailure { Log.e(TAG, "Failed to update dismissed videos", it) }
        }
}
