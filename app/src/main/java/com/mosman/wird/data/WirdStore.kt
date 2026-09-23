package com.mosman.wird.data

import android.content.Context
import androidx.core.content.edit
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.domain.AwayPeriod
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.TrackScheduleMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Paper, ink, or the phone's own setting. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Whether you read from the mushaf or recite from memory.
 *
 * **PROFILE.md § 5l and § 5r.** Mutalib's own words: *"some people memorize and some look
 * in the mushaf to read"*, and asked what it changes, *"it has to do with the audio"* — it
 * selects what a recitation is checked against.
 *
 * ⚠ **That check is PLAN task 14 and does not exist yet.** What this changes today is what
 * the app calls things, which is small but is not nothing: a memoriser pressing a button
 * labelled "Recite it out loud" while looking at a page they are deliberately not reading is
 * being described wrongly by their own app.
 *
 * The design agrees on the shape, and its wording is worth keeping: *"the same loop, pointed
 * at revision. Reciting aloud is still how a day gets marked."* This is a lens on one
 * mechanic, not a second app.
 */
enum class ReadingMode { READING, MEMORISING }

/**
 * Where you are and what you've asked of yourself, kept on this phone.
 *
 * SharedPreferences rather than a database: this is a handful of integers, and a database
 * would be machinery without a job. Sacred Rule 1 — nothing here syncs anywhere, and the
 * manifest's three backup exclusions make sure Android does not quietly copy it to Drive
 * either.
 */
class WirdStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** False on a brand-new install, which is what sends the app to setup. */
    val isSetUp: Boolean get() = prefs.contains(KEY_UNIT)

    /**
     * The half-page unit you are due to start on.
     *
     * This moves only when a day is marked done (task 6), never on app launch — opening
     * the app twice in a day must show the same portion twice.
     */
    var positionUnit: Int
        get() = prefs.getInt(KEY_UNIT, 0)
        set(value) = prefs.edit { putInt(KEY_UNIT, Math.floorMod(value, Mushaf.TOTAL_UNITS)) }

    /** 1-based page you are due to start on. Setter keeps the half-page you were on. */
    var positionPage: Int
        get() = Mushaf.pageOf(positionUnit)
        set(page) {
            val clamped = page.coerceIn(1, Mushaf.PAGES)
            positionUnit = (clamped - 1) * Mushaf.UNITS_PER_PAGE
        }

    /**
     * The exact ayah the reader said they were on, when they were mid-surah.
     *
     * Portions are counted in pages, but a page is not where anyone starts. Choosing
     * Ya-Sin means starting at Ya-Sin 1, which sits partway down page 440 underneath the
     * last verse of Fatir. This remembers that, so the first day begins where the reader
     * actually is. Cleared once the position moves past it.
     */
    var startVerse: Pair<Int, Int>?
        get() {
            val s = prefs.getInt(KEY_START_SURAH, 0)
            val a = prefs.getInt(KEY_START_AYAH, 0)
            return if (s > 0 && a > 0) s to a else null
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_START_SURAH); remove(KEY_START_AYAH)
            } else {
                putInt(KEY_START_SURAH, value.first); putInt(KEY_START_AYAH, value.second)
            }
        }

    /**
     * Light, dark, or whatever the phone is set to.
     *
     * **Default changed to [ThemeMode.SYSTEM] on 2026-08-18**, at Mutalib's word: *"for the
     * home page it matches with the phone."*
     *
     * **The reason it could not before is gone.** The default was forced to DARK on
     * 2026-08-17 because that palette's gold was **2.06:1 on cream** — unreadable — so a
     * light-by-default app would have been one that never showed its own accent. § 6d
     * replaced gold with teal and measured it: **5.09:1 on the light ground, AA.** The
     * constraint was a property of the old colours, not a preference, and it left with them.
     *
     * Anyone who has already chosen keeps their choice; this only changes what a fresh
     * install does.
     */
    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

    /**
     * What to call the reader, or null if they never said.
     *
     * **PROFILE.md § 5c approved this on 2026-08-17 and § 5j specified it on 2026-08-18.**
     * It exists for exactly one thing: the greeting on Home says "Good evening, Mutalib"
     * instead of "Good evening". That is the whole feature.
     *
     * **Null is a first-class answer, not a missing value.** Setup offers a Skip, and the
     * greeting falls back to the bare hour — which is what it did before this existed and
     * which already read fine. A name is a courtesy, and an app that will not start until
     * you identify yourself is doing something else.
     *
     * Sacred Rule 1: this is a string in SharedPreferences on this phone. It is not an
     * account, it never leaves the device, and the manifest's backup exclusions cover the
     * file it lives in.
     */
    var readerName: String?
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) remove(KEY_NAME) else putString(KEY_NAME, trimmed)
        }

    /**
     * Reading from the mushaf, or reciting from memory.
     *
     * Defaults to [ReadingMode.READING] because it is the larger group and because it is
     * what every screen already assumes — a default that changes nothing is the safe one for
     * anybody who skipped the question or upgraded into it.
     */
    var readingMode: ReadingMode
        get() = runCatching {
            ReadingMode.valueOf(prefs.getString(KEY_MODE, ReadingMode.READING.name)!!)
        }.getOrDefault(ReadingMode.READING)
        set(value) = prefs.edit { putString(KEY_MODE, value.name) }

    /**
     * The promise currently being held, if any.
     *
     * **Stored, because a promise that evaporates when you close the app is not a promise.**
     * PROFILE.md § 5b — naming a time to something that checks back is the experiment, and
     * the check-back has to survive being backgrounded, force-stopped and rebooted, exactly
     * like the alarm it sits beside.
     *
     * One readable string, `"<iso timestamp> | <schedule> | <what you said>"`, so a
     * half-written change can never pair the wrong time with the wrong words. Anything
     * unparseable reads as no commitment rather than throwing — a corrupt preference must
     * not stop the app opening.
     */
    var commitment: Commitment?
        get() {
            val raw = prefs.getString(KEY_COMMITMENT, null) ?: return null
            return runCatching {
                // The older two-field form had no schedule. Read it rather than drop it:
                // losing a promise on upgrade would lose a day of PLAN task 21's evidence.
                if (!raw.contains(FIELD)) {
                    val spoken = raw.substringAfter(' ')
                    if (spoken.isBlank()) return@runCatching null
                    return@runCatching Commitment(spoken, LocalDateTime.parse(raw.substringBefore(' ')))
                }
                val parts = raw.split(FIELD)
                val spoken = parts[2]
                if (spoken.isBlank()) null
                else Commitment(
                    spoken = spoken,
                    madeAt = LocalDateTime.parse(parts[0]),
                    schedule = parts[1].takeIf { it.isNotBlank() }?.let(::decodeSchedule),
                )
            }.getOrNull()
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_COMMITMENT)
            } else {
                // Spoken text goes last because it is the only field that can contain
                // anything; a separator that cannot appear in the middle of a date or a
                // schedule cannot be spoofed by what someone types.
                putString(
                    KEY_COMMITMENT,
                    listOf(
                        value.madeAt.toString(),
                        value.schedule?.let(::encodeSchedule).orEmpty(),
                        value.spoken,
                    ).joinToString(FIELD),
                )
            }
        }

    /**
     * Which recitation model to actually use, when more than one is downloaded.
     *
     * ⚠ **Added because "try the smaller one instead" did nothing.** The first version picked
     * the larger model whenever both were present, on the reasoning that someone who downloaded
     * BASE second wanted accuracy. That is a fine default and a terrible rule: it made the two
     * models impossible to compare, which is the entire reason PLAN task 14 offers both.
     *
     * Null means "whatever is on the phone", which is right for the ordinary case of one model.
     */
    var chosenModel: String?
        get() = prefs.getString(KEY_MODEL, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_MODEL) else putString(KEY_MODEL, value)
        }

    /**
     * Which way through the mushaf this reader travels. **His instruction, 2026-08-19.**
     *
     * Defaults to [ReadingDirection.TOWARDS_NAS] — front to back — because that is what every
     * existing install has been doing, and a default that silently reverses somebody's position
     * would be the worst possible way to introduce this.
     */
    var readingDirection: ReadingDirection
        get() = runCatching {
            ReadingDirection.valueOf(prefs.getString(KEY_DIRECTION, ReadingDirection.TOWARDS_NAS.name)!!)
        }.getOrDefault(ReadingDirection.TOWARDS_NAS)
        set(value) = prefs.edit { putString(KEY_DIRECTION, value.name) }

    /**
     * Whether the **mushaf page** paints dark, kept apart from the app's theme.
     *
     * **His instruction, 2026-08-19**, describing the app he actually reads in: *"home and
     * menu are in dark mode but page is light mode."* Quran for Android treats the reading
     * surface as its own thing, and he is right that it should be — the chrome is software
     * and follows the phone, while the page is a printed object you are looking at. Someone
     * reading at night with a dark launcher does not necessarily want the Qur'an inverted.
     *
     * ⚠ **Before this, the page's own night toggle changed the WHOLE APP's theme.** Flipping
     * it on the reading screen turned Home and the menus dark too, which is why the two could
     * never disagree. Defaulting to light means a dark phone now shows dark chrome around a
     * light page, which is exactly the arrangement he pointed at.
     */
    var pageNight: Boolean
        get() = prefs.getBoolean(KEY_PAGE_NIGHT, false)
        set(value) = prefs.edit { putBoolean(KEY_PAGE_NIGHT, value) }

    /**
     * The stretch of days with no reminders, or null. **PLAN task 22.**
     *
     * Two dates in one string, same reasoning as [nudgeSchedule]: a half-written change
     * cannot leave a start without its end. Anything unparseable reads as "not away", which
     * is the failure that costs least — an unwanted reminder, rather than silence nobody
     * asked for and nobody can explain.
     */
    var away: AwayPeriod?
        get() = prefs.getString(KEY_AWAY, null)?.let { raw ->
            runCatching {
                val (from, until) = raw.split(AWAY_SEP)
                AwayPeriod(LocalDate.parse(from), LocalDate.parse(until))
            }.getOrNull()
        }
        set(value) = prefs.edit {
            if (value == null) remove(KEY_AWAY)
            else putString(KEY_AWAY, "${value.from}$AWAY_SEP${value.until}")
        }

    /**
     * What tonight's alarm should follow.
     *
     * A promise made today wins over the routine, and only for today. That single rule is
     * what makes *"in an hour"* a promise about tonight instead of a permanent move of the
     * reminder, which is what it used to be. See [Commitment.schedule].
     */
    fun scheduleFor(today: LocalDate): NudgeSchedule =
        commitment?.takeIf { it.appliesOn(today) }?.schedule ?: nudgeSchedule

    /**
     * When the nudge last actually fired, and what it was last armed for.
     *
     * **PLAN task 15's self-check, and this pair is the whole diagnostic.** Comparing them
     * answers the only question that matters on a Transsion phone: *did the reminder I was
     * promised actually arrive?* If [lastArmedFor] is in the past and [lastNudgeFiredAt] is
     * older than it, the alarm was killed — that is not a guess, it is two timestamps
     * disagreeing.
     *
     * Written by [com.mosman.wird.nudge.NudgeReceiver] when it runs, and by the scheduler
     * when it arms. Anything unparseable reads as null rather than throwing.
     */
    var lastNudgeFiredAt: LocalDateTime?
        get() = prefs.getString(KEY_FIRED, null)?.let {
            runCatching { LocalDateTime.parse(it) }.getOrNull()
        }
        set(value) = prefs.edit {
            if (value == null) remove(KEY_FIRED) else putString(KEY_FIRED, value.toString())
        }

    /** The moment the current alarm is set for. See [lastNudgeFiredAt]. */
    var lastArmedFor: LocalDateTime?
        get() = prefs.getString(KEY_ARMED_FOR, null)?.let {
            runCatching { LocalDateTime.parse(it) }.getOrNull()
        }
        set(value) = prefs.edit {
            if (value == null) remove(KEY_ARMED_FOR) else putString(KEY_ARMED_FOR, value.toString())
        }

    /**
     * Whether the reader has been shown, once, that tapping the page reveals the chrome.
     *
     * The gesture is otherwise invisible — nothing on a clean mushaf page announces that
     * it is tappable, which Mutalib spotted when he asked whether it should be a hamburger
     * icon. A permanent icon would announce it, but at the cost of a mark parked on the
     * Qur'an forever. Showing the bar once teaches the same thing and then gets out of the
     * way.
     */
    var hasSeenChrome: Boolean
        get() = prefs.getBoolean(KEY_SEEN_CHROME, false)
        set(value) = prefs.edit { putBoolean(KEY_SEEN_CHROME, value) }

    /**
     * When the reminder arrives. Defaults to thirty minutes after Maghrib.
     *
     * Stored as one short string rather than three separate keys, so a half-written
     * change can never leave "at Fajr" paired with a clock time. Anything unparseable
     * falls back to the default instead of throwing — a corrupt preference must not stop
     * the app opening.
     */
    var nudgeSchedule: NudgeSchedule
        get() = decodeSchedule(prefs.getString(KEY_NUDGE, null))
        set(value) = prefs.edit { putString(KEY_NUDGE, encodeSchedule(value)) }

    /**
     * Which Shatri recording to fetch.
     *
     * Defaults to the lighter one. A page is ~1.1 MB at 64 kbps against ~2.3 MB at 128,
     * every day, and the people this is being built for are students on Ghanaian mobile
     * data — so the default protects the bill and the setting is there for anyone who
     * would rather spend it.
     */
    var audioQuality: AudioQuality
        get() = runCatching {
            AudioQuality.valueOf(prefs.getString(KEY_AUDIO, AudioQuality.LIGHT.name)!!)
        }.getOrDefault(AudioQuality.LIGHT)
        set(value) = prefs.edit { putString(KEY_AUDIO, value.name) }

    var plan: ReadingPlan
        get() = ReadingPlan(
            defaultUnits = prefs.getInt(KEY_DEFAULT_UNITS, Mushaf.UNITS_PER_PAGE),
            weekdayUnits = DayOfWeek.entries
                .mapNotNull { day ->
                    val v = prefs.getInt(weekdayKey(day), 0)
                    if (v > 0) day to v else null
                }
                .toMap(),
        )
        set(value) = prefs.edit {
            putInt(KEY_DEFAULT_UNITS, value.defaultUnits)
            // Rewrite every day, so removing an override actually removes it.
            DayOfWeek.entries.forEach { day ->
                val v = value.weekdayUnits[day]
                if (v == null) remove(weekdayKey(day)) else putInt(weekdayKey(day), v)
            }
        }

    var lockOrientation: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ORIENTATION, false)
        set(value) = prefs.edit { putBoolean(KEY_LOCK_ORIENTATION, value) }

    var landscapeOrientation: Boolean
        get() = prefs.getBoolean(KEY_LANDSCAPE_ORIENTATION, false)
        set(value) = prefs.edit { putBoolean(KEY_LANDSCAPE_ORIENTATION, value) }

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_AWAKE, true)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_AWAKE, value) }

    var surahTranslatedName: Boolean
        get() = prefs.getBoolean(KEY_SURAH_TRANSLATED, true)
        set(value) = prefs.edit { putBoolean(KEY_SURAH_TRANSLATED, value) }

    var ayahBeforeTranslation: Boolean
        get() = prefs.getBoolean(KEY_AYAH_BEFORE_TRANS, true)
        set(value) = prefs.edit { putBoolean(KEY_AYAH_BEFORE_TRANS, value) }

    var customAyahTextSizeEnabled: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_AYAH_TEXT_SIZE, false)
        set(value) = prefs.edit { putBoolean(KEY_CUSTOM_AYAH_TEXT_SIZE, value) }

    var ayahTextSize: Int
        get() = prefs.getInt(KEY_AYAH_TEXT_SIZE, 24)
        set(value) = prefs.edit { putInt(KEY_AYAH_TEXT_SIZE, value) }

    var translationTextSize: Int
        get() = prefs.getInt(KEY_TRANS_TEXT_SIZE, 15)
        set(value) = prefs.edit { putInt(KEY_TRANS_TEXT_SIZE, value) }

    var streamingAudio: Boolean
        get() = prefs.getBoolean(KEY_STREAMING_AUDIO, false)
        set(value) = prefs.edit { putBoolean(KEY_STREAMING_AUDIO, value) }

    var downloadAmount: String
        get() = prefs.getString(KEY_DOWNLOAD_AMOUNT, "Page") ?: "Page"
        set(value) = prefs.edit { putString(KEY_DOWNLOAD_AMOUNT, value) }

    var selectedTranslation: String
        get() = prefs.getString(KEY_SELECTED_TRANS, "Saheeh International") ?: "Saheeh International"
        set(value) = prefs.edit { putString(KEY_SELECTED_TRANS, value) }

    var selectedReciter: String
        get() = prefs.getString(KEY_SELECTED_RECITER, "Abu Bakr al-Shatri") ?: "Abu Bakr al-Shatri"
        set(value) = prefs.edit { putString(KEY_SELECTED_RECITER, value) }

    var nightTextBrightness: Int
        get() = prefs.getInt(KEY_NIGHT_TEXT_BRIGHTNESS, 210)
        set(value) = prefs.edit { putInt(KEY_NIGHT_TEXT_BRIGHTNESS, value) }

    var nightBgBrightness: Int
        get() = prefs.getInt(KEY_NIGHT_BG_BRIGHTNESS, 0)
        set(value) = prefs.edit { putInt(KEY_NIGHT_BG_BRIGHTNESS, value) }

    /** Recent pages opened or read, newest first. Keeps up to 20 pages. */
    var recentPages: List<Int>
        get() {
            val raw = prefs.getString(KEY_RECENT_PAGES, null) ?: return emptyList()
            return raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..Mushaf.PAGES }
                .distinct()
        }
        private set(value) {
            prefs.edit { putString(KEY_RECENT_PAGES, value.distinct().take(20).joinToString(",")) }
        }

    fun recordRecentPage(page: Int) {
        if (page !in 1..Mushaf.PAGES) return
        val current = recentPages.filter { it != page }
        recentPages = listOf(page) + current
    }

    var trackScheduleMode: TrackScheduleMode
        get() = runCatching {
            TrackScheduleMode.valueOf(prefs.getString(KEY_TRACK_SCHEDULE_MODE, TrackScheduleMode.AUTOMATIC.name)!!)
        }.getOrDefault(TrackScheduleMode.AUTOMATIC)
        set(value) = prefs.edit { putString(KEY_TRACK_SCHEDULE_MODE, value.name) }

    var activeSpaceId: String
        get() = prefs.getString(KEY_ACTIVE_SPACE_ID, "home") ?: "home"
        set(value) = prefs.edit { putString(KEY_ACTIVE_SPACE_ID, value) }

    var manualActiveTrackId: String?
        get() = prefs.getString(KEY_ACTIVE_TRACK_ID, null)
        set(value) = prefs.edit { putString(KEY_ACTIVE_TRACK_ID, value) }

    fun getLifeSpaces(): List<LifeSpace> {
        val raw = prefs.getString(KEY_LIFE_SPACES, null)
        if (raw.isNullOrBlank()) {
            val initial = createDefaultLifeSpaces()
            saveLifeSpaces(initial)
            return initial
        }
        return runCatching {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<LifeSpace>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { list.add(LifeSpace.fromJson(it)) }
            }
            if (list.isEmpty()) createDefaultLifeSpaces() else list
        }.getOrElse {
            createDefaultLifeSpaces()
        }
    }

    fun saveLifeSpaces(spaces: List<LifeSpace>) {
        val arr = org.json.JSONArray()
        spaces.forEach { arr.put(it.toJson()) }
        prefs.edit { putString(KEY_LIFE_SPACES, arr.toString()) }
    }

    private fun createDefaultLifeSpaces(): List<LifeSpace> {
        val currentPos = positionUnit
        val currentDir = readingDirection
        val currentUnits = plan.defaultUnits

        val monThu = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
        val satSun = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        val homeSpace = LifeSpace(
            id = "home",
            name = "Home",
            isFrozen = false,
            tracks = listOf(
                ReadingTrack(
                    id = "home_mon_thu",
                    name = "Evening Madrasa (Ya-Sin)",
                    type = TrackType.HIFZ,
                    activeDays = monThu,
                    positionUnit = if (currentPos > 0) currentPos else (440 - 1) * Mushaf.UNITS_PER_PAGE,
                    direction = currentDir,
                    dailyUnits = currentUnits,
                    startVerseSurah = 36,
                    startVerseAyah = 1,
                ),
                ReadingTrack(
                    id = "home_weekend",
                    name = "Weekend Madrasa (Al-Anbiya)",
                    type = TrackType.TILAWAH,
                    activeDays = satSun,
                    positionUnit = (322 - 1) * Mushaf.UNITS_PER_PAGE,
                    direction = ReadingDirection.TOWARDS_NAS,
                    dailyUnits = currentUnits,
                    startVerseSurah = 21,
                    startVerseAyah = 1,
                ),
            ),
        )

        val schoolSpace = LifeSpace(
            id = "school",
            name = "School",
            isFrozen = true,
            tracks = listOf(
                ReadingTrack(
                    id = "school_daily",
                    name = "School Quran",
                    type = TrackType.TILAWAH,
                    activeDays = DayOfWeek.entries.toSet(),
                    positionUnit = 0,
                    direction = ReadingDirection.TOWARDS_NAS,
                    dailyUnits = currentUnits,
                ),
            ),
        )

        return listOf(homeSpace, schoolSpace)
    }

    fun activeSpace(): LifeSpace {
        val spaces = getLifeSpaces()
        return spaces.firstOrNull { it.id == activeSpaceId } ?: spaces.first()
    }

    fun activeTrack(date: LocalDate = LocalDate.now()): ReadingTrack {
        val space = activeSpace()
        if (trackScheduleMode == TrackScheduleMode.MANUAL && manualActiveTrackId != null) {
            val manual = space.tracks.firstOrNull { it.id == manualActiveTrackId }
            if (manual != null) return manual
        }

        val dow = date.dayOfWeek
        val matching = space.tracks.firstOrNull { dow in it.activeDays }
        return matching ?: space.tracks.firstOrNull() ?: ReadingTrack(
            id = "default",
            name = "Daily Quran",
            positionUnit = positionUnit,
            direction = readingDirection,
            dailyUnits = plan.defaultUnits,
        )
    }

    fun setActiveSpace(spaceId: String) {
        activeSpaceId = spaceId
        manualActiveTrackId = null
        val track = activeTrack()
        positionUnit = track.positionUnit
        readingDirection = track.direction
    }

    fun setActiveTrack(trackId: String) {
        manualActiveTrackId = trackId
        val track = activeTrack()
        positionUnit = track.positionUnit
        readingDirection = track.direction
    }

    fun setSpaceFrozen(spaceId: String, frozen: Boolean) {
        val spaces = getLifeSpaces().map {
            if (it.id == spaceId) it.copy(isFrozen = frozen) else it
        }
        saveLifeSpaces(spaces)
    }

    fun addLifeSpace(name: String): LifeSpace {
        val id = "space_" + System.currentTimeMillis()
        val defaultTrack = ReadingTrack(
            id = "track_${id}_1",
            name = "$name Quran",
            type = TrackType.TILAWAH,
            activeDays = DayOfWeek.entries.toSet(),
            positionUnit = 0,
            direction = ReadingDirection.TOWARDS_NAS,
            dailyUnits = plan.defaultUnits,
        )
        val newSpace = LifeSpace(id = id, name = name, isFrozen = false, tracks = listOf(defaultTrack))
        saveLifeSpaces(getLifeSpaces() + newSpace)
        return newSpace
    }

    fun deleteLifeSpace(spaceId: String): Boolean {
        val current = getLifeSpaces()
        if (current.size <= 1) return false
        val filtered = current.filter { it.id != spaceId }
        saveLifeSpaces(filtered)
        if (activeSpaceId == spaceId) {
            setActiveSpace(filtered.first().id)
        }
        return true
    }

    fun addTrackToSpace(spaceId: String, track: ReadingTrack) {
        val spaces = getLifeSpaces().map { space ->
            if (space.id == spaceId) {
                space.copy(tracks = space.tracks + track)
            } else space
        }
        saveLifeSpaces(spaces)
    }

    fun deleteTrackFromSpace(spaceId: String, trackId: String): Boolean {
        val spaces = getLifeSpaces().map { space ->
            if (space.id == spaceId) {
                if (space.tracks.size <= 1) return false
                space.copy(tracks = space.tracks.filter { it.id != trackId })
            } else space
        }
        saveLifeSpaces(spaces)
        if (manualActiveTrackId == trackId) {
            manualActiveTrackId = null
        }
        return true
    }

    fun updateTrack(updated: ReadingTrack) {
        val spaces = getLifeSpaces().map { space ->
            if (space.tracks.any { it.id == updated.id }) {
                space.copy(tracks = space.tracks.map { if (it.id == updated.id) updated else it })
            } else {
                space
            }
        }
        saveLifeSpaces(spaces)
    }

    fun recordTrackDone(trackId: String, nextStartUnit: Int, today: LocalDate = LocalDate.now()) {
        val space = activeSpace()
        val track = space.tracks.firstOrNull { it.id == trackId } ?: return

        val isAlreadyCompletedToday = track.lastCompletedDate == today.toString()
        if (isAlreadyCompletedToday) {
            val updated = track.copy(positionUnit = nextStartUnit)
            updateTrack(updated)
            positionUnit = nextStartUnit
            return
        }

        // Streak progression respecting active days and freeze state
        val isConsecutive = if (track.lastCompletedDate == null) {
            false
        } else if (space.isFrozen) {
            true
        } else if (track.activeDays.isNotEmpty()) {
            var checkDate = today.minusDays(1)
            var lookbackLimit = 0
            while (checkDate.dayOfWeek !in track.activeDays && lookbackLimit < 7) {
                checkDate = checkDate.minusDays(1)
                lookbackLimit++
            }
            track.lastCompletedDate == checkDate.toString()
        } else {
            track.lastCompletedDate == today.minusDays(1).toString()
        }

        val newStreak = if (isConsecutive) track.currentStreak + 1 else 1
        val updated = track.copy(
            positionUnit = nextStartUnit,
            currentStreak = newStreak,
            totalDaysRead = track.totalDaysRead + 1,
            lastCompletedDate = today.toString(),
        )
        updateTrack(updated)
        positionUnit = nextStartUnit
    }

    private companion object {
        const val PREFS = "wird_position"
        const val KEY_UNIT = "position_unit"
        const val KEY_START_SURAH = "start_surah"
        const val KEY_START_AYAH = "start_ayah"
        const val KEY_DEFAULT_UNITS = "default_units"
        const val KEY_THEME = "theme_mode"
        const val KEY_SEEN_CHROME = "seen_chrome"
        const val KEY_NAME = "reader_name"
        const val KEY_COMMITMENT = "commitment"
        const val KEY_MODE = "reading_mode"
        const val KEY_FIRED = "nudge_fired_at"
        const val KEY_ARMED_FOR = "nudge_armed_for"
        const val KEY_NUDGE = "nudge_schedule"
        const val KEY_AUDIO = "audio_quality"
        const val KEY_AWAY = "away_period"
        const val KEY_PAGE_NIGHT = "page_night"
        const val KEY_DIRECTION = "reading_direction"
        const val KEY_MODEL = "recitation_model"
        const val KEY_LOCK_ORIENTATION = "lock_orientation"
        const val KEY_LANDSCAPE_ORIENTATION = "landscape_orientation"
        const val KEY_KEEP_AWAKE = "keep_screen_awake"
        const val KEY_SURAH_TRANSLATED = "surah_translated_name"
        const val KEY_AYAH_BEFORE_TRANS = "ayah_before_translation"
        const val KEY_CUSTOM_AYAH_TEXT_SIZE = "custom_ayah_text_size"
        const val KEY_AYAH_TEXT_SIZE = "ayah_text_size"
        const val KEY_TRANS_TEXT_SIZE = "translation_text_size"
        const val KEY_STREAMING_AUDIO = "streaming_audio"
        const val KEY_DOWNLOAD_AMOUNT = "download_amount"
        const val KEY_SELECTED_TRANS = "selected_translation"
        const val KEY_SELECTED_RECITER = "selected_reciter"
        const val KEY_NIGHT_TEXT_BRIGHTNESS = "night_text_brightness"
        const val KEY_NIGHT_BG_BRIGHTNESS = "night_bg_brightness"
        const val KEY_RECENT_PAGES = "recent_pages"
        const val KEY_LIFE_SPACES = "life_spaces_json"
        const val KEY_ACTIVE_SPACE_ID = "active_space_id"
        const val KEY_ACTIVE_TRACK_ID = "active_track_id"
        const val KEY_TRACK_SCHEDULE_MODE = "track_schedule_mode"
        const val FIELD = " | "
        const val AWAY_SEP = ".."
        fun weekdayKey(day: DayOfWeek) = "units_${day.name}"
    }
}

/** "PRAYER MAGHRIB 30", "CLOCK 20:00", or "OFF". Readable on purpose — see DayLogStore. */
internal fun encodeSchedule(schedule: NudgeSchedule): String = when (schedule) {
    is NudgeSchedule.Off -> "OFF"
    is NudgeSchedule.AtClockTime -> "CLOCK ${schedule.time}"
    is NudgeSchedule.AfterPrayer -> "PRAYER ${schedule.prayer.name} ${schedule.offsetMinutes}"
}

internal fun decodeSchedule(raw: String?): NudgeSchedule {
    if (raw == null) return NudgeSchedule.Default
    val parts = raw.split(" ")
    return runCatching {
        when (parts[0]) {
            "OFF" -> NudgeSchedule.Off
            "CLOCK" -> NudgeSchedule.AtClockTime(LocalTime.parse(parts[1]))
            "PRAYER" -> NudgeSchedule.AfterPrayer(
                prayer = Prayer.valueOf(parts[1]),
                offsetMinutes = parts[2].toInt(),
            )
            else -> NudgeSchedule.Default
        }
    }.getOrDefault(NudgeSchedule.Default)
}
