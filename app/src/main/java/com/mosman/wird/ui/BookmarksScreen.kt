package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Bookmark
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.arabicName
import com.mosman.wird.ui.theme.LocalWirdColors
import java.time.format.DateTimeFormatter

/**
 * Bookmarks & Recents screen redesign matching the New Look design tokens (PROFILE § 5bj, PLAN task 28).
 * Provides two clean views:
 * 1. "Recent Pages": recently opened and read Mushaf pages.
 * 2. "Bookmarks": explicitly bookmarked verses with removal action.
 * Strictly uses vector iconography from [WirdIcons] (Sacred Rule 6).
 */
@Composable
fun BookmarksScreen(
    bookmarks: List<Bookmark>,
    recentPages: List<Int>,
    onOpenPage: (Int) -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalWirdColors.current

    // 0 = Recent pages, 1 = Ayah bookmarks
    var selectedTab by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding(),
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = WirdIcons.Back,
                    contentDescription = "Back",
                    tint = colors.ink,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bookmarks & Recents",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                Text(
                    text = if (selectedTab == 0) "${recentPages.size} recent pages" else "${bookmarks.size} saved ayahs",
                    color = colors.ink2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Segmented Control Pill Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.card)
                .padding(4.dp),
        ) {
            // Tab 1: Recent Pages
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == 0) colors.action else colors.card)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent Pages",
                        color = if (selectedTab == 0) colors.onAction else colors.ink2,
                        fontSize = 13.5.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    if (recentPages.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selectedTab == 0) colors.onAction.copy(alpha = 0.2f) else colors.tile)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "${recentPages.size}",
                                color = if (selectedTab == 0) colors.onAction else colors.ink2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Tab 2: Bookmarks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == 1) colors.action else colors.card)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bookmarks",
                        color = if (selectedTab == 1) colors.onAction else colors.ink2,
                        fontSize = 13.5.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    if (bookmarks.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selectedTab == 1) colors.onAction.copy(alpha = 0.2f) else colors.tile)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "${bookmarks.size}",
                                color = if (selectedTab == 1) colors.onAction else colors.ink2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content
        if (selectedTab == 0) {
            // Recent Pages Section
            if (recentPages.isEmpty()) {
                EmptyStateView(
                    icon = WirdIcons.Sheet,
                    title = "No recent pages yet",
                    message = "Pages you open and read in the Mushaf will automatically appear here for quick access.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(recentPages, key = { it }) { page ->
                        RecentPageCard(
                            page = page,
                            onClick = { onOpenPage(page) },
                        )
                    }
                }
            }
        } else {
            // Ayah Bookmarks Section
            if (bookmarks.isEmpty()) {
                EmptyStateView(
                    icon = WirdIcons.Bookmark,
                    title = "No saved bookmarks",
                    message = "Tap the bookmark icon while reading any ayah to keep it saved on this device.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(bookmarks, key = { it.verseKey }) { bookmark ->
                        AyahBookmarkCard(
                            bookmark = bookmark,
                            onClick = { onOpenBookmark(bookmark) },
                            onRemove = { onRemoveBookmark(bookmark.verseKey) },
                        )
                    }
                }
            }
        }
    }
}

/** Card representing a recently visited Mushaf page. */
@Composable
private fun RecentPageCard(
    page: Int,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current

    val surahs = remember(page) { SurahIndex.all.filter { page in it.firstPage..it.lastPage } }
    val primarySurah = surahs.firstOrNull()
    val juz = remember(page) { JuzIndex.all.lastOrNull { it.firstPage <= page }?.number ?: 1 }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.rule),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Page Number badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.disc),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$page",
                            color = colors.action,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "PAGE",
                            color = colors.ink2,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = primarySurah?.name ?: "Page $page",
                        color = colors.ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Juz' $juz · ${primarySurah?.meaning ?: ""}",
                        color = colors.ink2,
                        fontSize = 13.sp,
                    )
                }
            }

            // Arrow right
            Icon(
                imageVector = WirdIcons.ChevronRight,
                contentDescription = "Open",
                tint = colors.ink2,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Card representing a saved ayah bookmark. */
@Composable
private fun AyahBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalWirdColors.current

    val surahNum = bookmark.verseKey.substringBefore(':').toIntOrNull() ?: 1
    val ayahNum = bookmark.verseKey.substringAfter(':').toIntOrNull() ?: 1
    val surah = remember(surahNum) { SurahIndex.byNumber(surahNum) }
    val dateLabel = remember(bookmark.savedAt) {
        bookmark.savedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.rule),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(colors.tile, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = WirdIcons.Bookmark,
                            contentDescription = null,
                            tint = colors.goldText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = "${surah?.name ?: "Surah $surahNum"} · ${bookmark.verseKey}",
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = WirdIcons.Close,
                        contentDescription = "Remove bookmark",
                        tint = colors.ink2,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${surah?.meaning ?: ""} · Ayah $ayahNum",
                    color = colors.ink2,
                    fontSize = 13.sp,
                )
                surah?.arabicName?.let { arName ->
                    Text(
                        text = "سُورَةُ $arName",
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Footer jump row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Saved $dateLabel",
                    color = colors.ink2,
                    fontSize = 12.sp,
                )
                Text(
                    text = "Tap to view page →",
                    color = colors.action,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Vector empty state view adhering to Sacred Rule 6. */
@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.tile, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = colors.ink2,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
