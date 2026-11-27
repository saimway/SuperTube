package io.github.aedev.flow.data.music.model

import io.github.aedev.flow.data.recommendation.NeuroLanguages
import io.github.aedev.flow.utils.foldForSearch

private val Brackets = Regex("""\s*[(\[][^)\]]*[)\]]""")
private val Featuring = Regex("""\s+(feat\.?|ft\.?|featuring)\s.*$""")
private val NonWord = Regex("""[^\p{L}\p{N}]+""")
private val MultiSpace = Regex("""\s+""")
private val TrailingDashOrPipe = Regex("""\s+[-|]\s+([^|\n]+)$""")
private val FanEditRegex =
    Regex(
        """\b(slowed(?:\s*(?:&|and|\+)?\s*reverb)?|reverb(?:ed)?|sped\s*up|speed\s*up|""" +
            """nightcore|daycore|8d(?:\s*audio)?|16d(?:\s*audio)?|3d\s*audio|""" +
            """bass\s*boosted|chopped\s*(?:&|and)?\s*screwed|slowed\s*down|""" +
            """1\s*(?:hour|hr)|loop\s*ver(?:sion)?|tiktok\s*(?:ver(?:sion)?|mix))\b""",
    )
private val VariantTokenRegex =
    Regex(
        """\b(slowed|reverb|reverbed|sped\s*up|speed\s*up|nightcore|daycore|8d|16d|""" +
            """bass\s*boosted|chopped|screwed|lofi|lo\s*fi|remix|cover|acoustic|live|""" +
            """instrumental|karaoke|acapella|a\s*cappella|mashup|unplugged|orchestral|""" +
            """lyrics|lyric\s*video|official\s*(?:audio|video|music\s*video)|music\s*video|""" +
            """visualizer|radio\s*edit|extended|remaster(?:ed)?|clean|explicit|version)\b""",
    )
private val TrailingVariantPhraseRegex =
    Regex(
        """(?:\s+(?:slowed(?:\s+(?:and|&|\+)?\s*reverb)?|reverb(?:ed)?|sped\s*up|speed\s*up|""" +
            """nightcore|daycore|8d(?:\s*audio)?|16d(?:\s*audio)?|bass\s*boosted|""" +
            """lofi(?:\s*remix|\s*flip|\s*ver(?:sion)?)?|lo\s*fi(?:\s*remix|\s*flip|\s*ver(?:sion)?)?|""" +
            """remix|acoustic|karaoke|instrumental|acapella|mashup|lyrics(?:\s*video)?|""" +
            """lyric\s*video|official\s*(?:audio|video)|1\s*(?:hour|hr)))\s*$""",
    )
private const val TOPIC_SUFFIX = " - topic"

/** [title] without its bracketed parts or featured artists: "Levitating (feat. DaBaby)" is "Levitating". */
fun musicBaseTitle(title: String): String =
    title
        .replace(Brackets, "")
        .replace(Featuring, "")
        .trim()

/** An artist name compared without case, accents or YouTube's " - Topic" channel suffix. */
fun musicArtistKey(name: String): String = name.foldForSearch().removeSuffix(TOPIC_SUFFIX).trim()

/**
 * A song title as the same song is titled everywhere: "The Weeknd - Blinding Lights (Official
 * Video)" and "Blinding Lights" both become "blinding lights" when [artists] holds The Weeknd.
 */
fun musicTitleKey(
    title: String,
    artists: List<String> = emptyList(),
): String {
    var folded = musicBaseTitle(title.foldForSearch())
    for (artist in artists.map(::musicArtistKey)) {
        val prefix = "$artist - "
        if (artist.isNotEmpty() && folded.startsWith(prefix)) folded = folded.removePrefix(prefix)
    }
    return folded.replace(NonWord, " ").trim()
}

/**
 * True when [title] denotes an unrequested fan edit or audio modification (slowed, reverb,
 * sped up, nightcore, 8D audio, bass boosted, extended loop, etc.).
 */
fun musicIsFanEditVariant(title: String): Boolean = FanEditRegex.containsMatchIn(title.foldForSearch())

/**
 * True when [title] denotes either a fan edit or an alternate version/packaging (remix, lofi,
 * cover, live, acoustic, karaoke, instrumental, lyrics video, mashup, etc.).
 */
fun musicIsVariantVersion(title: String): Boolean {
    val folded = title.foldForSearch().replace('-', ' ')
    return FanEditRegex.containsMatchIn(folded) || VariantTokenRegex.containsMatchIn(folded)
}

/**
 * Canonical song identity key that strips bracketed editions, artist prefixes, trailing
 * ` - <variant>` / ` | <variant>` tags, and unbracketed variant phrases such as
 * `"Blinding Lights Slowed and Reverb"` or `"Levitating Sped Up"`.
 */
