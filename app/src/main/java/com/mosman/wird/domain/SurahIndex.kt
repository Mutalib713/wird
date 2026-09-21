package com.mosman.wird.domain

/** Where a surah was revealed. The list calls these Makki and Madani, as mushafs do. */
enum class RevealedIn { MAKKI, MADANI }

/**
 * Where each surah sits in the mushaf, and what its name means.
 *
 * **Generated from the Quran.com API's `/chapters?language=en` endpoint**, most recently on
 * 2026-08-18 — not typed from memory. Bundled rather than fetched so the list works with the
 * radio off and costs no data: it is metadata, not Qur'anic text, so Sacred Rule 2's
 * verified-source requirement is met by this provenance line rather than by a live call.
 *
 * **The 2026-08-18 regeneration was checked against the previous file before it replaced
 * it**, row by row on number, name and both page numbers: **114 rows, zero mismatches.** The
 * new columns — [Surah.meaning], [Surah.verses], [Surah.revealedIn] — are additions to data
 * that was already correct, not a re-derivation of it.
 *
 * Note surahs can *share* a page: Sad ends on 458 and Az-Zumar begins on 458.
 */
data class Surah(
    val number: Int,
    val name: String,
    /** The English sense of the name — "The Women" for An-Nisa. From `translated_name`. */
    val meaning: String,
    val verses: Int,
    val revealedIn: RevealedIn,
    val firstPage: Int,
    val lastPage: Int,
)

/**
 * Where each of the thirty juz' begins.
 *
 * **Fetched per juz' from `/verses/by_juz/{n}?fields=page_number`** on 2026-08-18, taking the
 * page of its first verse — rather than typing a table of thirty numbers from memory, which
 * is exactly the kind of Qur'anic data this project refuses to guess at.
 *
 * *Independently confirmed:* Mutalib's reference screenshots of Quran for Android show
 * Juz' 18 → 342, 19 → 362, 20 → 382 and 21 → 402. All four match.
 */
data class Juz(val number: Int, val firstPage: Int)

object JuzIndex {
    val all: List<Juz> = listOf(
        Juz(1, 1),
        Juz(2, 22),
        Juz(3, 42),
        Juz(4, 62),
        Juz(5, 82),
        Juz(6, 102),
        Juz(7, 121),
        Juz(8, 142),
        Juz(9, 162),
        Juz(10, 182),
        Juz(11, 201),
        Juz(12, 222),
        Juz(13, 242),
        Juz(14, 262),
        Juz(15, 282),
        Juz(16, 302),
        Juz(17, 322),
        Juz(18, 342),
        Juz(19, 362),
        Juz(20, 382),
        Juz(21, 402),
        Juz(22, 422),
        Juz(23, 442),
        Juz(24, 462),
        Juz(25, 482),
        Juz(26, 502),
        Juz(27, 522),
        Juz(28, 542),
        Juz(29, 562),
        Juz(30, 582),
    )

    /** The juz' a page falls in. Every page belongs to exactly one. */
    fun of(page: Int): Juz = all.last { page >= it.firstPage }
}

object SurahIndex {

