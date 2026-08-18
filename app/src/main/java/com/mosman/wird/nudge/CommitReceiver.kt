package com.mosman.wird.nudge

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mosman.wird.data.ConversationStore
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.CompanionAction
import com.mosman.wird.domain.CompanionBrain
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.replyFor
import java.time.LocalDateTime

/**
 * Answering the nudge without opening the app. **PLAN task 21.**
 *
 * **This is the experiment, not a convenience.** § 2 found ~8 of 14 missed days went to
 * procrastination rather than forgetting, and every other feature addresses the smaller half.
 * The thesis here is that *naming a time to something that checks back* changes behaviour. If
 * it does not, § 5b says the companion is theatre and gets cut — so the point of this build is
 * to produce evidence, not to feel good.
 *
 * **Buttons, not typing.** A notification you can answer with one tap is answered; one that
 * needs the app opened and a sentence composed is the thing being avoided in the first place.
 * The words are the same ones [CompanionBrain] already understands, so the notification and
 * the chat cannot drift apart — the button literally sends the phrase a person would type.
 *
 * **Every reply lands in the same conversation.** Answering from the lock screen writes both
 * turns to `chat.json`, so opening the companion later shows what you said and what it
 * answered. One record, two doors into it.
 *
 * ⚠ **It re-arms a real alarm rather than remembering a promise.** Task 8's scheduler turns
 * "after Isha" into a time that moves with the sun, so the sentence lands on machinery.
 */
class CommitReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val said = intent.getStringExtra(EXTRA_SAID).orEmpty()
        if (said.isBlank()) return

        // The notification has done its job the moment it is answered. Leaving it up would
        // make the reply look like it had not registered.
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(Nudge.NOTIFICATION_ID)

        val store = WirdStore(context)
        val chat = ConversationStore(context.filesDir)
        chat.say(Speaker.YOU, said)

        when (val action = CompanionBrain.understand(said)) {
            is CompanionAction.CommitTo -> {
                store.nudgeSchedule = action.schedule
                // PLAN task 21: *every commitment is logged - what was promised, whether it
                // was kept* - because task 24 cannot measure what was never recorded.
                store.commitment = Commitment(action.spoken, LocalDateTime.now())
                NudgeScheduler.arm(context)
                Log.i(TAG, "committed to ${action.spoken}, re-armed")
            }

            // Sacred Rule 3, and the hardest place to hold it. "Not today" changes nothing:
            // no penalty, no follow-up, no row saying you failed. The reply says it is fine
            // and the app gets out of the way, because the day someone is told off is the day
            // they delete a habit app.
            is CompanionAction.NotToday -> {
                store.commitment = null
                Log.i(TAG, "not today, nothing rescheduled")
            }

            else -> Log.i(TAG, "reply not actionable from a notification: $said")
        }

        chat.say(Speaker.WIRD, replyFor(CompanionBrain.understand(said), null, ""))
    }

    companion object {
        const val EXTRA_SAID = "com.mosman.wird.SAID"
        private const val TAG = "WirdCommit"

        /**
         * The replies offered on the notification.
         *
         * ⚠ **They are phrases rather than codes on purpose:** [CompanionBrain] parses them
         * exactly as it parses typing, so a button can never mean something the chat does
         * not. That also makes the set testable — check 35 asserts every one of these is
         * understood, because a button that parses to `NotUnderstood` does nothing at all
         * and says nothing about it.
         *
         * **PLAN task 21 lists a fourth, "Tonight", and it is deliberately absent.** Three
         * reasons, in order of weight:
         *  1. `CompanionBrain` does not understand it. `decline` matches *"not tonight"*, not
         *     bare *"tonight"*, and `commit` has no rule for it — so the button would have
         *     silently done nothing. Found before wiring it, by reading the parser.
         *  2. It is redundant here. Wird's default anchor is thirty minutes after Maghrib, so
         *     a nudge arriving in the evening makes "tonight" and "after Isha" the same
         *     answer.
         *  3. Android shows three actions. A fourth would be hidden on most phones anyway.
         *
         * Giving it a meaning would have meant inventing a clock time nobody chose, which is
         * worse than offering three answers that are all real.
         */
        val REPLIES = listOf("After Isha", "In an hour", "Not today")
    }
}
