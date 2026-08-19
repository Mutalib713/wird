package com.mosman.wird.domain

/**
 * What the app is willing to say about a recording. **PLAN task 14, cheap half.**
 *
 * ⚠ **Read [Heard.RECITATION] as "this sounds like someone reciting", never as "this is the
 * right passage, recited correctly".** The difference is the whole honesty of this file.
 * Nothing here listens to *words*. It measures loudness over time, which separates a real
 * recitation from silence, from a pocket, and from three seconds of throat-clearing — and
 * cannot separate it from a phone call, a conversation, or the radio.
 *
 * That ceiling is stated in the enum, in the reply the reader sees, and here, because a check
 * that quietly implies more than it verified is worse than no check at all. Sacred Rule 6 says
 * the app must never let anyone believe they recited more than they did — including the app
 * itself.
 */
enum class Heard {
    /** Sustained sound, long enough to plausibly be the portion. Not "correct". */
    RECITATION,

    /** The microphone heard almost nothing. A pocket, a muted mic, or a silent room. */
    TOO_QUIET,

    /** Loud enough, but over too soon to be a portion. The record-and-tap case. */
    TOO_SHORT,

    /** No samples at all — the recording never really started. */
    NOTHING,
}

/**
 * The measurement behind the verdict, kept so it can be shown and argued with.
 *
 * PLAN task 14 asks for *"the measured numbers written down"*, and a verdict with no numbers
 * behind it is exactly the thing that cannot be argued with when it is wrong.
 */
data class HeardResult(
    val verdict: Heard,
    /** Share of seconds carrying sound above the speech floor, 0f..1f. */
    val spokenShare: Float,
    /** Seconds that carried speech, which is not the same as the recording's length. */
    val spokenSeconds: Int,
    val totalSeconds: Int,
)

/**
 * Judge a recording from its loudness, one sample per second.
 *
 * **Why this exists instead of whisper.** Task 14 was written around whisper.cpp and the
 * Tarteel Qur'an model, and probing it on 2026-08-19 found the cost before a line was written:
 * no GGML build of that model exists (290 MB of PyTorch only), the NDK is not installed, and
 * the shipped model would be **45–57 MB against a 17.7 MB app** on Ghanaian mobile data.
 *
 * The task's own done-when, though, is narrower than transcription: *"reliably separates a real
 * recitation from silence and from unrelated speech."* The first half of that is a loudness
 * problem, and loudness costs nothing. So this ships first and the numbers it produces decide
 * whether the 50 MB is worth it — which is what a ⚠ task is for.
 *
 * ⚠ **Every threshold here is provisional until measured on Mutalib's own voice, on his phone,
 * in his room.** They are starting points chosen to be *forgiving*, because the cost of the two
 * errors is not symmetric: wrongly telling someone their recitation did not count is the kind of
 * thing that makes a person delete a habit app, while wrongly accepting a poor recording costs
 * nothing anyone can feel. Sacred Rule 3 decides which way to lean when the data is thin.
 *
 * @param levels peak amplitude per second, as `MediaRecorder.getMaxAmplitude()` reports it —
 *   0 to 32767. One entry per second of recording.
 */
fun judgeRecitation(
    levels: List<Int>,
    floor: Int = SPEECH_FLOOR,
    minimumSeconds: Int = MIN_SPOKEN_SECONDS,
    minimumShare: Float = MIN_SPOKEN_SHARE,
): HeardResult {
    if (levels.isEmpty()) {
        return HeardResult(Heard.NOTHING, spokenShare = 0f, spokenSeconds = 0, totalSeconds = 0)
    }

    val spoken = levels.count { it >= floor }
    val share = spoken.toFloat() / levels.size

    // Order matters, and it is the order of how obvious the problem is. A recording that is
    // silent throughout is silent whether it ran for four seconds or four minutes, so "too
    // quiet" is answered before "too short" — otherwise a silent two-second recording gets
    // told its length was the problem.
    val verdict = when {
        share < minimumShare -> Heard.TOO_QUIET
        spoken < minimumSeconds -> Heard.TOO_SHORT
        else -> Heard.RECITATION
    }

    return HeardResult(
        verdict = verdict,
        spokenShare = share,
        spokenSeconds = spoken,
        totalSeconds = levels.size,
    )
}

/**
 * Above this, a second counts as carrying speech.
 *
 * `getMaxAmplitude` runs 0–32767. Ordinary speech at arm's length peaks in the thousands and a
 * quiet room floors in the low hundreds, so this sits well below speech and well above room
 * tone. ⚠ **Unmeasured on his phone.** It is the first number task 14 has to check.
 */
const val SPEECH_FLOOR = 1_200

/** Fewer spoken seconds than this is not a portion, whatever else it is. */
const val MIN_SPOKEN_SECONDS = 10

/** Below this share of the recording carrying sound, the microphone heard a room. */
const val MIN_SPOKEN_SHARE = 0.30f

/**
 * What to say about it, in the second person and without an accusation in it.
 *
 * Sacred Rule 3 governs this the way it governs the companion: the reader has just recited,
 * and being told a machine is unconvinced is the worst moment in the app to sound like a
 * gatekeeper. So a doubt is phrased as **something the app could not hear**, which is the
 * literal truth, rather than as something the reader failed to do.
 */
fun heardLabel(result: HeardResult): String = when (result.verdict) {
    Heard.RECITATION -> "Recorded, ${result.spokenSeconds}s of recitation."
    Heard.TOO_QUIET -> "Saved, but I could barely hear it. Closer to the mic next time?"
    Heard.TOO_SHORT -> "Saved, ${result.spokenSeconds}s. Shorter than a portion usually takes."
    Heard.NOTHING -> "Saved, but nothing came through the microphone."
}
