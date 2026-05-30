package dev.weft.undercurrent.core.domain.usecase.theme

import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.model.ThemeMode

/** Persist light / dark / auto. */
class SetThemeModeUseCase(
    private val repo: ThemeRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repo.setMode(mode)
}
