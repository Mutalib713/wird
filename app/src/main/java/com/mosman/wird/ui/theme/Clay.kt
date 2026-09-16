package com.mosman.wird.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Claymorphic modifiers providing the soft inflated 3D tactile card look
 * with dual-light gradient bevels (top-left highlight, bottom-right depth shadow)
 * and ambient elevation drop shadows.
 */
fun Modifier.clayCard(
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color,
    highlightColor: Color = Color.White.copy(alpha = 0.4f),
    shadowColor: Color = Color.Black.copy(alpha = 0.25f),
    elevation: Dp = 6.dp,
    strokeWidth: Dp = 1.dp,
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowColor.copy(alpha = 0.25f),
            spotColor = shadowColor.copy(alpha = 0.35f),
        )
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = strokeWidth,
            brush = Brush.linearGradient(listOf(highlightColor, shadowColor)),
            shape = shape,
        )
}

fun Modifier.clayPill(
    shape: Shape = RoundedCornerShape(999.dp),
    backgroundColor: Color,
    highlightColor: Color = Color.White.copy(alpha = 0.35f),
    shadowColor: Color = Color.Black.copy(alpha = 0.2f),
    elevation: Dp = 4.dp,
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowColor.copy(alpha = 0.2f),
            spotColor = shadowColor.copy(alpha = 0.3f),
        )
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(highlightColor, shadowColor)),
            shape = shape,
        )
}

val ARABIC_DIGITS = listOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

/** Converts an integer (like 35) into Arabic numerals (like ٣٥) */
fun arabicNumerals(n: Int): String =
    n.toString().map { ARABIC_DIGITS[it - '0'] }.joinToString("")
