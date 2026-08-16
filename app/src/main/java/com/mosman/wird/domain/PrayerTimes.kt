package com.mosman.wird.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * The five daily prayers, in the order they fall.
 *
 * These exist here for one reason only: to give the nudge something to hang off that
 * moves with the sun instead of sitting at a fixed clock hour. PROFILE.md § 5 keeps
 * prayer times out of v1 as a feature — there is no timetable screen and there will not
 * be one. The user picks which prayer their reminder follows, and that is the whole of
 * it.
 */
enum class Prayer(val label: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha"),
}

/** A place on Earth, in degrees. North and East are positive. */
data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}

/**
 * One day's times, in local wall-clock time.
 *
 * A prayer is null when the sun never reaches the required angle that day — which
 * happens above roughly 48° of latitude in midsummer, and is why the scheduler always
 * has somewhere else to go. Ghana never sees it; a tester in Europe in June would.
 */
data class DayTimes(
    val prayers: Map<Prayer, LocalTime?>,
    val sunrise: LocalTime?,
) {
    operator fun get(prayer: Prayer): LocalTime? = prayers[prayer]
}

/**
 * How the twilight prayers are defined. Different bodies fix the sun's depression angle
 * differently, and the answers differ by up to twenty minutes for Fajr.
 *
 * **Measured 2026-08-16, and the reason this choice is nearly harmless here:** across
 * MWL, ISNA, Umm al-Qura, Egyptian and Gulf, Maghrib in Accra came out at 18:14 in every
 * single one. Maghrib is sunset, and sunset is astronomy rather than convention. Only
 * Fajr (04:38–04:57) and Isha (19:13–19:44) move. So anchoring the nudge on Maghrib —
 * the default — makes this setting almost irrelevant, and it only starts to matter for
 * someone who moves their reminder to Fajr or Isha.
 *
 * [asrShadowFactor] 1 is the Shafi'i and Maliki reckoning; Hanafi uses 2. Ghana is
 * predominantly Maliki, so 1 is the sane default.
 */
data class PrayerMethod(
    val fajrAngle: Double = 18.0,
    val ishaAngle: Double = 17.0,
    val asrShadowFactor: Int = 1,
) {
    companion object {
        /** Muslim World League. The most widely used, and what Aladhan calls method 3. */
        val MUSLIM_WORLD_LEAGUE = PrayerMethod()
    }
}

/**
 * Prayer times computed on the phone, from the date and a latitude/longitude.
 *
 * **Why compute rather than fetch.** PROFILE.md § 7 says no backend and § 10 says Ghana
 * mobile data is a first-class concern. This is a few dozen lines of solar geometry that
 * has been settled since the 1990s, it needs no key and no network, and it works with the
 * phone in airplane mode a year from now. An API would cost a request a day, forever, for
 * arithmetic the phone can do in under a millisecond.
 *
 * **Verified against a known source, 2026-08-16.** Checked against the Aladhan API
 * (Muslim World League) for Accra and Kumasi across both solstices and today's date:
 * all 24 values matched to the minute, with zero error. The QA suite holds those cases.
 *
 * The formulation is the standard one published by PrayTimes.org, which is what Aladhan
 * itself implements. Everything is in degrees, because every published version of these
 * formulas is written that way and translating them into radians is how transcription
 * bugs get in.
 */
object PrayerTimes {

    /**
     * Refraction plus the sun's own width: the sun's centre is 0.833° below the horizon
     * at the moment its upper edge appears to touch it. This is what makes sunset a
     * definable instant rather than a matter of taste.
     */
    private const val HORIZON_ANGLE = 0.833

    /**
     * Each pass refines the estimate of what time of day each prayer falls, which feeds
     * back into the sun's position for that moment. One pass is what PrayTimes ships and
     * it left Asr a minute out; three passes matched Aladhan exactly on every case. The
     * whole thing is well under a millisecond, so there is nothing to save by stopping
     * early.
     */
    private const val ITERATIONS = 3

