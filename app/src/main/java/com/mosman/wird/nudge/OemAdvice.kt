package com.mosman.wird.nudge

import android.os.Build

/**
 * What to change on *this* phone so the reminder survives. **PLAN task 15, the buildable half.**
 *
 * **Why this exists at all.** Android's alarm API is not the problem; the manufacturer's
 * battery software is. Transsion phones — TECNO, Infinix, itel — ship with background freezing
 * on by default, and those are exactly the phones Wird's testers carry. An app that says
 * nothing looks broken; an app that says *"open Phone Master and allow this"* looks like it
 * knows where it is.
 *
 * ⚠ **The menu names drift between versions and regions**, which is why every entry ends by
 * telling you what to look for rather than promising an exact path. Advice that names a screen
 * which is not there is worse than advice that describes the setting.
 *
 * ⚠ **None of this has been verified on a real Transsion phone.** It cannot be from here — the
 * emulator reports `Build.MANUFACTURER = "Google"`. The routes below come from the vendors' own
 * settings apps as documented publicly; the *detection* is testable and the *steps* are not.
 * PLAN task 15 stays open until it runs on an actual Tecno or Infinix.
 */
object OemAdvice {

    /** What to say, and whether this phone is one of the known-difficult ones. */
    data class Advice(
        val vendor: String,
        val known: Boolean,
        val steps: List<String>,
    )

    fun forThisPhone(
        manufacturer: String = Build.MANUFACTURER,
        brand: String = Build.BRAND,
    ): Advice {
        val m = (manufacturer + " " + brand).lowercase()
        return when {
            // Transsion. One company, three brands, one battery manager - and the phones this
            // app was built for.
            m.contains("tecno") || m.contains("infinix") || m.contains("itel") ||
                m.contains("transsion") -> Advice(
                vendor = manufacturer.replaceFirstChar { it.uppercase() },
                known = true,
                steps = listOf(
                    "Open Phone Master, then App Manager, and turn off any freezing or " +
                        "auto-close for Wird.",
                    "In Settings, find Battery, then Background freeze or Power saving, and " +
                        "let Wird run in the background.",
                    "Look for Auto-start or Startup manager and allow Wird there too.",
                ),
            )

            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> Advice(
                vendor = "Xiaomi",
                known = true,
                steps = listOf(
                    "Open Security, then Permissions, then Autostart, and switch Wird on.",
                    "In Settings, Battery, choose No restrictions for Wird.",
                ),
            )

            m.contains("huawei") || m.contains("honor") -> Advice(
                vendor = "Huawei",
                known = true,
                steps = listOf(
                    "Open Phone Manager, then Startup manager, and set Wird to Manage manually " +
                        "with all three switches on.",
                    "In Battery settings, turn off any optimisation for Wird.",
                ),
            )

            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ||
                m.contains("vivo") || m.contains("iqoo") -> Advice(
                vendor = manufacturer.replaceFirstChar { it.uppercase() },
                known = true,
                steps = listOf(
                    "In Settings, Battery, allow background activity for Wird.",
                    "Find Startup manager or Auto-launch and allow Wird there.",
                ),
            )

            m.contains("samsung") -> Advice(
                vendor = "Samsung",
                known = true,
                steps = listOf(
                    "In Settings, Battery, open Background usage limits and add Wird to " +
                        "Never sleeping apps.",
                ),
            )

            // Everything else, including the Pixel this was developed on. Saying "your phone
            // is probably fine" is more honest than inventing a path.
            else -> Advice(
                vendor = manufacturer.replaceFirstChar { it.uppercase() },
                known = false,
                steps = listOf(
                    "This phone usually lets reminders through. If one goes missing, look in " +
                        "Settings, Battery, and allow Wird to run in the background.",
                ),
            )
        }
    }
}
