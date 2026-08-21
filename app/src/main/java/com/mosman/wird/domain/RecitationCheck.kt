package com.mosman.wird.domain

/**
 * Comparing what was recited against what the page says. **PLAN task 14, his actual ask.**
 *
 * His words, 2026-08-20: *"I don't need what it hears. I need it to hear it very well and
 * highlight from the pages and verses that this is where I did mistake."*
 *
 * ### ⚠ The rule this whole file is built around
 *
 * **Telling someone they erred in the Qur'an when they did not is the worst thing this app can
 * do.** It is worse than saying nothing, worse than being slow, and worse than admitting the
 * check failed. Everything below leans one way: **when in doubt, say nothing.**
 *
 * That is not timidity. A reader who is falsely corrected once will not trust a correct
 * correction afterwards, and a reader who is falsely corrected about their recitation may
 * simply stop recording — which would cost them the honest half of Sacred Rule 6 to protect a
 * feature that is only evidence.
 *
 * ### What it can and cannot see
 *
 * It compares **words**, not sounds. It cannot hear tajwīd, length of a madd, or the difference
 * between a heavy and a light letter — § 5p already ruled tajweed out, and nothing here changes
 * that. What it can catch is a **word skipped, a word added, or a word plainly different**, which
 * is what most people mean when they say they made a mistake in a portion.
 */

/** How one expected word fared. */
enum class WordMark {
    /** Heard where it was expected. */
    MATCHED,

    /** ⚠ Expected but not heard anywhere near where it should be. */
    MISSING,

    /** Heard something else in its place. */
    DIFFERENT,

    /**
     * Not judged at all.
     *
     * ⚠ **The most important value here.** Whisper stops early, mishears a whole stretch, or
     * the reader recited only part of the page — in all of those the honest answer is *"I have
     * nothing to say about this word"*, not "you got it wrong".
     */
    UNCHECKED,
}

/** One expected word and what became of it. */
data class CheckedWord(
    val verseKey: String,
    /** Zero-based position within its verse, so a screen can find the word to mark. */
    val index: Int,
    val expected: String,
    val mark: WordMark,
)

/**
 * The verdict on a whole portion.
 *
 * [confident] is the gate everything else hangs on. When it is false **nothing should be
 * highlighted at all** — the marks are still here for logs and tests, but the screen must say
 * it could not check rather than show a page covered in corrections.
 */
data class RecitationVerdict(
    val words: List<CheckedWord>,
    val confident: Boolean,
    /** Share of expected words that were matched, 0f..1f. */
    val coverage: Float,
    /** Said plainly, for the screen. */
    val summary: String,
) {
    val problems: List<CheckedWord>
        get() = words.filter { it.mark == WordMark.MISSING || it.mark == WordMark.DIFFERENT }

    /**
     * The ayahs to look at again, or empty when the check could not be trusted.
     *
     * ⚠ **Verses rather than words, and that is his instruction as well as the honest
     * resolution.** His ask named *"pages and verses"*, and marking an individual word would
     * claim a precision the model does not have — a speech transcript can say roughly where a
     * recitation diverged, not which syllable. An ayah is also the actionable unit: it is a
     * thing you can read again.
     *
     * ⚠ **Empty unless [confident].** Everything downstream reads this, so a check that could
     * not follow the recitation marks nothing at all rather than relying on each screen to
     * remember to ask.
     */
    val versesToReview: Set<String>
        get() = if (!confident) emptySet() else problems.map { it.verseKey }.toSet()
}

/**
 * Strip an Arabic word down to what two spellings of it have in common.
 *
 * ⚠ **Without this every single word is a "mistake".** A speech model writes ordinary Arabic;
 * the Qur'anic text carries vowel marks the model never emits, and several letters have forms
 * that are the same letter to a reader and different code points to a computer.
 *
 * Removed or folded, and nothing else:
 *  - **Diacritics** (fatha, kasra, damma, shadda, sukūn, the daggers and small letters). The
 *    model does not produce them; keeping them would fail every comparison.
 *  - **Alif forms** — أ إ آ ٱ all become ا. A reciter cannot pronounce the difference in a way
 *    a transcript preserves.
 *  - **Tā' marbūṭa** ة to ه, and **alif maqṣūra** ى to ي, which transcripts use interchangeably.
 *  - **Tatweel** ـ, a typographic stretch with no sound at all.
 *
 * ⚠ **This folding is for COMPARISON ONLY and never touches what is displayed.** The page is
 * still drawn from the mushaf font, letter for letter. Sacred Rule 2 is about the text the
 * reader sees; this is a fingerprint used to line two strings up.
 */
fun foldArabic(word: String): String = buildString {
    for (ch in word) {
        when {
            // Harakat, tanwin, shadda, sukun, superscript alif and the small Qur'anic marks.
            ch in 'ً'..'ٟ' -> Unit
            ch == 'ٰ' -> Unit
            ch in 'ۖ'..'ۭ' -> Unit
            // Tatweel: pure typography.
            ch == 'ـ' -> Unit
            // Alif in all its dresses.
            ch == 'آ' || ch == 'أ' || ch == 'إ' || ch == 'ٱ' -> append('ا')
            ch == 'ة' -> append('ه')
            ch == 'ى' -> append('ي')
            ch.isLetter() -> append(ch)
            else -> Unit
        }
    }
}

/** Split a line of Arabic into comparable words. */
fun arabicWords(text: String): List<String> =
    text.split(' ', '\n', '\t').map(::foldArabic).filter { it.isNotEmpty() }

