package com.mosman.wird.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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

/**
 * Adjusts Android status bar icon appearance dynamically.
 *
 * When [isLightBackground] is true, Android draws dark icons (time, battery, Wi-Fi, notifications)
 * so they are crisp and easily readable against light surfaces (paper, cream, white).
 * When [isLightBackground] is false, Android draws white icons for high contrast against dark surfaces
 * (atmospheric dark emerald header, dark mode).
 */
@Composable
fun SetStatusBarAppearance(isLightBackground: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightBackground
        }
    }
}

