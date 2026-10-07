package com.mosman.wird.domain

import java.text.Normalizer

/**
 * Word search across the whole Qur'an, on the phone: Arabic, English, Hausa and transliteration.
 *
 * PLAN task 26, his answers of 2026-10-04: one search box on the Sūrahs tab, all four languages.
 * Everything searched is already bundled, so it needs no download and works offline.
 *
 * **What readers ran into in other apps, and what this does about it** (Quran for Android's own
 * issues 2014–2019, Stack Overflow 2011–2023; sources in PLAN task 26):
 *
 * - **Attached letters.** Whole-word matching found "قمر" in no verse, "القمر" in 5 and "والقمر"
 *   in 20, none overlapping. Here the query is matched inside words, and a leading article ("ال",
 *   "وال", "بال", …) is taken off the query first, so all three find the same verses.
 * - **Spellings of one letter.** أ إ آ ٱ all count as ا, ة as ه, ى and ئ as ي, ؤ as و, and the
 *   vowel marks and Qur'anic signs are ignored. The bundled text is the modern spelling (الصلاة,
 *   not the Uthmani الصلوٰة), so what people type on a phone matches it.
 * - **Exact first.** A verse holding the word exactly as typed ranks above one holding it with an
 *   attached letter, which ranks above one holding it inside a longer word.
 * - **The highlight.** Text is matched with its marks folded away, and every folded character
 *   remembers where it came from, so the highlight lands on the right letters of the real text.
 * - **Transliteration by sound.** "raheem", "rahim" and the bundled "alrraheemi" meet once
 *   doubled letters are collapsed and e/o are read as i/u.
 *
 * What it cannot do: find a word's whole family from its root (كتب, كاتب, مكتوب need morphology
 * data that is not in the app), search tafsir, or forgive a badly misspelled word.
 */
object QuranSearch {

    enum class Field(val label: String) {
        ARABIC("Arabic"),
        ENGLISH("English"),
        HAUSA("Hausa"),
        TRANSLITERATION("Transliteration"),
    }

    /** One verse's four texts, exactly as the app shows them. */
    data class VerseText(
        val key: Pair<Int, Int>,
        val arabic: String,
        val english: String,
        val hausa: String,
        val transliteration: String,
    ) {
        fun of(field: Field): String = when (field) {
            Field.ARABIC -> arabic
            Field.ENGLISH -> english
            Field.HAUSA -> hausa
            Field.TRANSLITERATION -> transliteration
        }
    }

    /**
     * A verse that matched, before its words are read: which of its texts, how well, and where in
     * the folded text. [rank] is 4 for the word exactly as typed, 3 for it with an attached letter,
     * 2 at the start of a longer word, 1 inside one.
     */
    class Match internal constructor(
        val key: Pair<Int, Int>,
        val field: Field,
        val rank: Int,
        /** The folded text it matched in, as the index holds it. */
        internal val hay: String,
        /** Each place it matched, as positions in [hay]. */
        internal val spans: List<IntRange>,
    )

    /** A match shown on the verse's own words: [highlight] marks characters of [text]. */
    data class Hit(
        val key: Pair<Int, Int>,
        val field: Field,
        val text: String,
        val highlight: List<IntRange>,
        val rank: Int,
    )

    /** Text folded for matching, and for each folded character the index it came from. */
    class Folded(val text: String, val from: IntArray)

    /** One verse's four texts folded for matching, without the maps. */
    class FoldedVerse(val arabic: String, val english: String, val hausa: String, val transliteration: String) {
        internal fun of(field: Field): String = when (field) {
            Field.ARABIC -> arabic
            Field.ENGLISH -> english
            Field.HAUSA -> hausa
            Field.TRANSLITERATION -> transliteration
        }
    }

    /** [verse] folded exactly as [Index] matches it. */
    fun fold(verse: VerseText) = FoldedVerse(
        arabic = foldArabicInto(verse.arabic, null),
        english = foldLatinInto(verse.english, null),
        hausa = foldLatinInto(verse.hausa, null),
        transliteration = foldSoundInto(foldLatinInto(verse.transliteration, null), null),
    )

