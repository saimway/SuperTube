package io.github.aedev.flow.data.shorts.feed

import io.github.aedev.flow.data.recommendation.SessionIntent

/**
 * How many of a page's [slots] each lane may take. SUBSCRIPTIONS is a cap, never a floor; the
 * remainder after rounding goes to EXPLORE, the lane that never runs dry.
 */
internal fun shortsFeedQuotas(
    slots: Int,
    profile: ShortsFeedProfile,
    sessionIntent: SessionIntent = profile.sessionIntent,
): Map<ShortsFeedLane, Int> {
    val total = slots.coerceAtLeast(0)
    if (total == 0) return ShortsFeedLane.entries.associateWith { 0 }

    val baseShares =
        when {
            profile.isColdStart && profile.hasSubscriptions -> {
                Shares(
                    related = 0.10,
                    explore = 0.40,
                    discovery = 0.20,
                    subscriptions = 0.30,
                )
            }

            profile.isColdStart -> {
                Shares(related = 0.15, explore = 0.55, discovery = 0.30, subscriptions = 0.0)
            }

            profile.hasSubscriptions -> {
                Shares(related = 0.35, explore = 0.30, discovery = 0.20, subscriptions = 0.15)
            }

            else -> {
                Shares(related = 0.40, explore = 0.35, discovery = 0.25, subscriptions = 0.0)
            }
        }

    var relatedShare = baseShares.related
    var exploreShare = baseShares.explore
    var discoveryShare = baseShares.discovery
    var subscriptionsShare = baseShares.subscriptions

    // Dynamic modulation via SessionIntent
    // 1. Fatigue Dial: consecutiveSkips >= 3
    if (sessionIntent.consecutiveSkips >= 3) {
        val fatigueShift =
            if (subscriptionsShare > 0.0) {
                minOf(0.15, subscriptionsShare)
            } else {
                minOf(0.15, exploreShare * 0.4)
            }
        if (subscriptionsShare > 0.0) {
            subscriptionsShare -= fatigueShift
        } else {
            exploreShare -= fatigueShift
        }
        discoveryShare += fatigueShift
    }

    // 2. Craving Adaptation: cravingIntensity > 0.5f elevates related up to 15%
    if (sessionIntent.activeCravingTopic != null && sessionIntent.cravingIntensity > 0.5f) {
        val cravingFactor = ((sessionIntent.cravingIntensity - 0.5f) / 0.5f).coerceIn(0.0f, 1.0f).toDouble()
        val targetBoost = 0.15 * cravingFactor
        val fromExplore = if (exploreShare > 0.15) minOf(targetBoost * 0.7, exploreShare - 0.15) else 0.0
        val fromSubs = if (subscriptionsShare > 0.05) minOf(targetBoost * 0.3, subscriptionsShare - 0.05) else 0.0
        val actualBoost = fromExplore + fromSubs
        exploreShare -= fromExplore
        subscriptionsShare -= fromSubs
        relatedShare += actualBoost
    }

    val related = (total * relatedShare).toInt()
    val discovery = (total * discoveryShare).toInt()
    val subscriptions = (total * subscriptionsShare).toInt()
    val explore = (total - related - discovery - subscriptions).coerceAtLeast(0)
    return mapOf(
        ShortsFeedLane.RELATED to related,
        ShortsFeedLane.EXPLORE to explore,
        ShortsFeedLane.DISCOVERY to discovery,
        ShortsFeedLane.SUBSCRIPTIONS to subscriptions,
    )
}

private data class Shares(
    val related: Double,
    val explore: Double,
    val discovery: Double,
    val subscriptions: Double,
)
