package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Bookmark
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.arabicName
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.SetStatusBarAppearance
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.format.DateTimeFormatter

/**
 * Dedicated Bookmarks & Recents screen.
 *
 * Provides two clean sections:
 * 1. "Recent Pages": recently opened and read Mushaf pages.
 * 2. "Ayah Bookmarks": explicitly bookmarked verses with removal action.
 *
 * Implements Sacred Rule 6: vector drawables exclusively (no raw emojis).
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
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)
    val gold = Color(0xFFC9A24B)

    // 0 = Recent pages, 1 = Ayah bookmarks
    var selectedTab by remember { mutableIntStateOf(0) }

    // Handle system back gesture
    BackHandler(onBack = onBack)

    SetStatusBarAppearance(isLightBackground = !isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(groundColor)
            .statusBarsPadding(),
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color(0xFFE4E9E5) else Color(0xFF1E3F32),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Bookmarks & Recents",
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        text = if (selectedTab == 0) "${recentPages.size} recent pages" else "${bookmarks.size} saved ayahs",
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Pill Tabs Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clayCard(
                    shape = RoundedCornerShape(999.dp),
                    backgroundColor = if (isDark) Color(0xFF111E18) else Color(0xFFECE7DA),
                    elevation = 1.dp,
                )
                .padding(4.dp),
        ) {
            // Tab 1: Recent Pages
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (selectedTab == 0) {
                            if (isDark) Color(0xFF245847) else Color(0xFF2D6B52)
                        } else Color.Transparent,
                        elevation = if (selectedTab == 0) 2.dp else 0.dp,
                    )
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent Pages",
                        color = if (selectedTab == 0) Color.White else colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (recentPages.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = CircleShape,
                                    backgroundColor = if (selectedTab == 0) Color.White.copy(alpha = 0.25f) else colors.ornament.copy(alpha = 0.15f),
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "${recentPages.size}",
                                color = if (selectedTab == 0) Color.White else colors.textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Tab 2: Ayah Bookmarks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (selectedTab == 1) {
                            if (isDark) Color(0xFF245847) else Color(0xFF2D6B52)
                        } else Color.Transparent,
                        elevation = if (selectedTab == 1) 2.dp else 0.dp,
                    )
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ayah Bookmarks",
                        color = if (selectedTab == 1) Color.White else colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (bookmarks.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = CircleShape,
                                    backgroundColor = if (selectedTab == 1) Color.White.copy(alpha = 0.25f) else colors.ornament.copy(alpha = 0.15f),
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "${bookmarks.size}",
                                color = if (selectedTab == 1) Color.White else colors.textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Content
        if (selectedTab == 0) {
            // Recent Pages Section
            if (recentPages.isEmpty()) {
                EmptyStateView(
                    icon = { BookVectorLarge(tint = colors.textSecondary.copy(alpha = 0.5f)) },
                    title = "No recent pages yet",
                    message = "Pages you open and read in the Mushaf will automatically appear here for quick access.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
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
                    icon = { BookmarkVectorLarge(tint = gold.copy(alpha = 0.6f)) },
                    title = "No saved ayahs",
                    message = "Tap the bookmark star while reading any ayah to keep it saved on this phone.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
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
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    // Surahs spanning this page
    val surahs = remember(page) { SurahIndex.all.filter { page in it.firstPage..it.lastPage } }
    val primarySurah = surahs.firstOrNull()
    val juz = remember(page) { JuzIndex.all.lastOrNull { it.firstPage <= page }?.number ?: 1 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFFFFFFF),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                elevation = 2.dp,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Squircle Page Seal
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clayCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isDark) Color(0xFF0F1A15) else Color(0xFFF3ECE0),
                        elevation = 1.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$page",
                        color = gold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "PAGE",
                        color = colors.textSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = primarySurah?.name ?: "Page $page",
                    color = colors.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Juz' $juz · ${primarySurah?.meaning ?: ""}",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        // Open indicator
        Box(
            modifier = Modifier
                .clayPill(
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = if (isDark) Color(0xFF1E352B) else Color(0xFFE4EFE8),
                    elevation = 1.dp,
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = "Open",
                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
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
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    val surahNum = bookmark.verseKey.substringBefore(':').toIntOrNull() ?: 1
    val ayahNum = bookmark.verseKey.substringAfter(':').toIntOrNull() ?: 1
    val surah = remember(surahNum) { SurahIndex.byNumber(surahNum) }
    val dateLabel = remember(bookmark.savedAt) {
        bookmark.savedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFFFFFFF),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                elevation = 2.dp,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Star vector icon
                StarVectorIcon(tint = gold)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${surah?.name ?: "Surah $surahNum"} · ${bookmark.verseKey}",
                    color = gold,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Remove button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clayPill(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF2A1E1E) else Color(0xFFF9EBEB),
                        elevation = 1.dp,
                    )
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove bookmark",
                    tint = if (isDark) Color(0xFFE29F9F) else Color(0xFF9C4A4A),
                    modifier = Modifier.size(14.dp),
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
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            surah?.arabicName?.let { arName ->
                Text(
                    text = "سُورَةُ $arName",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
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
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
            Text(
                text = "Tap to view page →",
                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Elegant empty state display with vector iconography. */
@Composable
private fun EmptyStateView(
    icon: @Composable () -> Unit,
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
        icon()
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Vector star icon matching Sacred Rule 6. */
@Composable
private fun StarVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.62f, h * 0.35f)
            lineTo(w * 0.95f, h * 0.38f)
            lineTo(w * 0.70f, h * 0.60f)
            lineTo(w * 0.78f, h * 0.92f)
            lineTo(w * 0.50f, h * 0.74f)
            lineTo(w * 0.22f, h * 0.92f)
            lineTo(w * 0.30f, h * 0.60f)
            lineTo(w * 0.05f, h * 0.38f)
            lineTo(w * 0.38f, h * 0.35f)
            close()
        }
        drawPath(path, color = tint)
    }
}

/** Large vector book illustration for empty state. */
@Composable
private fun BookVectorLarge(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(54.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        listOf(-1f, 1f).forEach { side ->
            val outer = Offset(w * (0.5f + side * 0.42f), h * 0.22f)
            val inner = Offset(w * 0.5f, h * 0.32f)
            drawLine(tint, outer, inner, stroke.width, stroke.cap)
            drawLine(tint, outer, Offset(outer.x, h * 0.80f), stroke.width, stroke.cap)
            drawLine(tint, Offset(outer.x, h * 0.80f), Offset(w * 0.5f, h * 0.86f), stroke.width, stroke.cap)
        }
        drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.86f), stroke.width, stroke.cap)
    }
}

/** Large vector bookmark ribbon illustration for empty state. */
@Composable
private fun BookmarkVectorLarge(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(54.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.68f)
            lineTo(w * 0.28f, h * 0.85f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}