    /**
     * The whole Qur'an folded, one long text per language: a verse a line, `2:255`, a tab, then
     * the verse's folded words.
     *
     * The app reads the four texts ready-made from `assets/search/` ([fileOf]) and searches each
     * as one string; a match finds its verse from the line it sits on. Loading them is reading
     * four files, with no work per verse. Measured on the emulator 2026-10-04, on a freshly
     * installed APK (it runs slowly until Android compiles it, and testers install the APK
     * straight from the site):
     * - folding all 2,416 page files at the first search: 51 s, then 9.6 s with tighter loops;
     * - one ready-made file, split into a string per verse and language: 3.5 s.
     *
     * QuranSearchTest folds the bundled texts again ([of]) and fails when a file is stale. Only
     * folded text is kept; the map back to the real letters is worked out in [hit], for the
     * verses that are shown.
     */
    class Index(internal val texts: Map<Field, String>) {
        init {
            require(texts.keys == Field.entries.toSet()) { "the index needs all four texts, has ${texts.keys}" }
        }

        /** Every verse's key, in mushaf order, read from the Arabic text. */
        val keys: List<Pair<Int, Int>> by lazy {
            texts.getValue(Field.ARABIC).lineSequence().filter { it.isNotEmpty() }
                .map { line -> line.substringBefore('\t').let { it.substringBefore(':').toInt() to it.substringAfter(':').toInt() } }
                .toList()
        }

        companion object {
            /** The bundled file holding [field]'s text. */
            fun fileOf(field: Field) = "search/${field.name.lowercase()}.txt"

            /** Folds [verses] here and now: what the four files must hold. */
            fun of(verses: List<VerseText>): Index {
                val folded = verses.map { QuranSearch.fold(it) }
                return Index(
                    Field.entries.associateWith { field ->
                        buildString {
                            for (i in verses.indices) {
                                append(verses[i].key.first).append(':').append(verses[i].key.second)
                                append('\t').append(folded[i].of(field)).append('\n')
                            }
                        }
                    },
                )
            }
        }
    }

    /** Clitics written onto the front of an Arabic word: and, so, with, for, like, the. */
    private val ARTICLES = listOf("وال", "فال", "بال", "كال", "لل", "ال")
    private val PREFIXES = setOf("و", "ف", "ب", "ل", "ك") + ARTICLES

    /** Fewer letters than this would match a large part of the Qur'an. */
    private const val SHORTEST = 2

    fun isArabic(query: String): Boolean = query.any { it in '؀'..'ۿ' }

    /** Whether [query] has enough letters to search the verses at all. */
    fun searchable(query: String): Boolean =
        (if (isArabic(query)) foldArabicInto(query, null) else foldLatinInto(query, null)).trim().length >= SHORTEST

    /**
     * Every verse that holds [query], best match first, then in mushaf order. Empty when the
     * query is too short to mean anything. Show one with [hit].
     */
    fun search(index: Index, query: String): List<Match> {
        val found = if (isArabic(query)) searchArabic(index, query) else searchLatin(index, query)
        return found.sortedWith(compareByDescending<Match> { it.rank }.thenBy { it.key.first }.thenBy { it.key.second })
    }

    /**
     * [match] on [text], the verse's own words in the matched field: the folded positions mapped
     * back to its real letters. Text other than what the index was folded from gets no highlight
     * rather than a wrong one.
     */
    fun hit(match: Match, text: String): Hit {
        // Named in full: RecitationCheck.kt has a top-level foldArabic(String) of its own.
        val folded = when (match.field) {
            Field.ARABIC -> QuranSearch.foldArabic(text)
            Field.TRANSLITERATION -> foldSound(foldLatin(text))
            else -> foldLatin(text)
        }
        val highlight = if (folded.text != match.hay) emptyList()
        else match.spans.map { original(folded, text, it.first, it.last + 1, marks = match.field == Field.ARABIC) }
        return Hit(match.key, match.field, text, highlight, match.rank)
    }

    private fun searchArabic(index: Index, query: String): List<Match> {
        val typed = foldArabicInto(query, null).trim()
        if (typed.length < SHORTEST) return emptyList()
        val core = if (' ' in typed) typed else {
            ARTICLES.firstOrNull { typed.startsWith(it) && typed.length - it.length >= SHORTEST }
                ?.let { typed.removePrefix(it) } ?: typed
        }
        val found = ArrayList<Match>()
        scan(index.texts.getValue(Field.ARABIC), core) { key, hay, spans ->
            found += Match(key, Field.ARABIC, spans.maxOf { rankArabic(hay, it.first, it.last + 1, typed) }, hay, spans)
        }
        return found
    }

