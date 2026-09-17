package com.mosman.wird.nudge

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.NudgeSchedule
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Diagnostic tool that checks whether reminders are firing and surviving phone battery managers.
 *
 * **PLAN task 15:** Answers "did the last nudge arrive?" by comparing [WirdStore.lastArmedFor]
 * against [WirdStore.lastNudgeFiredAt], alongside checking notification permissions, exact alarm
 * privileges, and battery optimization exemptions.
 */
object NudgeDiagnostic {

    enum class Status {
        OK,
        WARNING,
        ERROR,
    }

    data class CheckItem(
        val name: String,
        val detail: String,
        val status: Status,
    )

    data class Report(
        val overallStatus: Status,
        val headline: String,
        val explanation: String,
        val checks: List<CheckItem>,
    )

    /**
     * Pure functional evaluation that can be verified in JVM unit tests without Android runtime dependencies.
     */
    fun evaluate(
        notificationsEnabled: Boolean,
        exactAlarmsAllowed: Boolean,
        isBatteryOptimized: Boolean,
        isScheduleOff: Boolean,
        lastArmedFor: LocalDateTime?,
        lastNudgeFiredAt: LocalDateTime?,
        now: LocalDateTime = LocalDateTime.now(),
    ): Report {
        val checks = mutableListOf<CheckItem>()

        // 1. Notification permission
        val notifStatus = if (notificationsEnabled) Status.OK else Status.ERROR
        checks.add(
            CheckItem(
                name = "Notifications",
                detail = if (notificationsEnabled) {
                    "Allowed by system settings"
                } else {
                    "Blocked in system settings — reminders cannot show"
                },
                status = notifStatus,
            )
        )

        // 2. Exact alarms
        val exactStatus = if (exactAlarmsAllowed) Status.OK else Status.WARNING
        checks.add(
            CheckItem(
                name = "Exact Alarm Timing",
                detail = if (exactAlarmsAllowed) {
                    "Alarms fire at the exact scheduled minute"
                } else {
                    "Inexact alarms only — Android may delay reminders by 10–30 min"
                },
                status = exactStatus,
            )
        )

        // 3. Battery saver / optimization
        val batteryStatus = if (!isBatteryOptimized) Status.OK else Status.WARNING
        checks.add(
            CheckItem(
                name = "Battery Optimization",
                detail = if (!isBatteryOptimized) {
                    "Unrestricted — phone will not freeze or kill Wird"
                } else {
                    "Optimized — phone battery manager may kill the reminder"
                },
                status = batteryStatus,
            )
        )

        // 4. Schedule & Armed Alarm
        val scheduleStatus = when {
            isScheduleOff -> Status.WARNING
            lastArmedFor == null -> Status.WARNING
            else -> Status.OK
        }
        val scheduleDetail = when {
            isScheduleOff -> "Reminders are turned off in settings"
            lastArmedFor == null -> "No reminder has been scheduled yet"
            lastArmedFor.isAfter(now) -> {
                val timeStr = lastArmedFor.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                val dateStr = lastArmedFor.toLocalDate().toString()
                "Next reminder armed for $timeStr on $dateStr"
            }
            else -> "Reminder was armed for ${lastArmedFor.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        checks.add(
            CheckItem(
                name = "Scheduled Alarm",
                detail = scheduleDetail,
                status = scheduleStatus,
            )
        )

        // 5. Last Reminder Arrival check
        val deliveryCheck: CheckItem
        val deliveryExplanation: String

        if (isScheduleOff) {
            deliveryCheck = CheckItem(
                name = "Last Reminder Arrival",
                detail = "Reminder is currently turned off",
                status = Status.OK,
            )
            deliveryExplanation = "Reminders are turned off by choice."
        } else if (lastArmedFor == null) {
            deliveryCheck = CheckItem(
                name = "Last Reminder Arrival",
                detail = "No previous alarm on record",
                status = Status.OK,
            )
            deliveryExplanation = "Waiting for the first scheduled reminder."
        } else if (lastArmedFor.isAfter(now)) {
            val timeStr = lastArmedFor.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            deliveryCheck = CheckItem(
                name = "Last Reminder Arrival",
                detail = "Waiting for upcoming alarm at $timeStr",
                status = Status.OK,
            )
            deliveryExplanation = "The next reminder is scheduled for $timeStr."
        } else {
            // lastArmedFor is in the past
            val firedRecently = lastNudgeFiredAt != null &&
                !lastNudgeFiredAt.isBefore(lastArmedFor.minusMinutes(15))

            if (firedRecently) {
                val firedStr = lastNudgeFiredAt!!.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                deliveryCheck = CheckItem(
                    name = "Last Reminder Arrival",
                    detail = "Fired on time at $firedStr",
                    status = Status.OK,
                )
                deliveryExplanation = "Your last reminder woke the app on schedule at $firedStr."
            } else {
                val minutesLate = Duration.between(lastArmedFor, now).toMinutes()
                if (minutesLate > 20) {
                    val armedStr = lastArmedFor.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    deliveryCheck = CheckItem(
                        name = "Last Reminder Arrival",
                        detail = "Alarm at $armedStr did not fire (killed by phone)",
                        status = Status.ERROR,
                    )
                    deliveryExplanation = "The alarm set for $armedStr was missed. Your phone's battery saver likely killed Wird in the background."
                } else {
                    val armedStr = lastArmedFor.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    deliveryCheck = CheckItem(
                        name = "Last Reminder Arrival",
                        detail = "Alarm scheduled for $armedStr (triggering now)",
                        status = Status.OK,
                    )
                    deliveryExplanation = "Alarm scheduled for $armedStr; waiting for delivery."
                }
            }
        }
        checks.add(deliveryCheck)

        // Compute overall verdict
        val overallStatus = when {
            checks.any { it.status == Status.ERROR } -> Status.ERROR
            checks.any { it.status == Status.WARNING } -> Status.WARNING
            else -> Status.OK
        }

        val headline = when (overallStatus) {
            Status.OK -> "Reminder health is good"
            Status.WARNING -> "Action recommended for reliable reminders"
            Status.ERROR -> "Reminders are being blocked or killed"
        }

        return Report(
            overallStatus = overallStatus,
            headline = headline,
            explanation = deliveryExplanation,
            checks = checks,
        )
    }

    /**
     * Evaluates live Android system state.
     */
    fun evaluate(context: Context, store: WirdStore, now: LocalDateTime = LocalDateTime.now()): Report {
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exactAlarmsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
        val powerManager = context.getSystemService(PowerManager::class.java)
        val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        val isBatteryOptimized = !isIgnoringBattery
        val isScheduleOff = store.scheduleFor(now.toLocalDate()) is NudgeSchedule.Off

        return evaluate(
            notificationsEnabled = notificationsEnabled,
            exactAlarmsAllowed = exactAlarmsAllowed,
            isBatteryOptimized = isBatteryOptimized,
            isScheduleOff = isScheduleOff,
            lastArmedFor = store.lastArmedFor,
            lastNudgeFiredAt = store.lastNudgeFiredAt,
            now = now,
        )
    }
}
