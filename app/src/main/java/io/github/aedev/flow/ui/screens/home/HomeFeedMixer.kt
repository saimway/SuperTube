package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.recommendation.SessionIntent
import kotlinx.coroutines.flow.map

internal enum class FeedSource {
    SUBS,
    RELATED,
    DISCOVERY,

    /** New uploads from channels the viewer keeps watching without subscribing; see ChannelMemory. */
    CHANNEL_MEMORY,

    /** Uploads of channels several of the viewer's channels sit next to; see NeighbourChannels (#907). */
    NEIGHBOURS,
}

internal data class FeedCandidate(
    val video: Video,
    val source: FeedSource,
)

internal data class FeedMixResult(
    val items: List<FeedCandidate>,
    val sourceCounts: Map<FeedSource, Int>,
) {
    val videos: List<Video> get() = items.map { it.video }
}

internal fun homeFeedQuotas(
    remaining: Int,
    subCount: Int,
    totalInteractions: Int,
    hasNeighbours: Boolean = false,
    sessionIntent: SessionIntent = SessionIntent(),
): Map<FeedSource, Int> {
    val slots = remaining.coerceAtLeast(0)
    if (slots == 0) {
        return FeedSource.entries.associateWith { 0 }
    }

    var subsShare =
        when {
            subCount <= 0 -> 0.0
            totalInteractions > 50 -> 0.40
            else -> 0.35
        }
    var relatedShare =
        when {
            subCount <= 0 -> 0.35
            totalInteractions > 50 -> 0.25
            else -> 0.30
        }
    var discoveryShare =
        when {
            subCount <= 0 -> 0.45
            else -> 0.25
        }
    var remainderShare = (1.0 - subsShare - relatedShare - discoveryShare).coerceAtLeast(0.0)

    // Dynamic modulation via SessionIntent:
    // 1. Fatigue Dial: consecutiveSkips >= 3 shifts up to 15% from subs to discovery
    if (sessionIntent.consecutiveSkips >= 3) {
        val fatigueShift = if (subsShare > 0.0) minOf(0.15, subsShare) else minOf(0.15, relatedShare * 0.4)
        if (subsShare > 0.0) {
            subsShare -= fatigueShift
        } else {
            relatedShare -= fatigueShift
        }
        discoveryShare += fatigueShift
    }

    // 2. Craving Adaptation: cravingIntensity > 0.5f elevates related up to 15%
    if (sessionIntent.activeCravingTopic != null && sessionIntent.cravingIntensity > 0.5f) {
        val cravingFactor = ((sessionIntent.cravingIntensity - 0.5f) / 0.5f).coerceIn(0.0f, 1.0f).toDouble()
        val targetBoost = 0.15 * cravingFactor
        val fromSubs = if (subsShare > 0.10) minOf(targetBoost * 0.7, subsShare - 0.10) else 0.0
        val fromRemainder = if (remainderShare > 0.04) minOf(targetBoost * 0.3, remainderShare - 0.04) else 0.0
        val actualBoost = fromSubs + fromRemainder
        subsShare -= fromSubs
        remainderShare -= fromRemainder
        relatedShare += actualBoost
    }

    val subs = (slots * subsShare).toInt().coerceAtLeast(0)
    val related = (slots * relatedShare).toInt().coerceAtLeast(0)
    val discovery = (slots * discoveryShare).toInt().coerceAtLeast(0)
    val remainder = (slots - subs - related - discovery).coerceAtLeast(0)
    val neighbours = if (hasNeighbours && totalInteractions > 50) minOf(remainder / 2, (slots * 0.10).toInt()) else 0

    return mapOf(
        FeedSource.SUBS to subs,
        FeedSource.RELATED to related,
        FeedSource.DISCOVERY to discovery,
        FeedSource.CHANNEL_MEMORY to remainder - neighbours,
        FeedSource.NEIGHBOURS to neighbours,
    )
}

internal fun addUniqueVideo(
    video: Video?,
    targetList: MutableList<Video>,
    channelCounts: MutableMap<String, Int>,
    usedVideoIds: MutableSet<String>,
    maxPerChannel: Int = 2,
): Boolean {
    if (video == null) return false

    val hasChannel = video.channelId.isNotBlank()
    val count = channelCounts[video.channelId] ?: 0
    if (hasChannel && count >= maxPerChannel) return false
    if (!usedVideoIds.add(video.id)) return false
    targetList.add(video)
    if (hasChannel) channelCounts[video.channelId] = count + 1
    return true
}

