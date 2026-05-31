package dev.weft.undercurrent.feature.theme

import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.model.ThemePrefs
import dev.weft.undercurrent.shared.mvi.MviViewModel

class ThemeViewModel(
    private val repo: ThemeRepository,
) : MviViewModel<ThemeState, ThemeIntent, ThemeEffect>(
    initialState = ThemeState(),
) {
    init {
        repo.prefsFlow.collectInto { copy(prefs = it) }
    }

    override fun dispatch(intent: ThemeIntent) = launch {
        when (intent) {
            is ThemeIntent.SetPalette -> repo.setPalette(intent.palette)
            is ThemeIntent.SetThemeMode -> repo.setMode(intent.mode)
        }
    }
}

data class ThemeState(
    val prefs: ThemePrefs = ThemePrefs.Default,
)

sealed interface ThemeEffect
