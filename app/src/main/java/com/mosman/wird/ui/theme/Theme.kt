package com.mosman.wird.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.mosman.wird.data.ThemeMode

/**
 * Wird reads as paper by default, not as whatever the phone is set to.
 *
 * Mutalib's call, 2026-08-15. A mushaf is a paper object, and the app follows the thing
 * it is standing in for rather than the OS. Since task 5c that default is a *setting*
 * rather than a rule, because he reads at night and paper-white in a dark room is unkind.
 *
 * Dark is designed, not inverted — see [DarkColors], where deep teal stops being an
 * accent (1.78:1 on ink) and becomes a raised surface instead.
 */
@Composable
fun WirdTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalWirdColors provides if (dark) DarkColors else LightColors,
        content = content,
    )
}
