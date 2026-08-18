package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Bookmark
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The Sūrahs tab — the whole Qur'an, reachable.
 *
 * This tab is the visible half of Sacred Rule 5's reversal (2026-08-17). Until today
 * browsing lived in a sheet you had to know to summon by tapping the page; now it is a
 * place. Mutalib's reason was plain: *"I just want this to be my Qur'an app."*
 *
 * It reuses [SurahList] rather than growing a second list — the same question is being
 * asked as in setup, and two lists of 114 surahs would drift apart.
 */
@Composable
fun SurahsTab(
    onPick: (Surah) -> Unit,
    /** Ayahs saved from the verse toolbar. Newest first; empty hides the section entirely. */
    bookmarks: List<Bookmark> = emptyList(),
    /** Jump to a saved ayah's page. */
    onOpenBookmark: (Bookmark) -> Unit = {},
    /** Jump to a bare page, for the "still inside this juz'" rows. */
    onOpenPage: (Int) -> Unit = {},
) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        // The header is inset; the list is not. A Juz' band that stops short of the screen
        // edge reads as a card rather than as a section rule.
        Spacer(Modifier.height(Scale.space6))
        Text(
            "The Qur'an",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.display),
            modifier = Modifier.padding(horizontal = Scale.space4),
        )
        Spacer(Modifier.height(Scale.space2))
        Text(
            // Says plainly that browsing is not cheating and costs you nothing. Today's
            // portion only moves when a day is marked done — never by reading elsewhere.
            text = "Read anywhere you like. Today's portion stays where it is.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
            modifier = Modifier.padding(horizontal = Scale.space4),
        )
        Spacer(Modifier.height(Scale.space4))

        // **Saved ayahs sit above the surah list, when there are any.**
        //
        // The reference makes BOOKMARKS a tab beside SURAHS and JUZ'. Wird has three tabs by
        // his instruction (§ 5t) and a fourth was not asked for, so this goes at the top of
        // the browse surface instead — which is where it belongs anyway: this tab is already
        // "read anywhere you like", and a saved ayah is a place you chose to come back to.
        //
        // It disappears when empty rather than showing an empty state. A heading over nothing
        // teaches you the feature exists at the cost of a permanent hole in the screen.
        if (bookmarks.isNotEmpty()) {
            SavedAyahs(bookmarks, onOpenBookmark)
            Spacer(Modifier.height(Scale.space4))
        }

        SurahList(onPick = onPick, onOpenPage = onOpenPage)
    }
}

/**
 * The ayahs you saved.
 *
 * Capped at six on this screen. A bookmark list is for returning to something, and past a
 * handful the thing you want is search rather than a longer list — so rather than growing
 * without limit above the 114 surahs, it shows the recent ones and says how many more there
 * are.
 */
@Composable
private fun SavedAyahs(bookmarks: List<Bookmark>, onOpen: (Bookmark) -> Unit) {
    val colors = LocalWirdColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.band)
            .padding(horizontal = Scale.space4, vertical = Scale.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // ⚠ textPrimary on the band, not textSecondary: § 6d measured the secondary grey at
        // 3.99:1 there, which fails.
        Text(
            text = "SAVED",
            color = colors.textPrimary,
            style = TextStyle(fontSize = 10.5.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
        )
        Text(
            text = bookmarks.size.toString(),
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
    }

    bookmarks.take(6).forEach { b ->
        val surah = SurahIndex.byNumber(b.verseKey.substringBefore(':').toIntOrNull() ?: 0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen(b) }
                .defaultMinSize(minHeight = Scale.minTarget)
                .padding(horizontal = Scale.space4, vertical = Scale.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${surah?.name ?: "Ayah"} ${b.verseKey}",
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = Scale.body),
                )
                Text(
                    text = savedWhen(b.savedAt.toLocalDate()),
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            surah?.let {
                Text(
                    text = pageOfVerse(b.verseKey, it).toString(),
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.body),
                )
            }
        }
    }

    if (bookmarks.size > 6) {
        Text(
            text = "and ${bookmarks.size - 6} more",
            color = colors.textOutsidePortion,
            style = TextStyle(fontSize = Scale.caption),
            modifier = Modifier.padding(horizontal = Scale.space4, vertical = Scale.space2),
        )
    }
}

/**
 * The surah's opening page, which is the best this can do without loading the page data.
 *
 * ⚠ Honest limitation: an ayah deep inside Al-Baqarah shows page 2 rather than its real page.
 * Resolving it properly means the page layout for that ayah, which is a network fetch or a
 * cache read per row — too much for a list that exists to be glanced at. Tapping still lands
 * you on the right ayah, because that lookup happens once, on open.
 */
private fun pageOfVerse(verseKey: String, surah: Surah): Int = surah.firstPage

/** "Today", "Yesterday", or the date. Nobody needs a timestamp on a bookmark. */
private fun savedWhen(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Saved today"
        today.minusDays(1) -> "Saved yesterday"
        else -> "Saved " + date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}
