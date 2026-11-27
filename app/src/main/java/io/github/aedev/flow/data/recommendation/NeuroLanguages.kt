/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.data.recommendation

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Which language a title is written in, and that language's stop words (#907). Stop lists are data
 * in `resources/neuro/stop`, made by `scripts/neuro/gen_stoplists.py`. A title gets the lists of the
 * viewer's [UserBrain.languageProfile] and of its own language when [detect] is sure of it, so
 * "des", "les" and "как" never become interests.
 */
internal object NeuroLanguages {
    val STOP_LIST_LANGUAGES =
        setOf("en", "es", "pt", "fr", "de", "it", "ru", "id", "ar", "cs", "et", "fa", "hi", "ja", "ko", "pl", "tr", "uk", "vi", "zh")

    /** Confident detections counted before the profile can move to another language. */
    const val PROFILE_WINDOW = 200

    /** Share of [PROFILE_WINDOW] one other language needs to become the profile. */
    const val PROFILE_SWITCH_SHARE = 0.7

    private const val MIN_HITS = 2
    private const val MIN_MARGIN = 2
    private const val MIN_SCRIPT_LETTERS = 2

    // Frequent function words, less those English titles use as words ("pro", "plus", "son"). A word
    // only one language uses counts twice.
    internal val FunctionWords =
        mapOf(
            "en" to "the and of to in is for on with my you your how what why this that are from was it at an be we i it's don't i'm",
            "es" to "el la los las de del que y en un una por con para como es se su al lo más mi cómo qué pero muy este esta yo tu",
            "pt" to
                "o os as de do da dos das que e em um uma para com não no na por como mais é eu você seu sua meu minha isso esse essa são ao pra",
            "fr" to
                "le la les des du de et en un une pour avec est que qui dans sur pas au aux ce cette je tu il elle nous vous mon ma mes c'est comment pourquoi j'ai",
            "de" to
                "der die das und ist mit den dem ein eine einen nicht ich du wir sie es zu von für auf im auch wie was warum mein meine bei nach aus oder aber",
            "it" to
                "il lo la gli le di del della che e è un una per con non sono come perché più mi ti ci si nel nella al alla dei delle questo questa ho",
            "id" to
                "yang dan di ke dari ini itu untuk dengan tidak ada saya aku kamu kita cara apa bisa akan sudah juga dalam pada jadi lagi buat",
            "tr" to "ve bir bu da de ile için ne çok ben sen biz mi mı nasıl neden gibi daha en var yok olan ama şu",
            "pl" to "i w z na się nie do to że jak co jest o po dla od ale czy tak mój moja za przez ten ta jestem",
            "cs" to "v ve z na se je to že jak co do od ale jsem není tak po při k o s můj moje jsou",
            "et" to "ja ei on et kui mis ma sa ta ning oma aga kas väga kõik ka",
        ).mapValues { (_, words) -> words.split(' ') }

    private val Languages = FunctionWords.keys.toList()

    /** Letters only one of these languages writes; a word holding one is a function word's worth. */
    internal val OwnLetterSets =
        mapOf(
            "es" to "ñ",
            "pt" to "ãõ",
            "fr" to "œ",
            "de" to "ß",
            "tr" to "ığ",
            "pl" to "ąęłńśźż",
            "cs" to "ěřů",
        )

    private val OwnLetters =
        OwnLetterSets.entries
            .flatMap { (language, letters) ->
                letters.map {
                    it to
                        Languages.indexOf(
                            language,
                        )
                }
            }.toMap()

    // Without і, ї, є or ґ, these words still tell Ukrainian from Russian.
    internal val UkrainianWords = setOf("чи", "чому", "від", "та", "це", "як", "що", "який", "яка", "яке", "цей", "ця", "ці", "також", "й")

    private val WordLanguages: Map<String, List<Int>> by lazy {
        val index = HashMap<String, MutableList<Int>>()
        FunctionWords.forEach { (language, words) ->
            words.forEach { word -> index.getOrPut(word) { ArrayList(2) } += Languages.indexOf(language) }
        }
        index
    }

