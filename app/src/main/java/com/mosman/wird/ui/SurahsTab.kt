package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Bookmark
import com.mosman.wird.domain.Surah
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.SetStatusBarAppearance
import com.mosman.wird.ui.theme.clayCard

/**
 * The Sūrahs tab — the whole Qur'an, tactile and reachable.
 *
 * Redesigned with claymorphic cards, squircle Arabic numeral badges,
 * and an on-demand top-app-bar search icon matching standard Android design patterns.
 *
 * The bookmarks section has been removed from this screen per user instruction,
 * allowing Sūrah 1 (Al-Fatihah) to sit right under the top header.
 */
@Composable
fun SurahsTab(
    onPick: (Surah) -> Unit,
    /** Kept in signature for call-site compatibility; bookmark card removed per user instruction. */
    bookmarks: List<Bookmark> = emptyList(),
    onOpenBookmark: (Bookmark) -> Unit = {},
    /** Jump to a bare page, for the "still inside this juz'" rows. */
    onOpenPage: (Int) -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)

    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            focusRequester.requestFocus()
        }
    }

    SetStatusBarAppearance(isLightBackground = !isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(groundColor)
            .statusBarsPadding(),
    ) {
        // App top bar
        if (!searchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Wırd",
                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        )
                        Spacer(Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .offset(y = (-7).dp)
                                .background(Color(0xFF50A773), CircleShape),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "The 114 Sūrahs",
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Tactile Clay Search Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clayCard(
                            shape = CircleShape,
                            backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                            elevation = 3.dp,
                        )
                        .clickable { searchActive = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search Qur'an",
                        tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF1E3F32),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else {
            // Active Search Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tactile Search Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clayCard(
                            shape = RoundedCornerShape(999.dp),
                            backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.2f),
                            elevation = 3.dp,
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF50A773),
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search by name or number...",
                                    color = if (isDark) Color(0xFF6E8276) else Color(0xFF8C7D6B),
                                    fontSize = 13.5.sp,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                                    fontWeight = FontWeight.Medium,
                                ),
                                cursorBrush = SolidColor(Color(0xFF50A773)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            )
                        }
                        if (query.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF50A773),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { query = "" }
                                    .padding(start = 6.dp, end = 2.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Close Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clayCard(
                            shape = CircleShape,
                            backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                            elevation = 3.dp,
                        )
                        .clickable {
                            searchActive = false
                            query = ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close search",
                        tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (query.isNotBlank()) {
                val matchCount = remember(query) { searchSurahs(query).size }
                Text(
                    text = if (matchCount == 1) "1 MATCH FOUND" else "$matchCount MATCHES FOUND",
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        SurahList(
            onPick = onPick,
            onOpenPage = onOpenPage,
            searchQuery = if (searchActive) query else "",
            contentPadding = PaddingValues(bottom = 110.dp),
        )
    }
}
