package com.mosman.wird

import android.content.Intent
import com.mosman.wird.nudge.BootReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {

    @Test
    fun recognisesAllRequiredSystemSignalActions() {
        assertTrue(BootReceiver.isSupportedAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(BootReceiver.isSupportedAction(Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(BootReceiver.isSupportedAction(Intent.ACTION_TIME_CHANGED))
        assertTrue(BootReceiver.isSupportedAction(BootReceiver.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))
    }

    @Test
    fun rejectsUnrelatedOrNullActions() {
        assertFalse(BootReceiver.isSupportedAction(null))
        assertFalse(BootReceiver.isSupportedAction("android.intent.action.SCREEN_ON"))
        assertFalse(BootReceiver.isSupportedAction("com.mosman.wird.RANDOM_ACTION"))
    }
}