fun musicCanonicalTitleKey(
    title: String,
    artists: List<String> = emptyList(),
): String {
    var folded = musicBaseTitle(title.foldForSearch())
    for (artist in artists.map(::musicArtistKey)) {
        val prefix = "$artist - "
        if (artist.isNotEmpty() && folded.startsWith(prefix)) {
            folded = folded.removePrefix(prefix)
        }
    }
    var trimmed = folded.trim()
    while (true) {
        val match = TrailingDashOrPipe.find(trimmed) ?: break
        val suffix = match.groupValues[1].replace('-', ' ')
        if (VariantTokenRegex.containsMatchIn(suffix) || FanEditRegex.containsMatchIn(suffix)) {
            trimmed = trimmed.substring(0, match.range.first).trim()
        } else {
            break
        }
    }
    var normalized = trimmed.replace(NonWord, " ").replace(MultiSpace, " ").trim()
    while (true) {
        val stripped = normalized.replace(TrailingVariantPhraseRegex, "").trim()
        if (stripped.isEmpty() || stripped == normalized) break
        normalized = stripped
    }
    return normalized
}

/**
 * Language detection wrapper accessible outside `data/recommendation/music` so the music
 * recommendation package satisfies `MusicBrainLeakGuardTest`.
 */
fun musicDetectLanguage(
    title: String,
    artist: String = "",
): String? = NeuroLanguages.detectContentLanguage(title, artist)

private fun MusicTrack.allArtistNames(): List<String> =
    (artists.map { it.name } + artist)
        .filter { it.isNotBlank() }
        .distinct()

private fun musicArtistsOverlap(
    a: MusicTrack,
    b: MusicTrack,
): Boolean {
    val keysA = a.allArtistNames().map(::musicArtistKey).filter { it.isNotEmpty() }
    val keysB = b.allArtistNames().map(::musicArtistKey).filter { it.isNotEmpty() }
    if (keysA.isEmpty() || keysB.isEmpty()) return false
    return keysA.any { ka -> keysB.any { kb -> ka == kb || ka in kb || kb in ka } }
}

private fun musicTitleMentionsAnyArtist(
    title: String,
    artistNames: List<String>,
): Boolean {
    val foldedTitle = title.foldForSearch()
    return artistNames
        .map(::musicArtistKey)
        .any { key -> key.length >= 3 && (foldedTitle.startsWith("$key ") || foldedTitle.startsWith("$key-") || "$key - " in foldedTitle) }
}

/**
 * Returns true if [candidate] represents the same song as `this`, or a slowed/reverb/sped-up/
 * remix/cover/live/lyrics variant of `this`. Preserves distinct songs that happen to share a
 * short title across different artists when neither is a variant or references the other artist.
 */
fun MusicTrack.isSameSongOrVariant(candidate: MusicTrack): Boolean {
    if (videoId == candidate.videoId) return true
    val combinedArtists = (allArtistNames() + candidate.allArtistNames()).distinct()
    val seedKey = musicCanonicalTitleKey(title, combinedArtists)
    val candKey = musicCanonicalTitleKey(candidate.title, combinedArtists)
    if (seedKey.isEmpty() || candKey.isEmpty()) return false

    val seedFoldedAfterDash =
        candidate.title.foldForSearch().substringAfter(" - ", "").let {
            if (it.isNotEmpty()) musicCanonicalTitleKey(it, combinedArtists) else ""
        }
    val candFoldedAfterDash =
        title.foldForSearch().substringAfter(" - ", "").let {
            if (it.isNotEmpty()) musicCanonicalTitleKey(it, combinedArtists) else ""
        }

    val sameBaseSong =
        seedKey == candKey ||
            (seedFoldedAfterDash.isNotEmpty() && seedFoldedAfterDash == seedKey) ||
            (candFoldedAfterDash.isNotEmpty() && candFoldedAfterDash == candKey)
    if (!sameBaseSong) return false

    if (musicArtistsOverlap(this, candidate)) return true
    if (musicTitleMentionsAnyArtist(candidate.title, allArtistNames())) return true
    if (musicTitleMentionsAnyArtist(title, candidate.allArtistNames())) return true
    if (musicIsFanEditVariant(title) || musicIsFanEditVariant(candidate.title)) return true
    return musicIsVariantVersion(title) || musicIsVariantVersion(candidate.title)
}

/**
 * Deduplicates a list of tracks by canonical song identity (including slowed/reverb/sped-up/remix
 * variants), preferring original non-fan-edit tracks over fan-edited uploads when both exist.
 */
fun List<MusicTrack>.dedupeCanonicalSongs(preferCleanOriginal: Boolean = true): List<MusicTrack> {
    if (size <= 1) return this
    val kept = ArrayList<MusicTrack>(size)
    for (track in this) {
        val existingIdx = kept.indexOfFirst { it.isSameSongOrVariant(track) }
        if (existingIdx < 0) {
            kept += track
        } else if (preferCleanOriginal) {
            val existing = kept[existingIdx]
            val existingFanEdit = musicIsFanEditVariant(existing.title)
            val incomingFanEdit = musicIsFanEditVariant(track.title)
            if (existingFanEdit && !incomingFanEdit) {
                kept[existingIdx] = track
            } else if (!existingFanEdit && !incomingFanEdit &&
                musicIsVariantVersion(existing.title) && !musicIsVariantVersion(track.title)
            ) {
                kept[existingIdx] = track
            }
        }
    }
    return kept
}
