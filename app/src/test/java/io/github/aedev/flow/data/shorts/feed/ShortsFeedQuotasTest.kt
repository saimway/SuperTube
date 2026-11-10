package io.github.aedev.flow.data.shorts.feed

import io.github.aedev.flow.data.recommendation.SessionIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsFeedQuotasTest {
    @Test
    fun `a mature account with subscriptions leans on related reels and caps subscriptions`() {
        val quotas = shortsFeedQuotas(16, ShortsFeedProfile(subscribedChannelIds = setOf("UCa"), isColdStart = false))

        assertEquals(5, quotas[ShortsFeedLane.RELATED])
        assertEquals(3, quotas[ShortsFeedLane.DISCOVERY])
        assertEquals(2, quotas[ShortsFeedLane.SUBSCRIPTIONS])
        assertEquals(6, quotas[ShortsFeedLane.EXPLORE])
        assertEquals(16, quotas.values.sum())
    }

    @Test
    fun `no subscriptions means no subscription slots`() {
        val quotas = shortsFeedQuotas(16, ShortsFeedProfile(subscribedChannelIds = emptySet(), isColdStart = false))

        assertEquals(0, quotas[ShortsFeedLane.SUBSCRIPTIONS])
        assertEquals(16, quotas.values.sum())
    }

    @Test
    fun `a cold start leans on the explore chain`() {
        val quotas = shortsFeedQuotas(16, ShortsFeedProfile(subscribedChannelIds = emptySet(), isColdStart = true))

        assertEquals(2, quotas[ShortsFeedLane.RELATED])
        assertEquals(4, quotas[ShortsFeedLane.DISCOVERY])
        assertEquals(10, quotas[ShortsFeedLane.EXPLORE])
        assertEquals(16, quotas.values.sum())
    }

    @Test
    fun `no slots means no quotas`() {
        assertEquals(0, shortsFeedQuotas(0, ShortsFeedProfile(setOf("UCa"), false)).values.sum())
    }

    @Test
    fun `shorts feed quotas fatigue dial drops subscriptions and boosts discovery`() {
        val profile =
            ShortsFeedProfile(
                subscribedChannelIds = setOf("UCa"),
                isColdStart = false,
                sessionIntent = SessionIntent(consecutiveSkips = 3),
            )
        val quotas = shortsFeedQuotas(16, profile)

        // Base shares: related 5, explore 6, discovery 3, subscriptions 2
        // Fatigued: subscriptions drops by 15% (to 0), discovery increases by 15% (to 5)
        assertEquals(0, quotas[ShortsFeedLane.SUBSCRIPTIONS])
        assertEquals(5, quotas[ShortsFeedLane.DISCOVERY])
        assertEquals(5, quotas[ShortsFeedLane.RELATED])
        assertEquals(16, quotas.values.sum())
    }

    @Test
    fun `shorts feed quotas craving dial elevates related lane`() {
        val profile =
            ShortsFeedProfile(
                subscribedChannelIds = setOf("UCa"),
                isColdStart = false,
                sessionIntent = SessionIntent(activeCravingTopic = "comedy", cravingIntensity = 0.9f),
            )
        val quotas = shortsFeedQuotas(16, profile)

        // RELATED elevated above base 5 slots
        assertTrue((quotas[ShortsFeedLane.RELATED] ?: 0) >= 6)
        assertEquals(16, quotas.values.sum())
    }

    @Test
    fun `shorts feed quotas with zero slots returns zeros regardless of intent`() {
        val profile =
            ShortsFeedProfile(
                subscribedChannelIds = setOf("UCa"),
                isColdStart = false,
                sessionIntent = SessionIntent(consecutiveSkips = 5, cravingIntensity = 1.0f),
            )
        assertEquals(0, shortsFeedQuotas(0, profile).values.sum())
    }
}
