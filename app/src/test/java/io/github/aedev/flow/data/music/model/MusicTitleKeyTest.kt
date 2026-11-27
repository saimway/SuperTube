package io.github.aedev.flow.data.music.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MusicTitleKeyTest {
    @Test
    fun `titles match across case, accents, brackets, features and the artist prefix`() {
        assertThat(musicTitleKey("The Weeknd - BLINDING LIGHTS [4K]", listOf("The Weeknd"))).isEqualTo("blinding lights")
        assertThat(musicTitleKey("Levitating (feat. DaBaby)")).isEqualTo("levitating")
        assertThat(musicTitleKey("Anti-Hero ft. Bleachers")).isEqualTo("anti hero")
        assertThat(musicTitleKey("Café del Mar")).isEqualTo("cafe del mar")
    }

    @Test
    fun `an artist channel's Topic suffix is not part of the name`() {
        assertThat(musicArtistKey("Fairuz - Topic")).isEqualTo(musicArtistKey("FAIRUZ"))
    }

    @Test
    fun `canonical key and variant matcher collapse slowed reverb sped up and 8d copies`() {
        val original =
            MusicTrack(
                videoId = "orig",
                title = "Blinding Lights",
                artist = "The Weeknd",
                thumbnailUrl = "",
                duration = 200,
            )
        val slowed =
            MusicTrack(
                videoId = "slow",
                title = "The Weeknd - Blinding Lights (Slowed + Reverb)",
                artist = "Slowed Heaven",
                thumbnailUrl = "",
                duration = 245,
            )
        val spedUp =
            MusicTrack(
                videoId = "fast",
                title = "Blinding Lights Sped Up",
                artist = "Nightcore Vibes",
                thumbnailUrl = "",
                duration = 165,
            )
        val nightcore =
            MusicTrack(
                videoId = "nc",
                title = "Blinding Lights - Nightcore & 8D Audio",
                artist = "8D Music",
                thumbnailUrl = "",
                duration = 175,
            )

        assertThat(musicIsFanEditVariant(slowed.title)).isTrue()
        assertThat(musicIsFanEditVariant(spedUp.title)).isTrue()
        assertThat(musicIsFanEditVariant(nightcore.title)).isTrue()
        assertThat(musicIsFanEditVariant(original.title)).isFalse()

        assertThat(musicCanonicalTitleKey(slowed.title, listOf("The Weeknd"))).isEqualTo("blinding lights")
        assertThat(musicCanonicalTitleKey(spedUp.title)).isEqualTo("blinding lights")
        assertThat(original.isSameSongOrVariant(slowed)).isTrue()
        assertThat(original.isSameSongOrVariant(spedUp)).isTrue()
        assertThat(original.isSameSongOrVariant(nightcore)).isTrue()

        val deduped = listOf(slowed, spedUp, original, nightcore).dedupeCanonicalSongs()
        assertThat(deduped.map { it.videoId }).containsExactly("orig")
    }

    @Test
    fun `different songs sharing a plain title across unrelated artists are not collapsed`() {
        val rihannaStay =
            MusicTrack(
                videoId = "stay1",
                title = "Stay",
                artist = "Rihanna",
                thumbnailUrl = "",
                duration = 240,
            )
        val zeddStay =
            MusicTrack(
                videoId = "stay2",
                title = "Stay",
                artist = "Zedd",
                thumbnailUrl = "",
                duration = 210,
            )
        assertThat(rihannaStay.isSameSongOrVariant(zeddStay)).isFalse()
        assertThat(listOf(rihannaStay, zeddStay).dedupeCanonicalSongs()).hasSize(2)
    }

    @Test
    fun `music language detection recognizes bangla and hindi scripts and markers`() {
        assertThat(musicDetectLanguage("তুমি আমার (Slowed + Reverb)", "Unknown")).isEqualTo("bn")
        assertThat(musicDetectLanguage("Oporadhi Bangla Song", "Eagle Music")).isEqualTo("bn")
        assertThat(musicDetectLanguage("Tum Hi Ho", "T-Series")).isEqualTo("hi")
        assertThat(musicDetectLanguage("Midnight City", "M83")).isNull()
    }
}
