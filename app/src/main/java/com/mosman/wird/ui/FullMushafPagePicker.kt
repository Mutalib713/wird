package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayPill

/** Full Mushaf Page Picker allowing user to view the page and tap any ayah. */
@Composable
fun FullMushafPagePicker(
    initialPage: Int,
    onAyahPicked: (surahNumber: Int, ayahNumber: Int, page: Int) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalWirdColors.current
    var currentPage by remember { mutableIntStateOf(initialPage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clayPill(shape = CircleShape, backgroundColor = colors.surface)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tap your starting ayah",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Page $currentPage · Swipe left/right to browse",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                    )
                }
            }

            TextButton(onClick = onClose) {
                Text("Cancel", color = colors.textSecondary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            MushafPager(
                initialPage = initialPage,
                onPageChanged = { currentPage = it },
                lit = { page -> page.glyphs.map { it.verseKey }.toSet() },
                onWordTap = { verseKey ->
                    val s = verseKey.substringBefore(':').toIntOrNull()
                    val a = verseKey.substringAfter(':').toIntOrNull()
                    if (s != null && a != null) {
                        onAyahPicked(s, a, currentPage)
                    }
                },
            )
        }
    }
}
