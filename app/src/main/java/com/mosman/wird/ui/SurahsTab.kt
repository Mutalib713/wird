package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.mosman.wird.domain.Surah
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
fun SurahsTab(onPick: (Surah) -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = Scale.space6),
    ) {
        Spacer(Modifier.height(Scale.space6))
        Text("The Qur'an", color = colors.textPrimary, style = TextStyle(fontSize = Scale.display))
        Spacer(Modifier.height(Scale.space2))
        Text(
            // Says plainly that browsing is not cheating and costs you nothing. Today's
            // portion only moves when a day is marked done — never by reading elsewhere.
            text = "Read anywhere you like. Today's portion stays where it is.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
        Spacer(Modifier.height(Scale.space4))
        SurahList(onPick = onPick)
    }
}