    fun compute(
        date: LocalDate,
        at: Coordinates,
        zone: ZoneId,
        method: PrayerMethod = PrayerMethod.MUSLIM_WORLD_LEAGUE,
    ): DayTimes {
        // The offset for this date, not for today — so a schedule computed in advance
        // still lands correctly across a daylight-saving change. Ghana has none, but the
        // testers this app might reach one day are not all in Ghana.
        val utcOffsetHours =
            zone.rules.getOffset(date.atStartOfDay()).totalSeconds / 3600.0

        val lat = at.latitude
        val lng = at.longitude

        // Longitude is folded into the Julian date so the sun's position is evaluated
        // for local noon rather than for Greenwich noon.
        val jDate = julianDay(date) - lng / (15.0 * 24.0)

        fun midDay(t: Double) = fixHour(12.0 - sunPosition(jDate + t).equationOfTime)

        /**
         * The time the sun sits [angle] degrees below the horizon, before noon
         * ([beforeNoon] true) or after it. NaN when it never gets that low.
         */
        fun sunAngleTime(angle: Double, t: Double, beforeNoon: Boolean = false): Double {
            val decl = sunPosition(jDate + t).declination
            val inner = (-sinD(angle) - sinD(decl) * sinD(lat)) / (cosD(decl) * cosD(lat))
            if (abs(inner) > 1.0) return Double.NaN
            val halfArc = acosD(inner) / 15.0
            val noon = midDay(t)
            return if (beforeNoon) noon - halfArc else noon + halfArc
        }

        /** Asr: when an object's shadow is its own length (plus its noon shadow) longer. */
        fun asrTime(t: Double): Double {
            val decl = sunPosition(jDate + t).declination
            val angle = -acotD(method.asrShadowFactor + tanD(abs(lat - decl)))
            return sunAngleTime(angle, t)
        }

        /**
         * Refine one time, starting from a rough guess at the hour.
         *
         * Each prayer depends only on its own previous estimate — never on the others —
         * so they refine independently, and one that cannot be computed drops out
         * cleanly as NaN instead of contaminating the rest.
         */
        fun refine(seedHour: Double, step: (Double) -> Double): Double {
            // Held as a fraction of a day, because that is the unit the sun-position
            // formula wants.
            var t = seedHour / 24.0
            repeat(ITERATIONS) {
                val next = step(t)
                if (next.isNaN()) return Double.NaN
                t = next / 24.0
            }
            return t * 24.0
        }

        // Everything below is in solar time at this longitude; this shifts it onto the
        // clock the phone actually shows.
        val shift = utcOffsetHours - lng / 15.0
        fun clock(hours: Double): LocalTime? = toLocalTime(hours + shift)

        return DayTimes(
            prayers = mapOf(
                Prayer.FAJR to clock(
                    refine(5.0) { sunAngleTime(method.fajrAngle, it, beforeNoon = true) }
                ),
                Prayer.DHUHR to clock(refine(12.0) { midDay(it) }),
                Prayer.ASR to clock(refine(13.0) { asrTime(it) }),
                Prayer.MAGHRIB to clock(refine(18.0) { sunAngleTime(HORIZON_ANGLE, it) }),
                Prayer.ISHA to clock(refine(18.0) { sunAngleTime(method.ishaAngle, it) }),
            ),
            sunrise = clock(
                refine(6.0) { sunAngleTime(HORIZON_ANGLE, it, beforeNoon = true) }
            ),
        )
    }

    // ---- solar geometry ----

    private class Sun(val declination: Double, val equationOfTime: Double)

    /**
     * Where the sun is on Julian day [jd].
     *
     * *Declination* is how far north or south of the equator the sun stands — the thing
     * that makes days long in June and short in December. *Equation of time* is the gap
     * between clock noon and actual solar noon, up to about sixteen minutes, caused by
     * the Earth's orbit being an ellipse rather than a circle.
     */
    private fun sunPosition(jd: Double): Sun {
        val d = jd - 2451545.0
        val meanAnomaly = fixAngle(357.529 + 0.98560028 * d)
        val meanLongitude = fixAngle(280.459 + 0.98564736 * d)
        val eclipticLongitude = fixAngle(
            meanLongitude + 1.915 * sinD(meanAnomaly) + 0.020 * sinD(2 * meanAnomaly)
        )
        val obliquity = 23.439 - 0.00000036 * d

        val rightAscension =
            atan2D(cosD(obliquity) * sinD(eclipticLongitude), cosD(eclipticLongitude)) / 15.0

        return Sun(
            declination = asinD(sinD(obliquity) * sinD(eclipticLongitude)),
            equationOfTime = meanLongitude / 15.0 - fixHour(rightAscension),
        )
    }

    /** Days since 4713 BC, the calendar astronomers use so no month arithmetic is needed. */
    private fun julianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) +
            floor(30.6001 * (month + 1)) +
            date.dayOfMonth + b - 1524.5
    }

    /**
     * Hours past midnight to a wall-clock time, rounded to the nearest minute.
     *
     * Wraps rather than overflowing: an Isha late enough to fall after midnight comes
     * back as an early-morning time, and the scheduler is what decides which day that
     * belongs to.
     */
    private fun toLocalTime(hours: Double): LocalTime? {
        if (hours.isNaN()) return null
        val minutes = Math.floorMod(Math.round(hours * 60.0).toInt(), 24 * 60)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    // ---- degree trigonometry ----
    //
    // Named rather than inlined so the formulas above read the way they are published.

    private fun sinD(d: Double) = sin(Math.toRadians(d))
    private fun cosD(d: Double) = cos(Math.toRadians(d))
    private fun tanD(d: Double) = tan(Math.toRadians(d))
    private fun asinD(x: Double) = Math.toDegrees(asin(x))
    private fun acosD(x: Double) = Math.toDegrees(acos(x))
    private fun atan2D(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun acotD(x: Double) = Math.toDegrees(atan(1.0 / x))

    /** Wraps [a] into 0 until [b], for angles that ran past 360 and hours past 24. */
    private fun fix(a: Double, b: Double): Double {
        val r = a - b * floor(a / b)
        return if (r < 0) r + b else r
    }

    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
}