    val all: List<Surah> = listOf(
        Surah(1, "Al-Fatihah", "The Opener", 7, RevealedIn.MAKKI, 1, 1),
        Surah(2, "Al-Baqarah", "The Cow", 286, RevealedIn.MADANI, 2, 49),
        Surah(3, "Ali 'Imran", "Family of Imran", 200, RevealedIn.MADANI, 50, 76),
        Surah(4, "An-Nisa", "The Women", 176, RevealedIn.MADANI, 77, 106),
        Surah(5, "Al-Ma'idah", "The Table Spread", 120, RevealedIn.MADANI, 106, 127),
        Surah(6, "Al-An'am", "The Cattle", 165, RevealedIn.MAKKI, 128, 150),
        Surah(7, "Al-A'raf", "The Heights", 206, RevealedIn.MAKKI, 151, 176),
        Surah(8, "Al-Anfal", "The Spoils of War", 75, RevealedIn.MADANI, 177, 186),
        Surah(9, "At-Tawbah", "The Repentance", 129, RevealedIn.MADANI, 187, 207),
        Surah(10, "Yunus", "Jonah", 109, RevealedIn.MAKKI, 208, 221),
        Surah(11, "Hud", "Hud", 123, RevealedIn.MAKKI, 221, 235),
        Surah(12, "Yusuf", "Joseph", 111, RevealedIn.MAKKI, 235, 248),
        Surah(13, "Ar-Ra'd", "The Thunder", 43, RevealedIn.MADANI, 249, 255),
        Surah(14, "Ibrahim", "Abraham", 52, RevealedIn.MAKKI, 255, 261),
        Surah(15, "Al-Hijr", "The Rocky Tract", 99, RevealedIn.MAKKI, 262, 267),
        Surah(16, "An-Nahl", "The Bee", 128, RevealedIn.MAKKI, 267, 281),
        Surah(17, "Al-Isra", "The Night Journey", 111, RevealedIn.MAKKI, 282, 293),
        Surah(18, "Al-Kahf", "The Cave", 110, RevealedIn.MAKKI, 293, 304),
        Surah(19, "Maryam", "Mary", 98, RevealedIn.MAKKI, 305, 312),
        Surah(20, "Taha", "Ta-Ha", 135, RevealedIn.MAKKI, 312, 321),
        Surah(21, "Al-Anbya", "The Prophets", 112, RevealedIn.MAKKI, 322, 331),
        Surah(22, "Al-Hajj", "The Pilgrimage", 78, RevealedIn.MADANI, 332, 341),
        Surah(23, "Al-Mu'minun", "The Believers", 118, RevealedIn.MAKKI, 342, 349),
        Surah(24, "An-Nur", "The Light", 64, RevealedIn.MADANI, 350, 359),
        Surah(25, "Al-Furqan", "The Criterion", 77, RevealedIn.MAKKI, 359, 366),
        Surah(26, "Ash-Shu'ara", "The Poets", 227, RevealedIn.MAKKI, 367, 376),
        Surah(27, "An-Naml", "The Ant", 93, RevealedIn.MAKKI, 377, 385),
        Surah(28, "Al-Qasas", "The Stories", 88, RevealedIn.MAKKI, 385, 396),
        Surah(29, "Al-'Ankabut", "The Spider", 69, RevealedIn.MAKKI, 396, 404),
        Surah(30, "Ar-Rum", "The Romans", 60, RevealedIn.MAKKI, 404, 410),
        Surah(31, "Luqman", "Luqman", 34, RevealedIn.MAKKI, 411, 414),
        Surah(32, "As-Sajdah", "The Prostration", 30, RevealedIn.MAKKI, 415, 417),
        Surah(33, "Al-Ahzab", "The Combined Forces", 73, RevealedIn.MADANI, 418, 427),
        Surah(34, "Saba", "Sheba", 54, RevealedIn.MAKKI, 428, 434),
        Surah(35, "Fatir", "Originator", 45, RevealedIn.MAKKI, 434, 440),
        Surah(36, "Ya-Sin", "Ya Sin", 83, RevealedIn.MAKKI, 440, 445),
        Surah(37, "As-Saffat", "Those who set the Ranks", 182, RevealedIn.MAKKI, 446, 452),
        Surah(38, "Sad", "The Letter “Saad”", 88, RevealedIn.MAKKI, 453, 458),
        Surah(39, "Az-Zumar", "The Troops", 75, RevealedIn.MAKKI, 458, 467),
        Surah(40, "Ghafir", "The Forgiver", 85, RevealedIn.MAKKI, 467, 476),
        Surah(41, "Fussilat", "Explained in Detail", 54, RevealedIn.MAKKI, 477, 482),
        Surah(42, "Ash-Shuraa", "The Consultation", 53, RevealedIn.MAKKI, 483, 489),
        Surah(43, "Az-Zukhruf", "The Ornaments of Gold", 89, RevealedIn.MAKKI, 489, 495),
        Surah(44, "Ad-Dukhan", "The Smoke", 59, RevealedIn.MAKKI, 496, 498),
        Surah(45, "Al-Jathiyah", "The Crouching", 37, RevealedIn.MAKKI, 499, 502),
        Surah(46, "Al-Ahqaf", "The Wind-Curved Sandhills", 35, RevealedIn.MAKKI, 502, 506),
        Surah(47, "Muhammad", "Muhammad", 38, RevealedIn.MADANI, 507, 510),
        Surah(48, "Al-Fath", "The Victory", 29, RevealedIn.MADANI, 511, 515),
        Surah(49, "Al-Hujurat", "The Rooms", 18, RevealedIn.MADANI, 515, 517),
        Surah(50, "Qaf", "The Letter “Qaf”", 45, RevealedIn.MAKKI, 518, 520),
        Surah(51, "Adh-Dhariyat", "The Winnowing Winds", 60, RevealedIn.MAKKI, 520, 523),
        Surah(52, "At-Tur", "The Mount", 49, RevealedIn.MAKKI, 523, 525),
        Surah(53, "An-Najm", "The Star", 62, RevealedIn.MAKKI, 526, 528),
        Surah(54, "Al-Qamar", "The Moon", 55, RevealedIn.MAKKI, 528, 531),
        Surah(55, "Ar-Rahman", "The Beneficent", 78, RevealedIn.MADANI, 531, 534),
        Surah(56, "Al-Waqi'ah", "The Inevitable", 96, RevealedIn.MAKKI, 534, 537),
        Surah(57, "Al-Hadid", "The Iron", 29, RevealedIn.MADANI, 537, 541),
        Surah(58, "Al-Mujadila", "The Pleading Woman", 22, RevealedIn.MADANI, 542, 545),
        Surah(59, "Al-Hashr", "The Exile", 24, RevealedIn.MADANI, 545, 548),
        Surah(60, "Al-Mumtahanah", "She that is to be examined", 13, RevealedIn.MADANI, 549, 551),
        Surah(61, "As-Saf", "The Ranks", 14, RevealedIn.MADANI, 551, 552),
        Surah(62, "Al-Jumu'ah", "The Congregation, Friday", 11, RevealedIn.MADANI, 553, 554),
        Surah(63, "Al-Munafiqun", "The Hypocrites", 11, RevealedIn.MADANI, 554, 555),
        Surah(64, "At-Taghabun", "The Mutual Disillusion", 18, RevealedIn.MADANI, 556, 557),
        Surah(65, "At-Talaq", "The Divorce", 12, RevealedIn.MADANI, 558, 559),
        Surah(66, "At-Tahrim", "The Prohibition", 12, RevealedIn.MADANI, 560, 561),
        Surah(67, "Al-Mulk", "The Sovereignty", 30, RevealedIn.MAKKI, 562, 564),
        Surah(68, "Al-Qalam", "The Pen", 52, RevealedIn.MAKKI, 564, 566),
        Surah(69, "Al-Haqqah", "The Reality", 52, RevealedIn.MAKKI, 566, 568),
        Surah(70, "Al-Ma'arij", "The Ascending Stairways", 44, RevealedIn.MAKKI, 568, 570),
        Surah(71, "Nuh", "Noah", 28, RevealedIn.MAKKI, 570, 571),
        Surah(72, "Al-Jinn", "The Jinn", 28, RevealedIn.MAKKI, 572, 573),
        Surah(73, "Al-Muzzammil", "The Enshrouded One", 20, RevealedIn.MAKKI, 574, 575),
        Surah(74, "Al-Muddaththir", "The Cloaked One", 56, RevealedIn.MAKKI, 575, 577),
        Surah(75, "Al-Qiyamah", "The Resurrection", 40, RevealedIn.MAKKI, 577, 578),
        Surah(76, "Al-Insan", "The Man", 31, RevealedIn.MADANI, 578, 580),
        Surah(77, "Al-Mursalat", "The Emissaries", 50, RevealedIn.MAKKI, 580, 581),
        Surah(78, "An-Naba", "The Tidings", 40, RevealedIn.MAKKI, 582, 583),
        Surah(79, "An-Nazi'at", "Those who drag forth", 46, RevealedIn.MAKKI, 583, 584),
        Surah(80, "'Abasa", "He Frowned", 42, RevealedIn.MAKKI, 585, 585),
        Surah(81, "At-Takwir", "The Overthrowing", 29, RevealedIn.MAKKI, 586, 586),
        Surah(82, "Al-Infitar", "The Cleaving", 19, RevealedIn.MAKKI, 587, 587),
        Surah(83, "Al-Mutaffifin", "The Defrauding", 36, RevealedIn.MAKKI, 587, 589),
        Surah(84, "Al-Inshiqaq", "The Sundering", 25, RevealedIn.MAKKI, 589, 589),
        Surah(85, "Al-Buruj", "The Mansions of the Stars", 22, RevealedIn.MAKKI, 590, 590),
        Surah(86, "At-Tariq", "The Nightcommer", 17, RevealedIn.MAKKI, 591, 591),
        Surah(87, "Al-A'la", "The Most High", 19, RevealedIn.MAKKI, 591, 592),
        Surah(88, "Al-Ghashiyah", "The Overwhelming", 26, RevealedIn.MAKKI, 592, 592),
        Surah(89, "Al-Fajr", "The Dawn", 30, RevealedIn.MAKKI, 593, 594),
        Surah(90, "Al-Balad", "The City", 20, RevealedIn.MAKKI, 594, 594),
        Surah(91, "Ash-Shams", "The Sun", 15, RevealedIn.MAKKI, 595, 595),
        Surah(92, "Al-Layl", "The Night", 21, RevealedIn.MAKKI, 595, 596),
        Surah(93, "Ad-Duhaa", "The Morning Hours", 11, RevealedIn.MAKKI, 596, 596),
        Surah(94, "Ash-Sharh", "The Relief", 8, RevealedIn.MAKKI, 596, 596),
        Surah(95, "At-Tin", "The Fig", 8, RevealedIn.MAKKI, 597, 597),
        Surah(96, "Al-'Alaq", "The Clot", 19, RevealedIn.MAKKI, 597, 597),
        Surah(97, "Al-Qadr", "The Power", 5, RevealedIn.MAKKI, 598, 598),
        Surah(98, "Al-Bayyinah", "The Clear Proof", 8, RevealedIn.MADANI, 598, 599),
        Surah(99, "Az-Zalzalah", "The Earthquake", 8, RevealedIn.MADANI, 599, 599),
        Surah(100, "Al-'Adiyat", "The Courser", 11, RevealedIn.MAKKI, 599, 600),
        Surah(101, "Al-Qari'ah", "The Calamity", 11, RevealedIn.MAKKI, 600, 600),
        Surah(102, "At-Takathur", "The Rivalry in world increase", 8, RevealedIn.MAKKI, 600, 600),
        Surah(103, "Al-'Asr", "The Declining Day", 3, RevealedIn.MAKKI, 601, 601),
        Surah(104, "Al-Humazah", "The Traducer", 9, RevealedIn.MAKKI, 601, 601),
        Surah(105, "Al-Fil", "The Elephant", 5, RevealedIn.MAKKI, 601, 601),
        Surah(106, "Quraysh", "Quraysh", 4, RevealedIn.MAKKI, 602, 602),
        Surah(107, "Al-Ma'un", "The Small kindnesses", 7, RevealedIn.MAKKI, 602, 602),
        Surah(108, "Al-Kawthar", "The Abundance", 3, RevealedIn.MAKKI, 602, 602),
        Surah(109, "Al-Kafirun", "The Disbelievers", 6, RevealedIn.MAKKI, 603, 603),
        Surah(110, "An-Nasr", "The Divine Support", 3, RevealedIn.MADANI, 603, 603),
        Surah(111, "Al-Masad", "The Palm Fiber", 5, RevealedIn.MAKKI, 603, 603),
        Surah(112, "Al-Ikhlas", "The Sincerity", 4, RevealedIn.MAKKI, 604, 604),
        Surah(113, "Al-Falaq", "The Daybreak", 5, RevealedIn.MAKKI, 604, 604),
        Surah(114, "An-Nas", "Mankind", 6, RevealedIn.MAKKI, 604, 604),
    )


