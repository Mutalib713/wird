package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.RevealedIn
import com.mosman.wird.domain.arabicName
import com.mosman.wird.domain.surahs
import com.mosman.wird.ui.theme.LocalWirdColors

enum class QuranSubView { MAIN, SURAHS, JUZ }
private enum class SurahFilter { ALL, MAKKI, MADANI }

/**
 * Qur'an destination in the new look (PROFILE § 5bj, PLAN task 28).
 * Search, continue reading, quick access (Bookmarks, Recent, Downloads),
 * and browsing across Sūrahs, Juz', and direct Page selection.
 */
@Composable
fun QuranTab(
    lastReadPage: Int,
    onOpenPage: (Int) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    initialSubView: QuranSubView = QuranSubView.MAIN,
) {
    val colors = LocalWirdColors.current
    var subView by remember(initialSubView) { mutableStateOf(initialSubView) }
    var showPageDialog by remember { mutableStateOf(false) }

    when (subView) {
        QuranSubView.MAIN -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(colors.field)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Qur'an",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(14.dp))

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.card)
                        .clickable { onOpenSearch() }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = WirdIcons.Search,
                        contentDescription = "Search",
                        tint = colors.ink2,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Search the Qur'an",
                        color = colors.ink2,
                        fontSize = 15.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Continue Reading Card
                val surahAtPage = remember(lastReadPage) {
                    SurahIndex.on(lastReadPage).firstOrNull()
                }
                val juzAtPage = remember(lastReadPage) {
                    JuzIndex.of(lastReadPage).number
                }

                Text(
                    text = "Continue reading",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(44.dp)
                                .background(colors.action, RoundedCornerShape(3.dp)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surahAtPage?.name ?: "Al-Fatihah",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                ),
                            )
                            Text(
                                text = "Page $lastReadPage · Juz' $juzAtPage",
                                fontSize = 13.sp,
                                color = colors.ink2,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(colors.action)
                                .clickable { onOpenPage(lastReadPage) }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Continue",
                                color = colors.onAction,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Quick Access 3 Tiles
                Text(
                    text = "Quick access",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickAccessTile(
                        icon = WirdIcons.Bookmark,
                        label = "Bookmarks",
                        onClick = onOpenBookmarks,
                        modifier = Modifier.weight(1f),
                    )
                    QuickAccessTile(
                        icon = WirdIcons.History,
                        label = "Recent",
                        onClick = { onOpenPage(lastReadPage) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickAccessTile(
                        icon = WirdIcons.Download,
                        label = "Downloads",
                        onClick = onOpenDownloads,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Browse Section
                Text(
                    text = "Browse",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        BrowseRow(
                            icon = WirdIcons.List,
                            title = "Sūrahs",
                            subtitle = "114 chapters",
                            onClick = { subView = QuranSubView.SURAHS },
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                        BrowseRow(
                            icon = WirdIcons.Layers,
                            title = "Juz'",
                            subtitle = "30 parts",
                            onClick = { subView = QuranSubView.JUZ },
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                        BrowseRow(
                            icon = WirdIcons.Quran,
                            title = "Page",
                            subtitle = "Go to any of the 604 pages",
                            onClick = { showPageDialog = true },
                        )
                    }
                }
            }
        }

        QuranSubView.SURAHS -> {
            SurahsBrowserScreen(
                onBack = { subView = QuranSubView.MAIN },
                onSelectSurah = { surah ->
                    onOpenPage(surah.firstPage)
                },
            )
        }

        QuranSubView.JUZ -> {
            JuzBrowserScreen(
                onBack = { subView = QuranSubView.MAIN },
                onSelectJuzPage = { page ->
                    onOpenPage(page)
                },
            )
        }
    }

    if (showPageDialog) {
        PagePickerDialog(
            onDismiss = { showPageDialog = false },
            onPick = { page ->
                showPageDialog = false
                onOpenPage(page)
            },
        )
    }
}

@Composable
private fun QuickAccessTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(colors.disc, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = colors.action,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink,
            )
        }
    }
}

