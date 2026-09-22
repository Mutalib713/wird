package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mosman.wird.R
import com.mosman.wird.audio.PortionAudio
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

/**
 * Dialog to select an ayah range and configure repetition counts for memorisation (Hifdh).
 */
@Composable
fun RangePlayDialog(
    startVerse: String,
    onDismiss: () -> Unit,
    onPlayRange: (verses: List<String>, repeatRange: Int, repeatEach: Int) -> Unit,
) {
    val colors = LocalWirdColors.current
    val surahNumber = startVerse.substringBefore(':').toIntOrNull() ?: 1
    val initialAyah = startVerse.substringAfter(':').toIntOrNull() ?: 1
    val surah = SurahIndex.byNumber(surahNumber)
    val maxAyahs = surah?.verses ?: 286

    var fromAyah by remember { mutableIntStateOf(initialAyah) }
    var toAyah by remember { mutableIntStateOf(minOf(initialAyah + 4, maxAyahs)) }
    var repeatRange by remember { mutableIntStateOf(1) }
    var repeatEach by remember { mutableIntStateOf(1) }

    val ayahCount = (toAyah - fromAyah + 1).coerceAtLeast(1)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clayCard(shape = RoundedCornerShape(24.dp), backgroundColor = colors.surface)
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Repeat & Loop",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            ),
                        )
                        Text(
                            text = "${surah?.name.orEmpty()} (Sūrah $surahNumber)",
                            style = TextStyle(fontSize = 12.5.sp, color = colors.accent),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clayPill(shape = CircleShape, backgroundColor = colors.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_repeat),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Ayah Range Selectors (From & To)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // From Ayah Stepper
                    AyahStepper(
                        label = "From Ayah",
                        value = fromAyah,
                        min = 1,
                        max = toAyah,
                        onValueChange = { fromAyah = it },
                        modifier = Modifier.weight(1f),
                    )

                    // To Ayah Stepper
                    AyahStepper(
                        label = "To Ayah",
                        value = toAyah,
                        min = fromAyah,
                        max = maxAyahs,
                        onValueChange = { toAyah = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Selection Summary Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceRaised)
                        .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Ayahs $fromAyah to $toAyah · $ayahCount ${if (ayahCount == 1) "ayah" else "ayahs"}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        ),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Repeat Whole Range
                Text(
                    text = "Repeat whole range",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    ),
                )
                Text(
                    text = "How many times to loop all $ayahCount ayahs together",
                    style = TextStyle(fontSize = 11.sp, color = colors.textSecondary),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val rangeOptions = listOf(1, 2, 3, 5, 10, PortionAudio.FOREVER)
                    rangeOptions.forEach { count ->
                        val isSelected = repeatRange == count
                        val label = if (count == PortionAudio.FOREVER) "∞" else "${count}x"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else colors.surfaceRaised)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colors.accent else colors.hairline,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { repeatRange = count }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.surface else colors.textPrimary,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Repeat Each Ayah
                Text(
                    text = "Repeat each ayah",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    ),
                )
                Text(
                    text = "Times each individual ayah plays before moving to the next",
                    style = TextStyle(fontSize = 11.sp, color = colors.textSecondary),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 2, 3).forEach { count ->
                        val isSelected = repeatEach == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else colors.surfaceRaised)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colors.accent else colors.hairline,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { repeatEach = count }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${count}x per ayah",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.surface else colors.textPrimary,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel", color = colors.textSecondary)
                    }

                    Button(
                        onClick = {
                            val verses = (fromAyah..toAyah).map { "$surahNumber:$it" }
                            onPlayRange(verses, repeatRange, repeatEach)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colors.surface,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Loop $ayahCount ${if (ayahCount == 1) "Ayah" else "Ayahs"}",
                            color = colors.surface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 11.5.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Minus button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (value > min) colors.surface else colors.surface.copy(alpha = 0.4f))
                    .clickable(enabled = value > min) { onValueChange(value - 1) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "−",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (value > min) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                )
            }

            Text(
                text = value.toString(),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )

            // Plus button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (value < max) colors.surface else colors.surface.copy(alpha = 0.4f))
                    .clickable(enabled = value < max) { onValueChange(value + 1) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (value < max) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