    internal val FrenchElisions = setOf("j", "qu", "n", "s", "m", "t")
    internal val SharedElisions = setOf("l", "d", "c")
    internal val ItalianElisions = setOf("un", "dell", "all", "nell", "sull", "dall")

    private val stopLists = ConcurrentHashMap<String, Set<String>>()

    /** The two-letter code the stop lists use: "in-ID" reads "id", "iw" reads "he". */
    fun normalize(code: String): String =
        when (val language = code.substringBefore('-').substringBefore('_').lowercase()) {
            "in" -> "id"
            "iw" -> "he"
            "ji" -> "yi"
            else -> language
        }

    /** Only the `zh` lists fold Han; `ja` and `ko` keep their kanji and hanja. */
    fun listFold(language: String): HanFold = if (language == "zh") HanFold.UNLESS_KANA else HanFold.NONE

    /** [language]'s stop words, folded, or none when it has no list. Read once per process. */
    fun stopList(language: String): Set<String> {
        if (language !in STOP_LIST_LANGUAGES) return emptySet()
        return stopLists.getOrPut(language) {
            val stream = NeuroLanguages::class.java.getResourceAsStream("/neuro/stop/$language.txt") ?: return@getOrPut emptySet()
            stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapTo(HashSet()) { NeuroText.fold(it.trim(), listFold(language)) }
            }
        }
    }

    private val runBreakSets = ConcurrentHashMap<String, Set<Char>>()

    /** Merged-run break characters of [language] (`resources/neuro/runbreak`), or null: no merge. */
    fun runBreaks(language: String): Set<Char>? {
        val set =
            runBreakSets.getOrPut(language) {
                val stream = NeuroLanguages::class.java.getResourceAsStream("/neuro/runbreak/$language.txt") ?: return@getOrPut emptySet()
                stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapTo(HashSet()) {
                        NeuroText.fold(it.trim(), listFold(language)).single()
                    }
                }
            }
        return set.takeIf { it.isNotEmpty() }
    }

    private val TOPICAL_LANGUAGES = listOf("zh")

    /**
     * Words of channel names in scripts written without spaces that name a topic, folded
     * (`resources/neuro/topical`). English has [NeuroTokenizer]'s own list. Read once per process.
     */
    val channelTopics: Set<String> by lazy {
        buildSet {
            TOPICAL_LANGUAGES.forEach { language ->
                val stream = NeuroLanguages::class.java.getResourceAsStream("/neuro/topical/$language.txt") ?: return@forEach
                stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapTo(this) { NeuroText.fold(it.trim()) }
                }
            }
        }
    }

    /**
     * The language of [pieces], the folded whitespace-separated pieces of a title, or null when
     * unsure. Scripts used by one language decide alone; Latin titles need two function words and a
     * lead of two points over the next language.
     */
    fun detect(pieces: List<String>): String? {
        val script = ScriptCount()
        var ascii = true
        pieces.forEach { piece ->
            for (c in piece) {
                if (c.code < 0x80) {
                    if (c.isLetter()) script.latin++
                } else {
                    ascii = false
                    script.add(c)
                }
            }
        }
        script.language()?.let { language ->
            if (language == "ru" && pieces.any { it.trim { c -> !NeuroText.isWordChar(c) } in UkrainianWords }) return "uk"
            return language.ifEmpty { null }
        }
        if (!ascii && script.vietnameseWords(pieces) >= MIN_HITS) return "vi"

        val points = IntArray(Languages.size)
        val hits = IntArray(Languages.size)
        pieces.forEach { piece ->
            val word = piece.trim { !NeuroText.isWordChar(it) && it != '\'' }
            if (!ascii) {
                word.firstNotNullOfOrNull { c -> if (c.code < 0x80) null else OwnLetters[c] }?.let {
                    points[it] += 2
                    hits[it]++
                }
            }
            val languages = WordLanguages[word] ?: (if ('\'' in word) elisionLanguages(word) else null) ?: return@forEach
            val weight = if (languages.size == 1) 2 else 1
            languages.forEach {
                points[it] += weight
                hits[it]++
            }
        }
        var best = 0
        var second = 0
        for (i in 1 until points.size) {
            if (points[i] > points[best]) {
                second = points[best]
                best = i
            } else if (points[i] > second) {
                second = points[i]
            }
        }
        return Languages[best].takeIf { hits[best] >= MIN_HITS && points[best] - second >= MIN_MARGIN }
    }

    /** [detect] over raw [text], such as a title and the first line of its description. */
    fun detectText(text: String): String? = detect(NeuroText.words(NeuroText.fold(text)))

    private const val LANGUAGE_PENALTY_HALF_LIFE_MS = 14L * 86_400_000L
    private const val LANGUAGE_PENALTY_FORGET_BELOW = 0.35
    private const val MAX_LANGUAGE_PENALTY_COUNT = 8

    private val PrimaryBanglaLatinMarkers =
        setOf("bangla", "bengali", "natok", "gaan", "bangladeshi", "valobasha", "bhalobasha", "oporadhi")
    private val SecondaryBanglaLatinMarkers =
        setOf("kolkata", "dhaka", "tumi", "amar", "tomar", "moner", "rabindra", "nazrul", "baul", "kotha", "boshonto")
    private val BanglaChannelSignatures =
        listOf("coke studio bangla", "svf", "sangeeta", "g-series", "g series", "eagle music")
    private val PrimaryHindiLatinMarkers =
        setOf("hindi", "bollywood", "gaana", "bhajan", "qawwali", "ghazal", "shayari", "bhojpuri", "haryanvi", "aashiqui")
    private val SecondaryHindiLatinMarkers =
        setOf("pyaar", "pyar", "ishq", "mohabbat", "bewafa", "zindagi", "zindgi", "sanam", "judai", "tumhara", "tere")
    private val IndicChannelSignatures =
        listOf("t-series", "t series", "zee music", "sony music india", "saregama", "tips official", "yrf")

    /**
     * Best-effort content language detection across native scripts, function words, and unmistakable
     * Romanized South-Asian markers (used when building negative language avoidance signals).
     */
    fun detectContentLanguage(
        text: String,
        channelName: String = "",
    ): String? {
        val combined = if (channelName.isBlank()) text else "$text\n$channelName"
        detectMixedRegionalScript(combined)?.let { return it }
        detectText(combined)?.let { return it }

        val foldedText = NeuroText.fold(combined).lowercase(Locale.ROOT)
        if (BanglaChannelSignatures.any { it in foldedText }) return "bn"
        if (IndicChannelSignatures.any { it in foldedText }) return "hi"

        val words = NeuroText.words(foldedText)
        if (words.isEmpty()) return null

        var banglaPrimary = 0
        var banglaSecondary = 0
        var hindiPrimary = 0
        var hindiSecondary = 0
        for (w in words) {
            when {
                w in PrimaryBanglaLatinMarkers -> banglaPrimary++
                w in SecondaryBanglaLatinMarkers -> banglaSecondary++
                w in PrimaryHindiLatinMarkers -> hindiPrimary++
                w in SecondaryHindiLatinMarkers -> hindiSecondary++
            }
        }
        return when {
            banglaPrimary >= 1 || banglaSecondary >= 2 -> "bn"
            hindiPrimary >= 1 || hindiSecondary >= 2 -> "hi"
            else -> null
        }
    }

    /**
     * Detects distinctive regional scripts even when a music or video title wraps a short native
     * phrase in longer English metadata such as "(Official Music Video) | Slowed + Reverb".
     */
    private fun detectMixedRegionalScript(text: String): String? {
        var bengali = 0
        var devanagari = 0
        var tamil = 0
        var telugu = 0
        var hangul = 0
        var kana = 0
        for (c in text) {
            if (!c.isLetter()) continue
            when (Character.UnicodeScript.of(c.code)) {
                Character.UnicodeScript.BENGALI -> bengali++
                Character.UnicodeScript.DEVANAGARI -> devanagari++
                Character.UnicodeScript.TAMIL -> tamil++
                Character.UnicodeScript.TELUGU -> telugu++
                Character.UnicodeScript.HANGUL -> hangul++
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> kana++
                else -> Unit
            }
        }
        return when {
            bengali >= 2 -> "bn"
            devanagari >= 2 -> "hi"
            tamil >= 2 -> "ta"
            telugu >= 2 -> "te"
            kana >= 2 -> "ja"
            hangul >= 2 -> "ko"
            else -> null
        }
    }

    fun effectiveLanguagePenalty(
        signal: RejectionSignal,
        now: Long,
    ): Double {
        if (signal.count <= 0) return 0.0
        val ageMs = (now - signal.lastRejectedAt).coerceAtLeast(0L)
        return signal.count * 0.5.pow(ageMs.toDouble() / LANGUAGE_PENALTY_HALF_LIFE_MS)
    }

    fun recordLanguageAvoidance(
        penalties: Map<String, RejectionSignal>,
        language: String?,
        primaryProfile: String?,
        now: Long,
        weight: Int = 1,
    ): Map<String, RejectionSignal> {
        if (language.isNullOrBlank() || language == primaryProfile || weight <= 0) return penalties
        val next = LinkedHashMap<String, RejectionSignal>(penalties.size + 1)
        penalties.forEach { (lang, signal) ->
            if (effectiveLanguagePenalty(signal, now) >= LANGUAGE_PENALTY_FORGET_BELOW) {
                next[lang] = signal
            }
        }
        val existing = next[language]
        val decayedCount = if (existing != null) effectiveLanguagePenalty(existing, now).roundToInt() else 0
        val newCount = (decayedCount + weight).coerceIn(1, MAX_LANGUAGE_PENALTY_COUNT)
        next[language] = RejectionSignal(count = newCount, lastRejectedAt = now)
        return next
    }

    fun forgiveLanguage(
        penalties: Map<String, RejectionSignal>,
        language: String?,
    ): Map<String, RejectionSignal> {
        if (language.isNullOrBlank() || language !in penalties) return penalties
        val existing = penalties.getValue(language)
        return if (existing.count <= 1) {
            penalties - language
        } else {
            penalties + (language to existing.copy(count = existing.count - 1))
        }
    }

    fun languagePenaltyMultiplier(
        penalties: Map<String, RejectionSignal>,
        language: String?,
        now: Long,
    ): Double {
        if (penalties.isEmpty() || language.isNullOrBlank()) return 1.0
        val signal = penalties[language] ?: return 1.0
        val effective = effectiveLanguagePenalty(signal, now)
        return when {
            effective >= 2.8 -> 0.05
            effective >= 1.8 -> 0.15
            effective >= 0.8 -> 0.35
            else -> 1.0
        }
    }

    fun avoidedLanguageSet(
        penalties: Map<String, RejectionSignal>,
        now: Long,
        threshold: Double = 0.85,
    ): Set<String> =
        penalties.entries
            .filter { (_, signal) -> effectiveLanguagePenalty(signal, now) >= threshold }
            .mapTo(HashSet()) { it.key }

    /**
     * [profile] and [votes] after one more title whose language was [detected]. Every
     * [PROFILE_WINDOW] confident titles the votes are counted, and a language with
     * [PROFILE_SWITCH_SHARE] of them becomes the profile.
     */
    fun vote(
        profile: String,
        votes: Map<String, Int>,
        detected: String?,
    ): Pair<String, Map<String, Int>> {
        if (detected == null) return profile to votes
        val next = votes + (detected to (votes[detected] ?: 0) + 1)
        val total = next.values.sum()
        if (total < PROFILE_WINDOW) return profile to next
        val (leader, count) = next.maxBy { it.value }
        return (if (leader != profile && count >= PROFILE_SWITCH_SHARE * total) leader else profile) to emptyMap()
    }

    private fun elisionLanguages(word: String): List<Int>? {
        val prefix = word.substringBefore('\'', "").takeIf { it.isNotEmpty() && word.length > it.length + 1 } ?: return null
        return when (prefix) {
            in FrenchElisions -> listOf(Languages.indexOf("fr"))
            in SharedElisions -> listOf(Languages.indexOf("fr"), Languages.indexOf("it"))
            in ItalianElisions -> listOf(Languages.indexOf("it"))
            else -> null
        }
    }

    private class ScriptCount {
        var latin = 0
        var hangul = 0
        var kana = 0
        var han = 0
        var devanagari = 0
        var bengali = 0
        var tamil = 0
        var telugu = 0
        var arabic = 0
        var cyrillic = 0
        var persian = false
        var urdu = false
        var ukrainian = false
        var otherCyrillic = false

        fun add(c: Char) {
            if (!c.isLetter()) return
            when (Character.UnicodeScript.of(c.code)) {
                Character.UnicodeScript.LATIN -> {
                    latin++
                }

                Character.UnicodeScript.HANGUL -> {
                    hangul++
                }

                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> {
                    kana++
                }

                Character.UnicodeScript.HAN -> {
                    han++
                }

                Character.UnicodeScript.DEVANAGARI -> {
                    devanagari++
                }

                Character.UnicodeScript.BENGALI -> {
                    bengali++
                }

                Character.UnicodeScript.TAMIL -> {
                    tamil++
                }

                Character.UnicodeScript.TELUGU -> {
                    telugu++
                }

                Character.UnicodeScript.ARABIC -> {
                    arabic++
                    if (c in PERSIAN_LETTERS) persian = true
                    if (c in URDU_LETTERS) urdu = true
                }

                Character.UnicodeScript.CYRILLIC -> {
                    cyrillic++
                    if (c in UKRAINIAN_LETTERS) ukrainian = true
                    if (c in OTHER_CYRILLIC_LETTERS) otherCyrillic = true
                }

                else -> {
                    Unit
                }
            }
        }

        /** A language decided by script, "" when the script has no supported language, null for Latin. */
        fun language(): String? {
            // Kana and Hangul belong to one language each, so brand names around them do not matter.
            if (kana >= MIN_SCRIPT_LETTERS) return "ja"
            if (hangul >= MIN_SCRIPT_LETTERS) return "ko"
            val others = hangul + kana + han + devanagari + bengali + tamil + telugu + arabic + cyrillic
            if (others < MIN_SCRIPT_LETTERS || others < latin) return null
            return when {
                kana > 0 -> {
                    "ja"
                }

                han >= MIN_SCRIPT_LETTERS -> {
                    "zh"
                }

                devanagari >= MIN_SCRIPT_LETTERS -> {
                    "hi"
                }

                bengali >= MIN_SCRIPT_LETTERS -> {
                    "bn"
                }

                tamil >= MIN_SCRIPT_LETTERS -> {
                    "ta"
                }

                telugu >= MIN_SCRIPT_LETTERS -> {
                    "te"
                }

                arabic >= MIN_SCRIPT_LETTERS -> {
                    if (urdu) {
                        ""
                    } else if (persian) {
                        "fa"
                    } else {
                        "ar"
                    }
                }

                cyrillic >= MIN_SCRIPT_LETTERS -> {
                    if (otherCyrillic) {
                        ""
                    } else if (ukrainian) {
                        "uk"
                    } else {
                        "ru"
                    }
                }

                else -> {
                    ""
                }
            }
        }

        fun vietnameseWords(pieces: List<String>): Int = pieces.count { piece -> piece.any { it in 'Ạ'..'ỹ' || it in VIETNAMESE_LETTERS } }
    }

    internal val PERSIAN_LETTERS = setOf('پ', 'چ', 'ژ', 'گ', 'ک', 'ی')
    internal val URDU_LETTERS = setOf('ٹ', 'ڈ', 'ڑ', 'ں', 'ے', 'ھ')
    internal val UKRAINIAN_LETTERS = setOf('і', 'ї', 'є', 'ґ')

    // Serbian, Macedonian, Kazakh and Belarusian letters: no list fits, so no list is chosen.
    internal val OTHER_CYRILLIC_LETTERS = setOf('ђ', 'ј', 'љ', 'њ', 'ћ', 'џ', 'ѓ', 'ќ', 'ѕ', 'ә', 'ғ', 'қ', 'ң', 'ө', 'ұ', 'ү', 'һ', 'ў')
    internal val VIETNAMESE_LETTERS = setOf('ă', 'ơ', 'ư', 'đ')
}