    private fun searchLatin(index: Index, query: String): List<Match> {
        val typed = foldLatinInto(query, null).trim()
        if (typed.length < SHORTEST) return emptyList()
        val sound = foldSoundInto(typed, null)
        // The best-ranked field for each verse; on a tie the earlier field, English first.
        val best = LinkedHashMap<Pair<Int, Int>, Match>()
        for (field in LATIN) {
            scan(index.texts.getValue(field), if (field == Field.TRANSLITERATION) sound else typed) { key, hay, spans ->
                val rank = spans.maxOf { rankLatin(hay, it.first, it.last + 1) }
                val held = best[key]
                if (held == null || rank > held.rank) best[key] = Match(key, field, rank, hay, spans)
            }
        }
        return best.values.toList()
    }

    private val LATIN = listOf(Field.ENGLISH, Field.HAUSA, Field.TRANSLITERATION)

    /**
     * Every verse whose line in [text] holds [needle], once each, in order: its key, its folded
     * words and where the needle sits in them. A needle never holds a tab or a line break, so a
     * match can't run from one verse into the next, and one inside a key ("2:255") is skipped.
     */
    private inline fun scan(text: String, needle: String, onVerse: (Pair<Int, Int>, String, List<IntRange>) -> Unit) {
        if (needle.isEmpty()) return
        var at = text.indexOf(needle)
        while (at >= 0) {
            val lineStart = text.lastIndexOf('\n', at - 1) + 1
            val tab = text.indexOf('\t', lineStart)
            if (at < tab) {
                at = text.indexOf(needle, tab)
                continue
            }
            val wordsStart = tab + 1
            val lineEnd = text.indexOf('\n', at).let { if (it < 0) text.length else it }
            val spans = ArrayList<IntRange>(2)
            while (at in wordsStart until lineEnd) {
                spans += (at - wordsStart) until (at - wordsStart + needle.length)
                at = text.indexOf(needle, at + 1)
            }
            val colon = text.indexOf(':', lineStart)
            val key = text.substring(lineStart, colon).toInt() to text.substring(colon + 1, tab).toInt()
            onVerse(key, text.substring(wordsStart, lineEnd), spans)
        }
    }

    private fun wordStart(hay: String, at: Int): Int = hay.lastIndexOf(' ', at - 1) + 1
    private fun wordEnds(hay: String, end: Int): Boolean = end == hay.length || hay[end] == ' '

    private fun rankArabic(hay: String, start: Int, end: Int, typed: String): Int {
        val wordAt = wordStart(hay, start)
        val ends = wordEnds(hay, end)
        val prefix = hay.substring(wordAt, start)
        val word = hay.substring(wordAt, hay.indexOf(' ', end).let { if (it < 0) hay.length else it })
        return when {
            word == typed -> 4
            ends && (prefix.isEmpty() || prefix in PREFIXES) -> 3
            prefix.isEmpty() || prefix in PREFIXES -> 2
            else -> 1
        }
    }

    private fun rankLatin(hay: String, start: Int, end: Int): Int {
        val starts = start == 0 || hay[start - 1] == ' '
        return when {
            starts && wordEnds(hay, end) -> 4
            starts -> 2
            else -> 1
        }
    }

    /** The characters of [text] behind folded [start, end), with any vowel marks after them. */
    private fun original(folded: Folded, text: String, start: Int, end: Int, marks: Boolean): IntRange {
        val first = folded.from[start]
        var last = folded.from[end - 1]
        if (marks) while (last + 1 < text.length && isMark(text[last + 1])) last++
        return first..last
    }

    private fun isMark(c: Char): Boolean =
        c in 'ً'..'ٟ' || c == 'ٰ' || c in 'ۖ'..'ۭ' || c == 'ـ'

    // The folds below run on every verse shown in the results, and over the whole Qur'an when the
    // test rebuilds the index, so each loops by position over plain chars. The first version used
    // withIndex() and a nullable Char, which made objects for every character.

    /** Arabic for matching: marks and signs gone, one alif, ة as ه, ى and ئ as ي, ؤ as و. */
    fun foldArabic(text: String): Folded {
        val from = IntArray(text.length)
        val folded = foldArabicInto(text, from)
        return Folded(folded, from.copyOf(folded.length))
    }

