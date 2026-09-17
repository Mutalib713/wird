package com.mosman.wird.nudge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

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
        val systemSkin: String? = null,
    )

    fun forThisPhone(
        manufacturer: String = Build.MANUFACTURER,
        brand: String = Build.BRAND,
    ): Advice {
        val m = (manufacturer + " " + brand).lowercase()
        return when {
            // Transsion. One company, three brands, one battery manager — and the phones this
            // app was built for.
            m.contains("tecno") || m.contains("infinix") || m.contains("itel") ||
                m.contains("transsion") -> Advice(
                vendor = when {
                    m.contains("tecno") -> "Tecno"
                    m.contains("infinix") -> "Infinix"
                    m.contains("itel") -> "itel"
                    else -> "Transsion"
                },
                known = true,
                systemSkin = if (m.contains("tecno")) "HiOS" else if (m.contains("infinix")) "XOS" else "Transsion",
                steps = listOf(
                    "Open Phone Master → App Manager → Turn off freezing or auto-close for Wird.",
                    "In Settings → Battery (or Battery Lab) → Power saving → Allow Wird to run in the background.",
                    "In Auto-start management (Startup manager) → Switch Wird to Allowed.",
                ),
            )

            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> Advice(
                vendor = "Xiaomi",
                known = true,
                systemSkin = "MIUI / HyperOS",
                steps = listOf(
                    "Open Security app → Permissions → Autostart → Switch Wird ON.",
                    "In Settings → Battery → Battery saver → Choose 'No restrictions' for Wird.",
                ),
            )

            m.contains("huawei") || m.contains("honor") -> Advice(
                vendor = "Huawei",
                known = true,
                systemSkin = "EMUI / MagicOS",
                steps = listOf(
                    "Open Phone Manager → Startup manager → Set Wird to 'Manage manually' with Auto-launch and Run in background ON.",
                    "In Settings → Battery → App launch → Turn off automatic management for Wird.",
                ),
            )

            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ||
                m.contains("vivo") || m.contains("iqoo") -> Advice(
                vendor = when {
                    m.contains("oppo") -> "OPPO"
                    m.contains("realme") -> "Realme"
                    m.contains("oneplus") -> "OnePlus"
                    m.contains("vivo") -> "vivo"
                    m.contains("iqoo") -> "iQOO"
                    else -> manufacturer.lowercase().replaceFirstChar { it.uppercase() }
                },
                known = true,
                systemSkin = if (m.contains("oppo")) "ColorOS" else if (m.contains("realme")) "Realme UI" else if (m.contains("oneplus")) "OxygenOS" else "Funtouch OS",
                steps = listOf(
                    "In Settings → Battery → Allow background activity for Wird.",
                    "Find Startup manager or Auto-launch in Settings and allow Wird.",
                ),
            )

            m.contains("samsung") -> Advice(
                vendor = "Samsung",
                known = true,
                systemSkin = "One UI",
                steps = listOf(
                    "In Settings → Battery and device care → Battery → Background usage limits → Add Wird to 'Never sleeping apps'.",
                    "In Apps → Wird → Battery → Select 'Unrestricted'.",
                ),
            )

            // Everything else, including the Pixel this was developed on. Saying "your phone
            // is probably fine" is more honest than inventing a path.
            else -> Advice(
                vendor = manufacturer.lowercase().replaceFirstChar { it.uppercase() },
                known = false,
                systemSkin = "Stock Android",
                steps = listOf(
                    "This phone usually lets reminders through. If one goes missing, look in Settings → Apps → Wird → Battery, and choose 'Unrestricted'.",
                ),
            )
        }
    }

    /**
     * Attempts to create the most direct Intent to open vendor-specific auto-start or battery
     * management. Falls back gracefully to standard Android battery saver optimization settings,
     * and finally to App Details settings.
     */
    fun createBatterySettingsIntent(context: Context): Intent {
        val packageName = context.packageName
        val pm = context.packageManager

        // Candidate vendor intents to try in priority order
        val vendorIntents = listOf(
            // Transsion (Tecno / Infinix): Phone Master Auto-start
            Intent().setComponent(
                ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.autostart.AutoStartManageActivity")
            ),
            // Xiaomi: MIUI Autostart
            Intent().setComponent(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            ),
            // Huawei: Startup manager
            Intent().setComponent(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            ),
            // Oppo / Realme: Startup manager
            Intent().setComponent(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            ),
            // Vivo: Whitelist
            Intent().setComponent(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            ),
            // Standard Android Battery Saver Optimization screen
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )

        for (intent in vendorIntents) {
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            if (resolved != null) {
                return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Universal fallback: Application Details Settings
        return createAppDetailsIntent(context)
    }

    /**
     * Universal fallback opening Wird's system App Info page.
     */
    fun createAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
