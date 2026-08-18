package com.mosman.wird.domain

import java.time.LocalDate
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
    /**
     * The alarm this promise asked for, **for the day it was made and no longer**.
     *
     * ⚠ **Added 2026-08-18, PLAN task 22, fixing a real defect.** A commitment used to be
     * written straight into the daily reminder, so *"in an hour"* said once at three in the
     * afternoon made four o'clock the reminder time every day afterwards. Nobody asked for
     * that and nothing said it had happened. Keeping the schedule *here* is what makes a
     * promise about tonight expire like one: [appliesOn] is the whole mechanism.
     *
     * Null for a commitment restored from the older stored format, which had no schedule to
     * remember. It still shows and still counts; it just cannot re-arm anything.
     */
    val schedule: NudgeSchedule? = null,
) {
    /**
     * Whether this promise is still the one governing tonight's alarm.
     *
     * ⚠ Known edge, deliberately left: "in an hour" said at 23:50 resolves to 00:50, and the
     * next day this reads false. The alarm itself is an absolute time and still fires, so the
     * reminder arrives; only a re-arm in that fifty-minute window would drop it.
     */
    fun appliesOn(date: LocalDate): Boolean = madeAt.toLocalDate() == date
}

/**
 * A stretch of days with no reminders. **PLAN task 22.**
 *
 * *"I'm travelling till Sunday."* Both ends are inclusive and both are stored, because
 * "travelling next week" starts on a day that is not today — silencing the days in between
 * would take days he never asked for.
 *
 * **His decision, 2026-08-18: the days stay absent.** Nothing is written into the record for
 * a day spent away. It is a day he did not read, exactly like any other, and the streak
 * restarts afterwards. The alternative was logging them so the streak could bridge across,
 * and he chose the version where the record keeps meaning one thing and one thing only:
 * days you actually read. Sacred Rule 6 is easier to keep when nothing else lives in there.
 */
data class AwayPeriod(val from: LocalDate, val until: LocalDate) {

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(from) && !date.isAfter(until)

    /** The first day a reminder comes back. */
    val returnsOn: LocalDate get() = until.plusDays(1)

    /** Over and done with, so nothing needs to consult it again. */
    fun isPast(today: LocalDate): Boolean = today.isAfter(until)
}

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
