package com.mosman.wird.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Dark is designed, not inverted — see [DarkColors], where deep teal stops being an
 * accent (1.78:1 on ink) and becomes a raised surface instead.
 */
@Composable
fun WirdTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWirdColors provides if (dark) DarkColors else LightColors,
        content = content,
    )
}
