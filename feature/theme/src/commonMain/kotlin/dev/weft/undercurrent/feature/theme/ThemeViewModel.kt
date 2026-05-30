package dev.weft.undercurrent.feature.theme

import dev.weft.undercurrent.core.domain.usecase.theme.ObserveThemePrefsUseCase
import dev.weft.undercurrent.core.domain.usecase.theme.SetPaletteUseCase
import dev.weft.undercurrent.core.domain.usecase.theme.SetThemeModeUseCase
import dev.weft.undercurrent.core.model.ThemePrefs
import dev.weft.undercurrent.shared.mvi.MviViewModel

class ThemeViewModel(
    private val setPalette: SetPaletteUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    observeThemePrefs: ObserveThemePrefsUseCase,
) : MviViewModel<ThemeState, ThemeIntent, ThemeEffect>(
    initialState = ThemeState(),
) {
    init {
        observeThemePrefs().collectInto { copy(prefs = it) }
    }

    override fun dispatch(intent: ThemeIntent) = launch {
        when (intent) {
            is ThemeIntent.SetPalette -> setPalette(intent.palette)
            is ThemeIntent.SetThemeMode -> setThemeMode(intent.mode)
        }
    }
}

data class ThemeState(
    val prefs: ThemePrefs = ThemePrefs.Default,
)

sealed interface ThemeEffect
