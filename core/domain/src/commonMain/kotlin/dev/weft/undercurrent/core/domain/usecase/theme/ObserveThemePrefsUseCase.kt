package dev.weft.undercurrent.core.domain.usecase.theme

import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.model.ThemePrefs
import kotlinx.coroutines.flow.Flow

/**
 * Observable read of the user's theme prefs (palette + mode). The
 * App composable + any feature surface that wants to know "what's
 * the current theme" collects this.
 */
class ObserveThemePrefsUseCase(
    private val repo: ThemeRepository,
) {
    operator fun invoke(): Flow<ThemePrefs> = repo.prefsFlow
}
