package com.mosman.wird.domain

import java.time.LocalDateTime

/** Who said a thing. Two speakers, and the app is never pretending to be a third. */
enum class Speaker { WIRD, YOU }

/**
 * One line of the conversation.
 *
 * Kept as plain data with no behaviour, so the store can write it to readable JSON and the
 * screen can render it without asking anything else. [at] is local time because that is how
 * it is read back — "you said this at 6:12 pm" — and this app has no server to disagree with
 * about time zones.
 */
data class Turn(
    val who: Speaker,
    val text: String,
    val at: LocalDateTime,
)

/**
 * A promise, held so it can be shown back to you.
 *
 * **This is the mechanic, not a detail.** PROFILE.md § 5b: naming a time to something that
 * checks back is the whole experiment, and a promise you cannot see is one you have already
 * half-forgotten. The chat keeps it pinned at the top for exactly that reason.
 *
 * PLAN task 21 also requires it: *every commitment is logged — what was promised, whether it
 * was kept* — because task 24 cannot measure what was never recorded.
 */
data class Commitment(
    /** What you actually said — "after Isha", not the time it resolved to. */
    val spoken: String,
    val madeAt: LocalDateTime,
)

/**
 * An ayah you saved.
 *
 * The verse key is all that is stored — "18:10", not the words. The app can always find the
 * page and the surah from that, and it means a bookmark is nine bytes rather than a copy of
 * Qur'anic text sitting in a second place where it could drift from the source.
 */
data class Bookmark(
    val verseKey: String,
    val savedAt: LocalDateTime,
)
