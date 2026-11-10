package io.github.aedev.flow.data.recommendation

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DismissedVideoStoreTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    private class InMemoryDataStore(
        initial: DismissedVideoIndex = DismissedVideoIndex(),
    ) : DataStore<DismissedVideoIndex> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<DismissedVideoIndex> = state

        override suspend fun updateData(
            transform: suspend (t: DismissedVideoIndex) -> DismissedVideoIndex,
        ): DismissedVideoIndex {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private fun sampleVideo(
        id: String = "v1",
        title: String = "Test Video 1",
        channelId: String = "UC123",
        channelName: String = "Test Channel",
        tags: List<String> = listOf("kotlin", "android"),
    ): Video =
        Video(
            id = id,
            title = title,
            channelName = channelName,
            channelId = channelId,
            thumbnailUrl = "https://thumb/$id.jpg",
            duration = 120,
            viewCount = 1000L,
            uploadDate = "2026-01-01",
            tags = tags,
        )

    @Test
    fun `record adds video to store with metadata`() =
        runTest {
            // Arrange
            val dataStore = InMemoryDataStore()
            val store = DismissedVideoStore(dataStore)
            val video = sampleVideo(id = "v_add", title = "New Video")

            // Act
            store.record(video, topics = listOf("science", "tech"))
            val all = store.all()

            // Assert
            assertThat(all).hasSize(1)
            val record = all.single()
            assertThat(record.videoId).isEqualTo("v_add")
            assertThat(record.title).isEqualTo("New Video")
            assertThat(record.channelId).isEqualTo("UC123")
            assertThat(record.channelName).isEqualTo("Test Channel")
            assertThat(record.thumbnailUrl).isEqualTo("https://thumb/v_add.jpg")
            assertThat(record.topics).containsExactly("science", "tech").inOrder()
            assertThat(record.dismissedAt).isGreaterThan(0L)
        }

    @Test
    fun `record falls back to video tags when topics are empty`() =
        runTest {
            // Arrange
            val dataStore = InMemoryDataStore()
            val store = DismissedVideoStore(dataStore)
            val video = sampleVideo(id = "v_tags", tags = listOf("space", "astronomy"))

            // Act
            store.record(video)
            val all = store.all()

            // Assert
            assertThat(all.single().topics).containsExactly("space", "astronomy").inOrder()
        }

    @Test
    fun `forget removes video by id`() =
        runTest {
            // Arrange
            val dataStore = InMemoryDataStore()
            val store = DismissedVideoStore(dataStore)
            store.record(sampleVideo(id = "v1"))
            store.record(sampleVideo(id = "v2"))
            assertThat(store.all()).hasSize(2)

            // Act
            store.forget("v1")
            val remaining = store.all()

            // Assert
            assertThat(remaining).hasSize(1)
            assertThat(remaining.single().videoId).isEqualTo("v2")
        }

    @Test
    fun `record caps at MAX_ENTRIES items LRU`() =
        runTest {
            // Arrange
            val dataStore = InMemoryDataStore()
            val store = DismissedVideoStore(dataStore)

            // Pre-fill 1000 items with staggered timestamps
            val initialRecords = (1..1000).associate { i ->
                val id = "id_$i"
                id to DismissedVideoRecord(
                    videoId = id,
                    title = "Title $i",
                    channelId = "c_$i",
                    channelName = "Ch $i",
                    thumbnailUrl = "",
                    dismissedAt = i.toLong(),
                )
            }
            dataStore.updateData { DismissedVideoIndex(initialRecords) }

            // Act: add one more (newest)
            val extraVideo = sampleVideo(id = "id_newest")
            store.record(extraVideo)

            val results = store.all()

            // Assert: exactly 1000 items kept, oldest (id_1) evicted, newest kept
            assertThat(results).hasSize(1000)
            assertThat(results.map { it.videoId }).contains("id_newest")
            assertThat(results.map { it.videoId }).doesNotContain("id_1")
        }

    @Test
    fun `observeAll emits records sorted by dismissedAt descending`() =
        runTest {
            // Arrange
            val dataStore = InMemoryDataStore()
            val store = DismissedVideoStore(dataStore)
            val older = DismissedVideoRecord("v_old", "Old", "c", "c", "", dismissedAt = 100L)
            val newer = DismissedVideoRecord("v_new", "New", "c", "c", "", dismissedAt = 500L)
            dataStore.updateData { DismissedVideoIndex(mapOf("v_old" to older, "v_new" to newer)) }

            // Act
            val observed = store.observeAll().first()

            // Assert
            assertThat(observed.map { it.videoId }).containsExactly("v_new", "v_old").inOrder()
        }

    @Test
    fun `clear removes all records`() =
        runTest {
            // Arrange
            val dataStore = InMemoryDataStore()
            val store = DismissedVideoStore(dataStore)
            store.record(sampleVideo(id = "v1"))
            store.record(sampleVideo(id = "v2"))
            assertThat(store.all()).isNotEmpty()

            // Act
            store.clear()

            // Assert
            assertThat(store.all()).isEmpty()
        }

    @Test
    fun `IndexSerializer roundtrips index accurately`() =
        runTest {
            // Arrange
            val record = DismissedVideoRecord(
                videoId = "vid_serial",
                title = "Serialized Title",
                channelId = "UC_serial",
                channelName = "Serial Channel",
                thumbnailUrl = "https://thumb/serial.jpg",
                dismissedAt = 123456789L,
                topics = listOf("serialization", "json"),
            )
            val index = DismissedVideoIndex(mapOf(record.videoId to record))

            // Act
            val output = ByteArrayOutputStream()
            DismissedVideoStore.IndexSerializer.writeTo(index, output)
            val input = ByteArrayInputStream(output.toByteArray())
            val deserialized = DismissedVideoStore.IndexSerializer.readFrom(input)

            // Assert
            assertThat(deserialized.records).hasSize(1)
            val decoded = deserialized.records.getValue("vid_serial")
            assertThat(decoded).isEqualTo(record)
        }

    @Test
    fun `IndexSerializer returns default on corrupted input`() =
        runTest {
            // Arrange
            val corruptInput = ByteArrayInputStream("{ malformed json }".toByteArray())

            // Act
            val result = DismissedVideoStore.IndexSerializer.readFrom(corruptInput)

            // Assert
            assertThat(result.records).isEmpty()
        }
}