@Composable
private fun BrowseRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(colors.disc, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.action,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.ink2,
            )
        }
        Icon(
            imageVector = WirdIcons.ChevronRight,
            contentDescription = "Open",
            tint = colors.ink2,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SurahsBrowserScreen(
    onBack: () -> Unit,
    onSelectSurah: (Surah) -> Unit,
) {
    val colors = LocalWirdColors.current
    var filter by remember { mutableStateOf(SurahFilter.ALL) }

    val allSurahs = remember { SurahIndex.all }
    val filtered = remember(filter) {
        when (filter) {
            SurahFilter.ALL -> allSurahs
            SurahFilter.MAKKI -> allSurahs.filter { it.revealedIn == RevealedIn.MAKKI }
            SurahFilter.MADANI -> allSurahs.filter { it.revealedIn == RevealedIn.MADANI }
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding(),
    ) {
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
            Text(
                text = "Sūrahs",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
            )
        }

        // Filters: All, Makki, Madani
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                label = "All",
                selected = filter == SurahFilter.ALL,
                onClick = { filter = SurahFilter.ALL },
            )
            FilterChip(
                label = "Makki",
                selected = filter == SurahFilter.MAKKI,
                onClick = { filter = SurahFilter.MAKKI },
            )
            FilterChip(
                label = "Madani",
                selected = filter == SurahFilter.MADANI,
                onClick = { filter = SurahFilter.MADANI },
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            items(filtered, key = { it.number }) { surah ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectSurah(surah) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(colors.field, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = surah.number.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surah.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            )
                            Text(
                                text = "${surah.verses} verses · ${if (surah.revealedIn == RevealedIn.MAKKI) "Makki" else "Madani"}",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                        }
                        Text(
                            text = surah.arabicName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.action else colors.chip)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.onAction else colors.ink,
        )
    }
}

@Composable
private fun JuzBrowserScreen(
    onBack: () -> Unit,
    onSelectJuzPage: (Int) -> Unit,
) {
    val colors = LocalWirdColors.current
    // 30 Juz definitions: Juz index to first page
    val juzPages = remember {
        listOf(
            1 to 1, 2 to 22, 3 to 42, 4 to 62, 5 to 82,
            6 to 102, 7 to 121, 8 to 142, 9 to 162, 10 to 182,
            11 to 201, 12 to 222, 13 to 242, 14 to 262, 15 to 282,
            16 to 302, 17 to 322, 18 to 342, 19 to 362, 20 to 382,
            21 to 402, 22 to 422, 23 to 442, 24 to 462, 25 to 482,
            26 to 502, 27 to 522, 28 to 542, 29 to 562, 30 to 582,
        )
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding(),
    ) {
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
            Text(
                text = "Juz'",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            items(juzPages) { (juzNumber, startPage) ->
                val surahName = SurahIndex.on(startPage).firstOrNull()?.name ?: ""
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectJuzPage(startPage) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(colors.field, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = juzNumber.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Juz' $juzNumber",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            )
                            Text(
                                text = "Page $startPage · $surahName",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                        }
                        Icon(
                            imageVector = WirdIcons.ChevronRight,
                            contentDescription = "Open",
                            tint = colors.ink2,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PagePickerDialog(
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val colors = LocalWirdColors.current
    var text by remember { mutableStateOf("") }
    val pageNum = text.toIntOrNull()
    val isValid = pageNum != null && pageNum in 1..Mushaf.PAGES

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Go to page",
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink,
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a page number between 1 and 604.",
                    fontSize = 14.sp,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) text = it },
                    singleLine = true,
                    placeholder = { Text("e.g. 255") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { pageNum?.let { onPick(it) } },
                enabled = isValid,
            ) {
                Text("Open", color = if (isValid) colors.action else colors.ink2)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.ink2)
            }
        },
        containerColor = colors.card,
    )
}