    /** Every surah with any text on [page]. Usually one, occasionally two. */
    fun on(page: Int): List<Surah> = all.filter { page in it.firstPage..it.lastPage }

    /**
     * Every surah touched by a run of pages, **in the order you meet them**.
     *
     * Not sorted by number, deliberately. A portion that wraps past the end of the mushaf
     * finishes An-Nas and then begins Al-Fatihah; sorting by number would announce
     * Al-Fatihah first and describe a journey nobody took.
     */
    fun across(pages: Iterable<Int>): List<Surah> =
        pages.flatMap { on(it) }.distinctBy { it.number }

    fun byNumber(number: Int): Surah? = all.firstOrNull { it.number == number }
}

/**
 * How a surah is written in a list: "An-Nisa (The Women)".
 *
 * **The parenthetical is dropped when it only repeats the name.** Six surahs are named for a
 * person or a word rather than a subject, and the API's translated name is simply that word
 * again — "Hud (Hud)", "Luqman (Luqman)", "Ya-Sin (Ya Sin)", "Taha (Ta-Ha)", "Muhammad
 * (Muhammad)", "Quraysh (Quraysh)". Each reads as a stutter and none of them tells you
 * anything.
 *
 * Done as a rule rather than a list of six numbers, so it keeps working if the API's wording
 * changes. Comparison folds away case, spacing, hyphens and diacritics, which is what makes
 * "Ya-Sin" match "Ya Sin" and "Taha" match "Ta-Ha".
 */
