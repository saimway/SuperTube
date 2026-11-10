/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.data.recommendation

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.recommendation.eval.NeuroEval.video
import org.junit.Test

class NeuroCravingScoringTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `calculateCravingBoost is zero when activeCravingTopic is null or cravingIntensity is zero`() {
        val vector = ContentVector(topics = mapOf("science" to 0.8))
        val noTopicIntent = SessionIntent(activeCravingTopic = null, cravingIntensity = 0.8f)
        val zeroIntensityIntent = SessionIntent(activeCravingTopic = "science", cravingIntensity = 0.0f)

        assertThat(NeuroScoring.calculateCravingBoost(vector, noTopicIntent)).isEqualTo(0.0)
        assertThat(NeuroScoring.calculateCravingBoost(vector, zeroIntensityIntent)).isEqualTo(0.0)
    }

    @Test
    fun `calculateCravingBoost scales with candidate topic match and cravingIntensity`() {
        val vector = ContentVector(topics = mapOf("science" to 0.8))
        val midIntent = SessionIntent(activeCravingTopic = "science", cravingIntensity = 0.5f)
        val maxIntent = SessionIntent(activeCravingTopic = "science", cravingIntensity = 1.0f)

        val midBoost = NeuroScoring.calculateCravingBoost(vector, midIntent)
        val maxBoost = NeuroScoring.calculateCravingBoost(vector, maxIntent)

        assertThat(midBoost).isGreaterThan(0.0)
        assertThat(maxBoost).isGreaterThan(midBoost)
        assertThat(maxBoost).isAtMost(NeuroScoring.CRAVING_BOOST_MAX)
    }

    @Test
    fun `calculateCravingBoost is suppressed when video matches active rejectionPatterns`() {
        val vector = ContentVector(topics = mapOf("gaming" to 0.9))
        val intent = SessionIntent(activeCravingTopic = "gaming", cravingIntensity = 1.0f)
        val rejections = mapOf("gaming" to RejectionSignal(count = 5, lastRejectedAt = now))

        val boost = NeuroScoring.calculateCravingBoost(vector, intent, rejections, now)
        assertThat(boost).isEqualTo(0.0)
    }

    @Test
    fun `calculateFreshness applies relaxed decay rate for activeCravingTopic`() {
        val v = video("c1", title = "science video")
        val vector = ContentVector(topics = mapOf("science" to 0.9))
        val sessionTopics = listOf("science", "science", "science", "science") // 4 repeats

        val normalFreshness =
            NeuroScoring.calculateFreshness(
                video = v,
                videoVector = vector,
                personalityScore = 0.5,
                sessionTopics = sessionTopics,
                sessionVideoCount = 4,
                impressionEntry = null,
                now = now,
                sessionIntent = SessionIntent(),
            )

        val cravingFreshness =
            NeuroScoring.calculateFreshness(
                video = v,
                videoVector = vector,
                personalityScore = 0.5,
                sessionTopics = sessionTopics,
                sessionVideoCount = 4,
                impressionEntry = null,
                now = now,
                sessionIntent = SessionIntent(activeCravingTopic = "science", cravingIntensity = 1.0f),
            )

        assertThat(cravingFreshness).isGreaterThan(normalFreshness)
    }

    @Test
    fun `hasActiveTopicRejection correctly detects rejected topics`() {
        val rejections = mapOf("phonk" to RejectionSignal(count = 2, lastRejectedAt = now))

        assertThat(NeuroScoring.hasActiveTopicRejection("phonk", rejections, now)).isTrue()
        assertThat(NeuroScoring.hasActiveTopicRejection("phonk mix", rejections, now)).isTrue()
        assertThat(NeuroScoring.hasActiveTopicRejection("classical music", rejections, now)).isFalse()
    }
}
