package com.mosman.wird.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The category of recitation discipline for a reading track.
 */
enum class TrackType(val label: String, val meaning: String) {
    HIFZ("Ḥifẓ", "Memorization"),
    TILAWAH("Tilāwah", "Reading"),
    REVISION("Murāja'ah", "Revision & Review"),
}

/**
 * How tracks are selected on any given day.
 * AUTOMATIC chooses based on DayOfWeek (e.g. Mon-Thu vs Sat-Sun);
 * MANUAL preserves user's explicit selection.
 */
enum class TrackScheduleMode {
    AUTOMATIC,
    MANUAL,
}

/**
 * An independent Quran journey with its own position, pace, direction, and streak.
 */
data class ReadingTrack(
    val id: String,
    val name: String,
    val type: TrackType = TrackType.HIFZ,
    /** Which days of the week this track activates when in AUTOMATIC mode. */
    val activeDays: Set<DayOfWeek> = emptySet(),
    /** Position in half-page units for this specific track */
    val positionUnit: Int = 0,
    /** Reading direction: towards An-Nas or towards Al-Fatihah */
    val direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
    /** Daily target in half-page units (2 = 1 page) */
    val dailyUnits: Int = 2,
    /** Current consecutive days streak for this track */
    val currentStreak: Int = 0,
    /** Total completed reading sessions for this track */
    val totalDaysRead: Int = 0,
    /** ISO-8601 date string (e.g. 2026-09-23) of last completion */
    val lastCompletedDate: String? = null,
    /** Optional starting Surah number */
    val startVerseSurah: Int? = null,
    /** Optional starting Ayah number */
    val startVerseAyah: Int? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("activeDays", JSONArray(activeDays.map { it.name }))
        put("positionUnit", positionUnit)
        put("direction", direction.name)
        put("dailyUnits", dailyUnits)
        put("currentStreak", currentStreak)
        put("totalDaysRead", totalDaysRead)
        put("lastCompletedDate", lastCompletedDate ?: "")
        if (startVerseSurah != null) put("startVerseSurah", startVerseSurah)
        if (startVerseAyah != null) put("startVerseAyah", startVerseAyah)
    }

    val pageNumber: Int get() = Mushaf.pageOf(positionUnit)

    fun surahName(): String {
        val page = pageNumber
        val surahs = SurahIndex.on(page)
        return surahs.firstOrNull()?.name ?: "Page $page"
    }

    fun scheduleLabel(): String {
        if (activeDays.isEmpty() || activeDays.size == 7) return "Daily"
        val monThu = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
        val satSun = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val friSat = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        return when (activeDays) {
            monThu -> "Mon–Thu"
            satSun -> "Sat–Sun"
            friSat -> "Fri–Sat"
            else -> activeDays.sortedBy { it.value }
                .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
        }
    }

    fun isDueToday(today: LocalDate = LocalDate.now()): Boolean =
        activeDays.isEmpty() || today.dayOfWeek in activeDays

    companion object {
        fun fromJson(json: JSONObject): ReadingTrack {
            val days = runCatching {
                val arr = json.optJSONArray("activeDays") ?: JSONArray()
                (0 until arr.length()).mapNotNull {
                    runCatching { DayOfWeek.valueOf(arr.getString(it)) }.getOrNull()
                }.toSet()
            }.getOrDefault(emptySet())

            val sSurah = if (json.has("startVerseSurah")) json.getInt("startVerseSurah") else null
            val sAyah = if (json.has("startVerseAyah")) json.getInt("startVerseAyah") else null

            return ReadingTrack(
                id = json.getString("id"),
                name = json.getString("name"),
                type = runCatching { TrackType.valueOf(json.getString("type")) }.getOrDefault(TrackType.HIFZ),
                activeDays = days,
                positionUnit = json.optInt("positionUnit", 0),
                direction = runCatching { ReadingDirection.valueOf(json.optString("direction", ReadingDirection.TOWARDS_NAS.name)) }.getOrDefault(ReadingDirection.TOWARDS_NAS),
                dailyUnits = json.optInt("dailyUnits", 2),
                currentStreak = json.optInt("currentStreak", 0),
                totalDaysRead = json.optInt("totalDaysRead", 0),
                lastCompletedDate = json.optString("lastCompletedDate").takeIf { it.isNotEmpty() },
                startVerseSurah = sSurah,
                startVerseAyah = sAyah,
            )
        }
    }
}

/**
 * A life context (e.g. "Home", "School", "Ramadan") containing parallel reading tracks.
 * When frozen, all its internal track streaks remain protected and dormant.
 */
data class LifeSpace(
    val id: String,
    val name: String,
    val isFrozen: Boolean = false,
    val tracks: List<ReadingTrack> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isFrozen", isFrozen)
        val arr = JSONArray()
        tracks.forEach { arr.put(it.toJson()) }
        put("tracks", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): LifeSpace {
            val trackList = mutableListOf<ReadingTrack>()
            val arr = json.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { trackList.add(ReadingTrack.fromJson(it)) }
            }
            return LifeSpace(
                id = json.getString("id"),
                name = json.getString("name"),
                isFrozen = json.optBoolean("isFrozen", false),
                tracks = trackList,
            )
        }
    }
}