/**
 * Line the recitation up against the page and mark what does not fit.
 *
 * **The algorithm is a longest-common-subsequence walk**, which is the same idea as a diff
 * between two files: find the longest run of words that appear in both, in order, and call
 * everything else a change. It is used rather than a word-by-word comparison because a single
 * missed word would otherwise push every following word out of step and mark the entire rest of
 * the page wrong — the exact failure that would make this feature unusable.
 *
 * @param expected the page's words, in order, each tagged with its verse.
 * @param heard what the recogniser produced.
 * @param minimumCoverage below this share matched, the whole check is called unreliable.
 */
fun checkRecitation(
    expected: List<Pair<String, String>>,
    heard: String,
    minimumCoverage: Float = MIN_COVERAGE,
): RecitationVerdict {
    val heardWords = arabicWords(heard)

    // ⚠ **Some "words" on a real page are pause marks, and they are not words.** The imlaei text
    // carries standalone stop signs — ۖ ۚ ۗ — as their own space-separated tokens. They fold to
    // nothing, are silent by definition, and can never be matched against a transcript.
    //
    // Leaving them in was a real false-accusation bug, found by checking against the shipped
    // Qur'an rather than a hand-written example: page 300 came back with four ayahs of Al-Kahf
    // marked wrong on a recitation that was **letter-perfect**. The synthetic test could not
    // have caught it, because nobody types a pause mark into a test fixture.
    val real = expected.filter { foldArabic(it.second).isNotEmpty() }
    val expectedFolded = real.map { foldArabic(it.second) }

    if (real.isEmpty()) {
        return RecitationVerdict(emptyList(), false, 0f, "There is nothing to check against.")
    }

    // Nothing usable came back. ⚠ Every word is UNCHECKED rather than MISSING: the app failed
    // to hear, which is not the same as the reader failing to recite, and the difference is the
    // whole ethic of this file.
    if (heardWords.isEmpty()) {
        return RecitationVerdict(
            words = real.mapIndexed { i, (key, word) ->
                CheckedWord(key, indexWithinVerse(real, i), word, WordMark.UNCHECKED)
            },
            confident = false,
            coverage = 0f,
            summary = "I couldn't make out any words, so I haven't marked anything.",
        )
    }

    val matched = longestCommonSubsequence(expectedFolded, heardWords)

    // Which expected words the reciter actually got through. Anything after the last match is
    // ground they may simply not have reached.
    val lastMatched = matched.lastOrNull() ?: -1
    val coverage = matched.size.toFloat() / expectedFolded.size

    val words = real.mapIndexed { i, (key, word) ->
        val mark = when {
            i in matched -> WordMark.MATCHED
            // ⚠ Past the last thing heard, so this is unread rather than misread. Stopping
            // halfway through a page is not a mistake and must never be shown as one.
            i > lastMatched -> WordMark.UNCHECKED
            else -> WordMark.DIFFERENT
        }
        CheckedWord(key, indexWithinVerse(real, i), word, mark)
    }

    val confident = coverage >= minimumCoverage
    return RecitationVerdict(
        words = words,
        confident = confident,
        coverage = coverage,
        summary = summarise(confident, coverage, words),
    )
}

private fun summarise(confident: Boolean, coverage: Float, words: List<CheckedWord>): String {
    if (!confident) {
        // ⚠ Names the app as the thing that failed, not the reader. Sacred Rule 3 governs the
        // sentence as much as the logic.
        return "I could only follow ${(coverage * 100).toInt()}% of that, so I haven't marked " +
            "anything. It may be the recording rather than the recitation."
    }
    val off = words.count { it.mark == WordMark.DIFFERENT }
    val unread = words.count { it.mark == WordMark.UNCHECKED }
    val tail = if (unread > 0) " The last $unread aren't marked — it sounds like you stopped there." else ""
    return when (off) {
        0 -> "That matched the page all the way through.$tail"
        1 -> "One word sounded different. It's marked on the page.$tail"
        else -> "$off words sounded different. They're marked on the page.$tail"
    }
}

/** Position of the word within its own verse, which is what a screen needs to find it. */
private fun indexWithinVerse(expected: List<Pair<String, String>>, at: Int): Int {
    var index = 0
    for (i in 0 until at) {
        index = if (expected[i].first == expected[at].first) index + 1 else 0
    }
    return index
}

/**
 * Indices of [a] that take part in the longest run common to both, in order.
 *
 * Classic dynamic programming, and it stays cheap because a page is a few hundred words: the
 * table is expected × heard, so a 200-word page against a 200-word transcript is 40,000 cells.
 */
private fun longestCommonSubsequence(a: List<String>, b: List<String>): Set<Int> {
    val table = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) {
        for (j in b.indices.reversed()) {
            table[i][j] = if (a[i] == b[j]) {
                table[i + 1][j + 1] + 1
            } else {
                maxOf(table[i + 1][j], table[i][j + 1])
            }
        }
    }

    val kept = mutableSetOf<Int>()
    var i = 0
    var j = 0
    while (i < a.size && j < b.size) {
        when {
            a[i] == b[j] -> { kept += i; i++; j++ }
            table[i + 1][j] >= table[i][j + 1] -> i++
            else -> j++
        }
    }
    return kept
}

/**
 * Below this share of the page matched, the check refuses to mark anything.
 *
 * ⚠ **A guess, and it is the number to tune once there are real recitations to tune it on.**
 * Set deliberately high: at 70% the app stays quiet on anything it half-followed, which is the
 * safe direction. The cost is missing some real mistakes; the cost of the other direction is
 * telling someone they erred in the Qur'an when they did not.
 */
const val MIN_COVERAGE = 0.70f
