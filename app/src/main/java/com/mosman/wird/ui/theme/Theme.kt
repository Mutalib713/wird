package com.mosman.wird.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Wird reads as paper by default, not as whatever the phone is set to.
 *
 * Mutalib's call, 2026-08-15. A mushaf is a paper object, and the app follows the thing
 * it is standing in for rather than the OS. The dark scheme is kept and still correct —
 * flip [FOLLOW_SYSTEM_THEME] to restore system-following, or wire it to a setting when
 * night reading needs it.
 *
 * Dark is designed, not inverted — see [DarkColors], where deep teal stops being an
 * accent (1.78:1 on ink) and becomes a raised surface instead.
 */
const val FOLLOW_SYSTEM_THEME = false

@Composable
fun WirdTheme(
    dark: Boolean = if (FOLLOW_SYSTEM_THEME) isSystemInDarkTheme() else false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWirdColors provides if (dark) DarkColors else LightColors,
        content = content,
    )
}
