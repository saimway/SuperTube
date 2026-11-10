/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.data.recommendation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NeuroDiscoveryRejectionFilteringTest {
    private val discovery = NeuroDiscovery(NeuroTopicCatalog.TOPIC_CATEGORIES, NeuroTokenizer())
    private val now = 1_700_000_000_000L

    @Test
    fun `generateQueries drops queries matching active rejection patterns`() {
        val brain =
            UserBrain(
                globalVector = ContentVector(topics = mapOf("minecraft" to 0.8, "cooking" to 0.7)),
                preferredTopics = setOf("minecraft", "cooking"),
                rejectionPatterns = mapOf("cooking" to RejectionSignal(count = 3, lastRejectedAt = now)),
            )

        val queries = discovery.generateQueries(brain, now = now) { FlowPersona.EXPLORER }
        val queryTexts = queries.map { it.query.lowercase() }

        assertThat(queryTexts.none { it.contains("cooking") }).isTrue()
        assertThat(queryTexts.any { it.contains("minecraft") }).isTrue()
    }

    @Test
    fun `generateQueries places craving topic at index 0 when cravingIntensity is high`() {
        val brain =
            UserBrain(
                globalVector = ContentVector(topics = mapOf("minecraft" to 0.8)),
                preferredTopics = setOf("minecraft"),
            )
        val intent =
            SessionIntent(
                activeCravingTopic = "science",
                cravingIntensity = 0.9f,
            )

        val queries = discovery.generateQueries(brain, now = now, intent = intent) { FlowPersona.EXPLORER }
        assertThat(queries).isNotEmpty()
        assertThat(queries.first().query.lowercase()).contains("science")
        assertThat(queries.first().strategy).isEqualTo(QueryStrategy.DEEP_DIVE)
    }

    @Test
    fun `generateQueries does NOT inject craving topic if it is actively rejected`() {
        val brain =
            UserBrain(
                globalVector = ContentVector(topics = mapOf("minecraft" to 0.8)),
                preferredTopics = setOf("minecraft"),
                rejectionPatterns = mapOf("science" to RejectionSignal(count = 3, lastRejectedAt = now)),
            )
        val intent =
            SessionIntent(
                activeCravingTopic = "science",
                cravingIntensity = 0.9f,
            )

        val queries = discovery.generateQueries(brain, now = now, intent = intent) { FlowPersona.EXPLORER }
        val queryTexts = queries.map { it.query.lowercase() }
        assertThat(queryTexts.none { it.contains("science") }).isTrue()
    }
}
