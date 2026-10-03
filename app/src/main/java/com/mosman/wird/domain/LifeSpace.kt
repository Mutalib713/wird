package com.mosman.wird.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The category of recitation discipline for a reading track.
 * Bilingual: English primary, Arabic terminology secondary underneath.
 */
enum class TrackType(
    val englishLabel: String,
    val arabicLabel: String,
    val description: String,
) {
    TILAWAH("Read", "Tilāwah", "For regularly reading Qur'an"),
    HIFZ("Memorize", "Ḥifẓ", "For learning new verses by heart"),
    REVISION("Review", "Murāja'ah", "For revising what you've already memorized");

    val label: String get() = englishLabel
    val meaning: String get() = description
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
    /** Meaningful intention/purpose anchor (e.g. "Build a daily habit", "Memorize new verses") */
    val intention: String? = null,
    /** Custom target verse count when custom verses target is configured */
    val customTargetVerses: Int? = null,
    /** Which days of the week this track activates when in AUTOMATIC mode. */
    val activeDays: Set<DayOfWeek> = emptySet(),
    /** Position in half-page units for this specific track */
    val positionUnit: Int = 0,
    /** Reading direction: towards An-Nas or towards Al-Fatihah */
    val direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
    /** Daily target in half-page units (2 = 1 page) */
    val dailyUnits: Int = 2,
    /**
     * Weekday exceptions to [dailyUnits], in half-page units: "half on Fridays" is
     * `FRIDAY to 1`. Added 2026-10-03; before that they lived in a global plan the portion
     * never read, so the companion said yes and nothing changed.
     */
    val weekdayUnits: Map<DayOfWeek, Int> = emptyMap(),
    /**
     * **Derived, never saved.** Filled from the day log by [withProgress]. See TrackRecord.kt
     * for why: a stored copy of these drifted from the log and showed the wrong streak.
     */
    val currentStreak: Int = 0,
    /** Derived, never saved. See [currentStreak]. */
    val totalDaysRead: Int = 0,
    /** Derived, never saved: the last date this track has a row in the day log. */
    val lastCompletedDate: String? = null,
    /** Optional starting Surah number */
    val startVerseSurah: Int? = null,
    /** Optional starting Ayah number */
    val startVerseAyah: Int? = null,
    /** Optional independent reminder schedule for this track */
    val reminderScheduleRaw: String? = null,
    /** When frozen, track streak remains protected and dormant */
    val isFrozen: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        if (intention != null) put("intention", intention)
        if (customTargetVerses != null) put("customTargetVerses", customTargetVerses)
        put("activeDays", JSONArray(activeDays.map { it.name }))
        put("positionUnit", positionUnit)
        put("direction", direction.name)
        put("dailyUnits", dailyUnits)
        if (weekdayUnits.isNotEmpty()) {
            put("weekdayUnits", JSONObject().apply { weekdayUnits.forEach { (d, u) -> put(d.name, u) } })
        }
        // currentStreak, totalDaysRead and lastCompletedDate are deliberately not written:
        // the day log is the record, and a second copy is what drifted.
        if (startVerseSurah != null) put("startVerseSurah", startVerseSurah)
        if (startVerseAyah != null) put("startVerseAyah", startVerseAyah)
        if (reminderScheduleRaw != null) put("reminderScheduleRaw", reminderScheduleRaw)
        put("isFrozen", isFrozen)
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

    fun isCompletedToday(today: LocalDate = LocalDate.now()): Boolean =
        lastCompletedDate == today.toString()

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
            val intention = json.optString("intention").takeIf { it.isNotEmpty() }
            val customTargetVerses = if (json.has("customTargetVerses")) json.getInt("customTargetVerses") else null

            return ReadingTrack(
                id = json.getString("id"),
                name = json.getString("name"),
                type = runCatching { TrackType.valueOf(json.getString("type")) }.getOrDefault(TrackType.HIFZ),
                intention = intention,
                customTargetVerses = customTargetVerses,
                activeDays = days,
                positionUnit = json.optInt("positionUnit", 0),
                direction = runCatching { ReadingDirection.valueOf(json.optString("direction", ReadingDirection.TOWARDS_NAS.name)) }.getOrDefault(ReadingDirection.TOWARDS_NAS),
                dailyUnits = json.optInt("dailyUnits", 2),
                weekdayUnits = json.optJSONObject("weekdayUnits")?.let { o ->
                    o.keys().asSequence().mapNotNull { k ->
                        val day = runCatching { DayOfWeek.valueOf(k) }.getOrNull()
                        val units = o.optInt(k, 0)
                        if (day != null && units > 0) day to units else null
                    }.toMap()
                } ?: emptyMap(),
                startVerseSurah = sSurah,
                startVerseAyah = sAyah,
                reminderScheduleRaw = json.optString("reminderScheduleRaw").takeIf { it.isNotEmpty() },
                isFrozen = json.optBoolean("isFrozen", false),
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
    val goal: String? = null,
    val reminderScheduleRaw: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isFrozen", isFrozen)
        if (goal != null) put("goal", goal)
        if (reminderScheduleRaw != null) put("reminderScheduleRaw", reminderScheduleRaw)
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
                goal = json.optString("goal").takeIf { it.isNotEmpty() },
                reminderScheduleRaw = json.optString("reminderScheduleRaw").takeIf { it.isNotEmpty() },
            )
        }
    }
}
