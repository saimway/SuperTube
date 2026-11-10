package io.github.aedev.flow.data.recommendation

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowNeuroEngineUnsuppressTest {

    private val now = 1000000L
    private val persisted =
        UserBrain(
            totalInteractions = 10,
            suppressedVideoIds = mapOf(
                "video_suppressed_1" to now,
                "video_suppressed_2" to now + 100,
            ),
        )
    private val saved = mutableListOf<UserBrain>()
    private val storage: NeuroStorage =
        mockk(relaxed = true) {
            coEvery { load() } returns persisted
            coEvery { save(any()) } answers {
                saved += firstArg<UserBrain>()
                Unit
            }
        }
    private lateinit var engine: FlowNeuroEngine

    @Before
    fun setUp() {
        saved.clear()
        engine = FlowNeuroEngine(mockk(relaxed = true), storage, mockk<NeuroContentStore>(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `unsuppressVideo removes video from suppressedVideoIds and saves brain`() =
        runTest {
            // Arrange
            val exclusionsBefore = engine.feedExclusions()
            assertThat(exclusionsBefore.suppressedVideoIds).contains("video_suppressed_1")

            // Act
            engine.unsuppressVideo("video_suppressed_1")

            // Assert
            assertThat(saved).isNotEmpty()
            val lastSaved = saved.last()
            assertThat(lastSaved.suppressedVideoIds).doesNotContainKey("video_suppressed_1")
            assertThat(lastSaved.suppressedVideoIds).containsKey("video_suppressed_2")

            val exclusionsAfter = engine.feedExclusions()
            assertThat(exclusionsAfter.suppressedVideoIds).doesNotContain("video_suppressed_1")
            assertThat(exclusionsAfter.suppressedVideoIds).contains("video_suppressed_2")
        }

    @Test
    fun `unsuppressVideo is a no-op if videoId was not suppressed`() =
        runTest {
            // Act
            engine.unsuppressVideo("video_never_suppressed")

            // Assert: save is not triggered when videoId is absent
            assertThat(saved).isEmpty()
            val exclusions = engine.feedExclusions()
            assertThat(exclusions.suppressedVideoIds).contains("video_suppressed_1")
            assertThat(exclusions.suppressedVideoIds).contains("video_suppressed_2")
        }

    @Test
    fun `unsuppressVideo with blank videoId is ignored`() =
        runTest {
            // Act
            engine.unsuppressVideo("")
            engine.unsuppressVideo("   ")

            // Assert
            assertThat(saved).isEmpty()
        }

    @Test
    fun `getSessionIntent derives fatigue and craving correctly`() =
        runTest {
            // Arrange: default intent with no topics
            val initialIntent = engine.getSessionIntent()
            assertThat(initialIntent.consecutiveSkips).isEqualTo(0)
            assertThat(initialIntent.activeCravingTopic).isNull()
            assertThat(initialIntent.cravingIntensity).isEqualTo(0.0f)
            assertThat(initialIntent.discoveryBias).isEqualTo(0.0f)

            // Note session topics
            engine.noteSessionTopics(listOf("science", "science", "gaming"))
            val cravingIntent = engine.getSessionIntent()
            assertThat(cravingIntent.activeCravingTopic).isEqualTo("science")
            assertThat(cravingIntent.cravingIntensity).isGreaterThan(0.5f)
        }
}