internal fun addUniquePageVideos(
    candidates: Iterable<Video>,
    targetList: MutableList<Video>,
    channelCounts: MutableMap<String, Int>,
    usedVideoIds: MutableSet<String>,
    targetSize: Int,
    maxPerChannel: Int = 1,
): Int {
    var added = 0
    for (candidate in candidates) {
        if (targetList.size >= targetSize) break
        if (addUniqueVideo(candidate, targetList, channelCounts, usedVideoIds, maxPerChannel)) {
            added++
        }
    }
    return added
}

private fun addUniqueCandidate(
    candidate: FeedCandidate?,
    targetList: MutableList<FeedCandidate>,
    channelCounts: MutableMap<String, Int>,
    usedVideoIds: MutableSet<String>,
    maxPerChannel: Int = 2,
): Boolean {
    if (candidate == null) return false
    val temp = mutableListOf<Video>()
    if (!addUniqueVideo(candidate.video, temp, channelCounts, usedVideoIds, maxPerChannel)) return false
    targetList.add(candidate)
    return true
}

/**
 * Round-robin over the lanes up to each quota, then lanes with leftovers fill what the others could
 * not. [CHANNEL_MEMORY] and [NEIGHBOURS] never take more than their quotas, and a channel in
 * [singleChannels] (one those lanes serve) appears at most once.
 */
internal fun blendFeedSources(
    lanes: Map<FeedSource, List<Video>>,
    quotas: Map<FeedSource, Int>,
    targetSize: Int,
    channelCounts: MutableMap<String, Int> = mutableMapOf(),
    usedVideoIds: MutableSet<String> = mutableSetOf(),
    singleChannels: Set<String> = emptySet(),
): FeedMixResult {
    val target = targetSize.coerceAtLeast(0)
    if (target == 0) return FeedMixResult(emptyList(), emptyMap())

    val queues =
        FeedSource.entries.associateWith { source ->
            java.util.ArrayDeque(lanes[source].orEmpty().map { FeedCandidate(it, source) })
        }
    val quotaOrder = listOf(FeedSource.SUBS, FeedSource.RELATED, FeedSource.DISCOVERY, FeedSource.CHANNEL_MEMORY, FeedSource.NEIGHBOURS)
    val scarcityOrder = listOf(FeedSource.RELATED, FeedSource.DISCOVERY, FeedSource.SUBS)
    val addedBySource = mutableMapOf<FeedSource, Int>()
    val out = mutableListOf<FeedCandidate>()

    // One video per channel first; a second only fills what the lanes could not, so it lands at the end.
    var perChannel = 1
    val skipped = FeedSource.entries.associateWith { java.util.ArrayDeque<FeedCandidate>() }

    fun take(candidate: FeedCandidate?): Boolean {
        val cap = if (candidate != null && candidate.video.channelId in singleChannels) 1 else perChannel
        return addUniqueCandidate(candidate, out, channelCounts, usedVideoIds, cap).also { added ->
            if (added) {
                addedBySource[candidate!!.source] = (addedBySource[candidate.source] ?: 0) + 1
            } else if (candidate != null && perChannel == 1 && candidate.video.id !in usedVideoIds) {
                skipped.getValue(candidate.source).addLast(candidate)
            }
        }
    }

    fill(target, queues, out, quotaOrder, scarcityOrder, addedBySource, quotas, ::take)
    if (out.size < target && skipped.values.any { it.isNotEmpty() }) {
        perChannel = 2
        fill(target, skipped, out, quotaOrder, scarcityOrder, addedBySource, quotas, ::take)
    }

    return FeedMixResult(
        items = out,
        sourceCounts = FeedSource.entries.associateWith { source -> addedBySource[source] ?: 0 },
    )
}

private fun fill(
    target: Int,
    queues: Map<FeedSource, java.util.ArrayDeque<FeedCandidate>>,
    out: List<FeedCandidate>,
    quotaOrder: List<FeedSource>,
    scarcityOrder: List<FeedSource>,
    addedBySource: Map<FeedSource, Int>,
    quotas: Map<FeedSource, Int>,
    take: (FeedCandidate?) -> Boolean,
) {
    while (out.size < target && queues.any { it.value.isNotEmpty() }) {
        var addedThisRound = false
        for (source in quotaOrder) {
            if (out.size >= target) break
            if ((addedBySource[source] ?: 0) < (quotas[source] ?: 0) && take(queues[source]?.pollFirst())) {
                addedThisRound = true
            }
        }

        if (!addedThisRound) {
            // A rejected head (duplicate or capped channel) must not end the refill while the lane
            // still holds usable videos behind it.
            val forced =
                scarcityOrder.any { source ->
                    val queue = queues.getValue(source)
                    var added = false
                    while (!added && queue.isNotEmpty()) added = take(queue.pollFirst())
                    added
                }
            if (!forced) break
        }
    }
}
