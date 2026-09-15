package com.mosman.wird.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayPill

/**
 * Option C: Floating Island Capsule Dock
 * Rests floating 16dp above the bottom navigation bar.
 * Features 3 navigation tabs: Home, Sūrahs, and History.
 */
@Composable
fun FloatingIslandDock(
    current: WirdTab,
    onPick: (WirdTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    val dockBackground = if (isDark) Color(0xFF101B16).copy(alpha = 0.94f) else Color(0xFFF7F4EC).copy(alpha = 0.96f)
    val dockHighlight = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.85f)
    val dockShadow = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF8C7D5F).copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clayPill(
                    shape = RoundedCornerShape(999.dp),
                    backgroundColor = dockBackground,
                    highlightColor = dockHighlight,
                    shadowColor = dockShadow,
                    elevation = 10.dp,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WirdTab.entries.forEach { tab ->
                val active = tab == current
                val activeBackground = if (isDark) Color(0xFF183F32) else Color(0xFF245847)
                val activeContentColor = if (isDark) Color(0xFF8ED676) else Color(0xFFFFFFFF)
                val inactiveContentColor = if (isDark) Color(0xFF798E82) else Color(0xFF6A7C73)

                val contentColor by animateColorAsState(
                    targetValue = if (active) activeContentColor else inactiveContentColor,
                    animationSpec = tween(250),
                    label = "dockContentColor",
                )

                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .then(
                            if (active) {
                                Modifier.clayPill(
                                    shape = RoundedCornerShape(999.dp),
                                    backgroundColor = activeBackground,
                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.25f else 0.35f),
                                    shadowColor = Color.Black.copy(alpha = 0.45f),
                                    elevation = 4.dp,
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onPick(tab) },
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (tab) {
                            WirdTab.HOME -> {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            WirdTab.SURAHS -> {
                                DockBookIcon(tint = contentColor)
                            }
                            WirdTab.HISTORY -> {
                                DockClockIcon(tint = contentColor)
                            }
                        }

                        if (active) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Crisp vector open book icon for Sūrahs tab */
@Composable
private fun DockBookIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
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

/** Crisp vector clock icon for History tab */
@Composable
private fun DockClockIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val r = size.minDimension * 0.42f
        drawCircle(color = tint, radius = r, center = center, style = stroke)
        // Hour and minute hands
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - r * 0.55f),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + r * 0.45f, center.y),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
    }
}