    /**
     * Latin for matching: lower case, accents off, Hausa's hooked letters as plain ones, apostrophes
     * gone so "Qur'an" meets "quran", anything else a space.
     */
    fun foldLatin(text: String): Folded {
        val from = IntArray(text.length)
        val folded = foldLatinInto(text, from)
        return Folded(folded, from.copyOf(folded.length))
    }

    /** Transliteration by sound: doubled letters once, e as i, o as u. Keeps the map. */
    fun foldSound(folded: Folded): Folded {
        val from = IntArray(folded.text.length)
        val sound = foldSoundInto(folded.text, from)
        for (i in sound.indices) from[i] = folded.from[from[i]]
        return Folded(sound, from.copyOf(sound.length))
    }

    /** [text] folded; when [from] is given, each folded character's index in [text] goes into it. */
    private fun foldArabicInto(text: String, from: IntArray?): String {
        val out = StringBuilder(text.length)
        for (i in text.indices) {
            val c = arabicLetter(text[i])
            if (c == DROP || (c == ' ' && (out.isEmpty() || out[out.length - 1] == ' '))) continue
            from?.set(out.length, i)
            out.append(c)
        }
        return out.toString()
    }

    private fun foldLatinInto(text: String, from: IntArray?): String {
        val out = StringBuilder(text.length)
        for (i in text.indices) {
            val c = latinLetter(text[i])
            if (c == DROP || (c == ' ' && (out.isEmpty() || out[out.length - 1] == ' '))) continue
            from?.set(out.length, i)
            out.append(c)
        }
        return out.toString()
    }

    private fun foldSoundInto(text: String, from: IntArray?): String {
        val out = StringBuilder(text.length)
        for (i in text.indices) {
            val c = when (val raw = text[i]) {
                'e' -> 'i'
                'o' -> 'u'
                else -> raw
            }
            if (c != ' ' && out.isNotEmpty() && out[out.length - 1] == c) continue
            from?.set(out.length, i)
            out.append(c)
        }
        return out.toString()
    }

    /** Stands for "leave this character out". */
    private const val DROP = '\u0000'

    private fun arabicLetter(ch: Char): Char = when {
        isMark(ch) -> DROP
        ch == 'آ' || ch == 'أ' || ch == 'إ' || ch == 'ٱ' -> 'ا'
        ch == 'ة' -> 'ه'
        ch == 'ى' || ch == 'ئ' -> 'ي'
        ch == 'ؤ' -> 'و'
        ch.isWhitespace() -> ' '
        ch.isLetter() -> ch
        else -> DROP
    }

    private fun latinLetter(ch: Char): Char = when {
        ch in 'a'..'z' || ch in '0'..'9' -> ch
        ch in 'A'..'Z' -> ch + ('a' - 'A')
        ch == '\'' || ch == '`' -> DROP
        ch.code < 0x80 -> ' '
        else -> latinWide(ch)
    }

    private fun latinWide(ch: Char): Char {
        if (ch.code >= WIDE.size) return foldWide(ch)
        val known = WIDE[ch.code]
        if (known != UNSEEN) return known
        val folded = foldWide(ch)
        WIDE[ch.code] = folded
        return folded
    }

    /**
     * Each letter past ASCII, worked out the first time it turns up: Latin-1 and Extended (ā, ã,
     * ƙ), the hooks (ɓ, ɗ), the ʿayn marks and Latin Extended Additional (ḥ, ṣ, ẽ). Two threads
     * may work one out at once; both write the same answer.
     */
    private val WIDE = CharArray(0x2020) { UNSEEN }
    private const val UNSEEN = '￿'

    private fun foldWide(raw: Char): Char {
        val ch = raw.lowercaseChar()
        return when {
            ch == 'ɓ' -> 'b'
            ch == 'ɗ' -> 'd'
            ch == 'ƙ' -> 'k'
            ch == 'ƴ' -> 'y'
            ch == '’' || ch == '‘' || ch == 'ʿ' || ch == 'ʾ' -> DROP
            // A combining accent belongs to the letter before it: the Hausa file writes the
            // disjointed letters as L plus a combining tilde ("A. L̃. M̃.").
            ch.category == CharCategory.NON_SPACING_MARK -> DROP
            ch.isLetter() -> Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)[0].takeIf { it in 'a'..'z' } ?: ch
            ch.isDigit() -> ch
            else -> ' '
        }
    }
}
