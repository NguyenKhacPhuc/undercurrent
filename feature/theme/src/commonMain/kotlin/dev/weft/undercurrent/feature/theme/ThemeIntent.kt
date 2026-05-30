package dev.weft.undercurrent.feature.theme

import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.ThemeMode

/**
 * Theme-surface intents. Persist palette + dark-mode preferences
 * through the theme repository.
 */
sealed interface ThemeIntent {
    data class SetPalette(val palette: AppPalette) : ThemeIntent
    data class SetThemeMode(val mode: ThemeMode) : ThemeIntent
}
