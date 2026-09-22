package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayPill

/**
 * Fullscreen Sample Page Preview allowing the reader to see a real 15-line Mushaf page
 * and adjust font sizing interactively, checking whether it fits on one screen without scrolling.
 */
@Composable
fun SamplePagePreviewDialog(
    initialSize: Int,
    onApplySize: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalWirdColors.current
    var currentSize by remember { mutableFloatStateOf(initialSize.toFloat()) }
    var currentPage by remember { mutableIntStateOf(1) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.page)
                .statusBarsPadding(),
        ) {
            // Main page container with dynamic font size applied
            CompositionLocalProvider(
                LocalAyahTextSize provides currentSize,
                LocalCustomAyahSizeEnabled provides true,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
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
                                    text = "Sample Page Preview",
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

                        Button(
                            onClick = {
                                onApplySize(currentSize.toInt())
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Done", color = colors.surface, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }

                    // Mushaf Pager rendering full page
                    Box(modifier = Modifier.weight(1f)) {
                        MushafPager(
                            initialPage = currentPage,
                            onPageChanged = { currentPage = it },
                            lit = { page -> page.glyphs.map { it.verseKey }.toSet() },
                        )
                    }

                    // Floating Bottom Controls for Font Sizing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .border(1.dp, colors.hairline)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Ayah font size",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (currentSize.toInt() != 24) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.accent.copy(alpha = 0.15f))
                                                .clickable { currentSize = 24f }
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = "Reset",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.accent,
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (currentSize.toInt() == 24) "24 sp (default)" else "${currentSize.toInt()} sp",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent,
                                    )
                                    // Status Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (currentSize <= 24f) Color(0xFF1E3A2F) else Color(0xFF382F1D)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = if (currentSize <= 24f) "✓ Fit" else "↕ Scroll",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (currentSize <= 24f) Color(0xFF8ED676) else Color(0xFFE2B768),
                                        )
                                    }
                                }
                            }

                            Slider(
                                value = currentSize,
                                onValueChange = { currentSize = it },
                                valueRange = 18f..32f,
                                steps = 13,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = colors.accent,
                                    inactiveTrackColor = colors.surfaceRaised,
                                ),
                            )

                            Text(
                                text = if (currentSize <= 24f)
                                    "Fits all 15 lines on a single screen without vertical scrolling (Quran for Android style)."
                                else
                                    "Larger Arabic text. Scroll vertically to view all 15 lines.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
