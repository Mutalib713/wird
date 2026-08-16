package com.mosman.wird.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.mosman.wird.domain.Coordinates
import java.time.ZoneId

/** How we came to believe we are here. */
enum class PlaceSource {
    /** A real fix from the phone. */
    LOCATION,

    /** The phone's timezone, matched to a city. Good to a few minutes. */
    TIMEZONE,
}

data class Place(val coordinates: Coordinates, val source: PlaceSource)

/**
 * Where the phone is, for the one purpose of timing the nudge.
 *
 * **Mutalib chose coarse location on 2026-08-16**, over deriving it from the timezone,
 * because he moves between Pig Farm in Accra and KNUST in Kumasi and those are seven
 * minutes apart at Maghrib.
 *
 * Three things this deliberately does *not* do, all of them Sacred Rule 1:
 * the coordinates never leave the phone; they are stored rounded to two decimals, which
 * is about a kilometre and plenty for sunset; and location is asked for **once** and then
 * remembered, so the app is not reaching for your whereabouts every morning to do
 * arithmetic it could have done from a number it already had.
 */
object Where {

    private const val PREFS = "wird_place"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"
    private const val KEY_FIXED_AT = "fixed_at"

    /** A fix older than this is worth quietly replacing next time the app opens. */
    private const val STALE_AFTER_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

    /** Long enough for a network fix, short enough that nothing waits on it. */
    private const val FIX_TIMEOUT_MS = 20_000L

    const val PERMISSION: String = Manifest.permission.ACCESS_COARSE_LOCATION

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * The best coordinates available right now, without blocking and without asking
     * Android for anything.
     *
     * Null means we genuinely do not know, and the caller must say so rather than guess.
     * Inventing a latitude is the one thing that would be worse than admitting it: a
     * plausible wrong sunset is indistinguishable from a right one until the reminder
     * arrives in the middle of a lecture.
     */
    fun best(context: Context): Place? {
        stored(context)?.let { return Place(it, PlaceSource.LOCATION) }
        fromTimezone(ZoneId.systemDefault())?.let { return Place(it, PlaceSource.TIMEZONE) }
        return null
    }

    /**
     * Ask the phone where it is and remember the answer.
     *
     * Does nothing without permission — the permission prompt belongs to the screen that
     * can explain itself, not to a utility. [then] runs on the main thread when something
     * new was stored, and never runs at all if nothing changed.
     */
    fun refresh(context: Context, then: (Place) -> Unit) {
        if (!hasPermission(context)) return
        if (!isStale(context)) return

        val lm = context.getSystemService(LocationManager::class.java) ?: return

        // Cheapest first: a fix some other app already paid for. On most phones this
        // returns immediately and nothing further is needed.
        lastKnown(context, lm)?.let {
            store(context, it)
            then(Place(it, PlaceSource.LOCATION))
            return
        }

        requestOneFix(context, lm) { coordinates ->
            store(context, coordinates)
            then(Place(coordinates, PlaceSource.LOCATION))
        }
    }

    // ---- reading the phone ----

    private fun lastKnown(context: Context, lm: LocationManager): Coordinates? {
        if (!hasPermission(context)) return null
        // Network before GPS on purpose. A coarse fix is all sunset needs, it arrives
        // indoors, and it does not wake the GPS radio on a phone whose battery the
        // testers are already protective of.
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
        return providers
            .mapNotNull { provider ->
                // Caught explicitly rather than swallowed by runCatching. Permission can
                // be revoked between the check above and this line — a real race on
                // Android, not a theoretical one — and a location we cannot have is a
                // reason to fall back, never a reason to crash on someone's phone.
                try {
                    lm.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null // provider not present on this device
                }
            }
            .maxByOrNull { it.time }
            ?.let { Coordinates(round2(it.latitude), round2(it.longitude)) }
    }

    /**
     * One fix, then stop listening.
     *
     * `requestLocationUpdates` rather than `getCurrentLocation`, which only exists from
     * API 30 — this app's floor is 26. The listener removes itself on the first result,
     * and a timeout removes it if none arrives, because a location listener left running
     * is a battery complaint waiting to happen on exactly the phones we care about.
     */
    private fun requestOneFix(context: Context, lm: LocationManager, then: (Coordinates) -> Unit) {
        if (!hasPermission(context)) return
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> return
        }

        val handler = Handler(Looper.getMainLooper())
        var done = false

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (done) return
                done = true
                runCatching { lm.removeUpdates(this) }
                Log.i(TAG, "location fix from $provider")
                then(Coordinates(round2(location.latitude), round2(location.longitude)))
            }

            // Present because on API 26–29 these are abstract on the framework's own
            // interface. They gained default bodies in API 30, so compiling against 36
            // alone would leave a device on Android 8 throwing AbstractMethodError.
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit

            // Deprecated in API 29 and still called below it, which is exactly the range
            // this override exists for. Suppressed rather than annotated: marking it
            // @Deprecated would push the warning onto every caller of an anonymous object
            // nobody calls.
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        try {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            return // revoked since the check above; the fallback already covers this
        } catch (_: IllegalArgumentException) {
            return // provider vanished between isProviderEnabled and here
        }

        handler.postDelayed({
            if (done) return@postDelayed
            done = true
            runCatching { lm.removeUpdates(listener) }
            Log.i(TAG, "no location fix within ${FIX_TIMEOUT_MS}ms, keeping the timezone guess")
        }, FIX_TIMEOUT_MS)
    }

    // ---- remembering it ----

    private fun stored(context: Context): Coordinates? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT)) return null
        return runCatching {
            Coordinates(
                latitude = prefs.getFloat(KEY_LAT, 0f).toDouble(),
                longitude = prefs.getFloat(KEY_LNG, 0f).toDouble(),
            )
        }.getOrNull()
    }

    private fun store(context: Context, at: Coordinates) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putFloat(KEY_LAT, at.latitude.toFloat())
            putFloat(KEY_LNG, at.longitude.toFloat())
            putLong(KEY_FIXED_AT, System.currentTimeMillis())
        }
    }

    private fun isStale(context: Context): Boolean {
        val at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_FIXED_AT, 0L)
        return System.currentTimeMillis() - at > STALE_AFTER_MS
    }

    /** ~1 km. Sunset does not need more, and the app should not hold more. */
    private fun round2(degrees: Double) = Math.round(degrees * 100.0) / 100.0

    // ---- the fallback ----

    /**
     * A city for the phone's timezone.
     *
     * This is deliberately a short list rather than a world atlas. It covers Ghana, where
     * every tester is, and its neighbours. **An unlisted timezone returns null rather
     * than a guess** — longitude can be inferred from a UTC offset but latitude cannot,
     * and latitude is what decides how far sunset moves through the year. A wrong
     * latitude would produce a reminder that is confidently an hour out in December.
     *
     * This only ever applies before a real fix arrives, or if location is refused.
     */
    private fun fromTimezone(zone: ZoneId): Coordinates? = when (zone.id) {
        "Africa/Accra" -> Coordinates(5.60, -0.19) // Accra
        "Africa/Abidjan" -> Coordinates(5.36, -4.01) // Abidjan
        "Africa/Lome" -> Coordinates(6.13, 1.23) // Lomé
        "Africa/Ouagadougou" -> Coordinates(12.37, -1.52) // Ouagadougou
        "Africa/Lagos" -> Coordinates(6.52, 3.38) // Lagos
        else -> null
    }

    private const val TAG = "WirdWhere"
}
