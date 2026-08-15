package com.mosman.wird.domain

/**
 * Where each surah sits in the mushaf.
 *
 * Generated from the Quran.com API's `/chapters` endpoint on 2026-08-15,
 * not typed from memory. Bundled rather than fetched so surah names work with the radio
 * off and cost no data — it is metadata, not Qur'anic text, so Sacred Rule 2's "verified
 * source" requirement is met by the provenance line above rather than by a live call.
 *
 * Note surahs can *share* a page: Sad ends on 458 and Az-Zumar begins on 458.
 */
data class Surah(val number: Int, val name: String, val firstPage: Int, val lastPage: Int)

object SurahIndex {

    val all: List<Surah> = listOf(
        Surah(1, "Al-Fatihah", 1, 1),
        Surah(2, "Al-Baqarah", 2, 49),
        Surah(3, "Ali 'Imran", 50, 76),
        Surah(4, "An-Nisa", 77, 106),
        Surah(5, "Al-Ma'idah", 106, 127),
        Surah(6, "Al-An'am", 128, 150),
        Surah(7, "Al-A'raf", 151, 176),
        Surah(8, "Al-Anfal", 177, 186),
        Surah(9, "At-Tawbah", 187, 207),
        Surah(10, "Yunus", 208, 221),
        Surah(11, "Hud", 221, 235),
        Surah(12, "Yusuf", 235, 248),
        Surah(13, "Ar-Ra'd", 249, 255),
        Surah(14, "Ibrahim", 255, 261),
        Surah(15, "Al-Hijr", 262, 267),
        Surah(16, "An-Nahl", 267, 281),
        Surah(17, "Al-Isra", 282, 293),
        Surah(18, "Al-Kahf", 293, 304),
        Surah(19, "Maryam", 305, 312),
        Surah(20, "Taha", 312, 321),
        Surah(21, "Al-Anbya", 322, 331),
        Surah(22, "Al-Hajj", 332, 341),
        Surah(23, "Al-Mu'minun", 342, 349),
        Surah(24, "An-Nur", 350, 359),
        Surah(25, "Al-Furqan", 359, 366),
        Surah(26, "Ash-Shu'ara", 367, 376),
        Surah(27, "An-Naml", 377, 385),
        Surah(28, "Al-Qasas", 385, 396),
        Surah(29, "Al-'Ankabut", 396, 404),
        Surah(30, "Ar-Rum", 404, 410),
        Surah(31, "Luqman", 411, 414),
        Surah(32, "As-Sajdah", 415, 417),
        Surah(33, "Al-Ahzab", 418, 427),
        Surah(34, "Saba", 428, 434),
        Surah(35, "Fatir", 434, 440),
        Surah(36, "Ya-Sin", 440, 445),
        Surah(37, "As-Saffat", 446, 452),
        Surah(38, "Sad", 453, 458),
        Surah(39, "Az-Zumar", 458, 467),
        Surah(40, "Ghafir", 467, 476),
        Surah(41, "Fussilat", 477, 482),
        Surah(42, "Ash-Shuraa", 483, 489),
        Surah(43, "Az-Zukhruf", 489, 495),
        Surah(44, "Ad-Dukhan", 496, 498),
        Surah(45, "Al-Jathiyah", 499, 502),
        Surah(46, "Al-Ahqaf", 502, 506),
        Surah(47, "Muhammad", 507, 510),
        Surah(48, "Al-Fath", 511, 515),
        Surah(49, "Al-Hujurat", 515, 517),
        Surah(50, "Qaf", 518, 520),
        Surah(51, "Adh-Dhariyat", 520, 523),
        Surah(52, "At-Tur", 523, 525),
        Surah(53, "An-Najm", 526, 528),
        Surah(54, "Al-Qamar", 528, 531),
        Surah(55, "Ar-Rahman", 531, 534),
        Surah(56, "Al-Waqi'ah", 534, 537),
        Surah(57, "Al-Hadid", 537, 541),
        Surah(58, "Al-Mujadila", 542, 545),
        Surah(59, "Al-Hashr", 545, 548),
        Surah(60, "Al-Mumtahanah", 549, 551),
        Surah(61, "As-Saf", 551, 552),
        Surah(62, "Al-Jumu'ah", 553, 554),
        Surah(63, "Al-Munafiqun", 554, 555),
        Surah(64, "At-Taghabun", 556, 557),
        Surah(65, "At-Talaq", 558, 559),
        Surah(66, "At-Tahrim", 560, 561),
        Surah(67, "Al-Mulk", 562, 564),
        Surah(68, "Al-Qalam", 564, 566),
        Surah(69, "Al-Haqqah", 566, 568),
        Surah(70, "Al-Ma'arij", 568, 570),
        Surah(71, "Nuh", 570, 571),
        Surah(72, "Al-Jinn", 572, 573),
        Surah(73, "Al-Muzzammil", 574, 575),
        Surah(74, "Al-Muddaththir", 575, 577),
        Surah(75, "Al-Qiyamah", 577, 578),
        Surah(76, "Al-Insan", 578, 580),
        Surah(77, "Al-Mursalat", 580, 581),
        Surah(78, "An-Naba", 582, 583),
        Surah(79, "An-Nazi'at", 583, 584),
        Surah(80, "'Abasa", 585, 585),
        Surah(81, "At-Takwir", 586, 586),
        Surah(82, "Al-Infitar", 587, 587),
        Surah(83, "Al-Mutaffifin", 587, 589),
        Surah(84, "Al-Inshiqaq", 589, 589),
        Surah(85, "Al-Buruj", 590, 590),
        Surah(86, "At-Tariq", 591, 591),
        Surah(87, "Al-A'la", 591, 592),
        Surah(88, "Al-Ghashiyah", 592, 592),
        Surah(89, "Al-Fajr", 593, 594),
        Surah(90, "Al-Balad", 594, 594),
        Surah(91, "Ash-Shams", 595, 595),
        Surah(92, "Al-Layl", 595, 596),
        Surah(93, "Ad-Duhaa", 596, 596),
        Surah(94, "Ash-Sharh", 596, 596),
        Surah(95, "At-Tin", 597, 597),
        Surah(96, "Al-'Alaq", 597, 597),
        Surah(97, "Al-Qadr", 598, 598),
        Surah(98, "Al-Bayyinah", 598, 599),
        Surah(99, "Az-Zalzalah", 599, 599),
        Surah(100, "Al-'Adiyat", 599, 600),
        Surah(101, "Al-Qari'ah", 600, 600),
        Surah(102, "At-Takathur", 600, 600),
        Surah(103, "Al-'Asr", 601, 601),
        Surah(104, "Al-Humazah", 601, 601),
        Surah(105, "Al-Fil", 601, 601),
        Surah(106, "Quraysh", 602, 602),
        Surah(107, "Al-Ma'un", 602, 602),
        Surah(108, "Al-Kawthar", 602, 602),
        Surah(109, "Al-Kafirun", 603, 603),
        Surah(110, "An-Nasr", 603, 603),
        Surah(111, "Al-Masad", 603, 603),
        Surah(112, "Al-Ikhlas", 604, 604),
        Surah(113, "Al-Falaq", 604, 604),
        Surah(114, "An-Nas", 604, 604),
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