fun Surah.listLabel(includeMeaning: Boolean = true): String =
    if (!includeMeaning || fold(name) == fold(meaning)) name else "$name ($meaning)"

private fun fold(text: String): String =
    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKD)
        .filterNot { it.isISOControl() }
        .filter { it.isLetterOrDigit() }
        .lowercase()

fun toArabicDigits(number: Int): String {
    val digits = "٠١٢٣٤٥٦٧٨٩"
    return number.toString().map { if (it in '0'..'9') digits[it - '0'] else it }.joinToString("")
}

val Surah.arabicName: String
    get() = arabicSurahNames[number] ?: name

private val arabicSurahNames: Map<Int, String> = mapOf(
    1 to "الفَاتِحَة", 2 to "البَقَرَة", 3 to "آل عِمْرَان", 4 to "النِّسَاء", 5 to "المَائِدَة",
    6 to "الأَنْعَام", 7 to "الأَعْرَاف", 8 to "الأَنْفَال", 9 to "التَّوْبَة", 10 to "يُونُس",
    11 to "هُود", 12 to "يُوسُف", 13 to "الرَّعْد", 14 to "إِبْرَاهِيم", 15 to "الحِجْر",
    16 to "النَّحْل", 17 to "الإِسْرَاء", 18 to "الكَهْف", 19 to "مَرْيَم", 20 to "طه",
    21 to "الأَنْبِيَاء", 22 to "الحَجّ", 23 to "المُؤْمِنُون", 24 to "النُّور", 25 to "الفُرْقَان",
    26 to "الشُّعَرَاء", 27 to "النَّمْل", 28 to "القَصَص", 29 to "العَنْكَبُوت", 30 to "الرُّوم",
    31 to "لُقْمَان", 32 to "السَّجْدَة", 33 to "الأَحْزَاب", 34 to "سَبَأ", 35 to "فَاطِر",
    36 to "يس", 37 to "الصَّافَّات", 38 to "ص", 39 to "الزُّمَر", 40 to "غَافِر",
    41 to "فُصِّلَت", 42 to "الشُّورَى", 43 to "الزُّخْرُف", 44 to "الدُّخَان", 45 to "الجَاثِيَة",
    46 to "الأَحْقَاف", 47 to "مُحَمَّد", 48 to "الفَتْح", 49 to "الحُجُرَات", 50 to "ق",
    51 to "الذَّارِيَات", 52 to "الطُّور", 53 to "النَّجْم", 54 to "القَمَر", 55 to "الرَّحْمَن",
    56 to "الوَاقِعَة", 57 to "الحَدِيد", 58 to "المُجَادَلَة", 59 to "الحَشْر", 60 to "المُمْتَحَنَة",
    61 to "الصَّفّ", 62 to "الجُمُعَة", 63 to "المُنَافِقُون", 64 to "التَّغَابُن", 65 to "الطَّلَاق",
    66 to "التَّحْرِيم", 67 to "المُلْك", 68 to "القَلَم", 69 to "الحَاقَّة", 70 to "المَعَارِج",
    71 to "نُوح", 72 to "الجِنّ", 73 to "المُزَّمِّل", 74 to "المُدَّثِّر", 75 to "القِيَامَة",
    76 to "الإِنْسَان", 77 to "المُرْسَلَات", 78 to "النَّبَأ", 79 to "النَّازِعَات", 80 to "عَبَس",
    81 to "التَّكْوِير", 82 to "الانْفِطَار", 83 to "المُطَفِّفِين", 84 to "الانْشِقَاق", 85 to "البُرُوج",
    86 to "الطَّارِق", 87 to "الأَعْلَى", 88 to "الغَاشِيَة", 89 to "الفَجْر", 90 to "البَلَد",
    91 to "الشَّمْس", 92 to "اللَّيْل", 93 to "الضُّحَى", 94 to "الشَّرْح", 95 to "التِّين",
    96 to "العَلَق", 97 to "القَدْر", 98 to "البَيِّنَة", 99 to "الزَّلْزَلَة", 100 to "العَادِيَات",
    101 to "القَارِعَة", 102 to "التَّكَاثُر", 103 to "العَصْر", 104 to "الهُمَزَة", 105 to "الفِيل",
    106 to "قُرَيْش", 107 to "المَاعُون", 108 to "الكَوْثَر", 109 to "الكافِرُون", 110 to "النَّصْر",
    111 to "المَسَد", 112 to "الإِخْلَاص", 113 to "الفَلَق", 114 to "النَّاس",
)

