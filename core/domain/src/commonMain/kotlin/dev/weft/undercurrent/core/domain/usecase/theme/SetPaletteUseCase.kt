package dev.weft.undercurrent.core.domain.usecase.theme

import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.model.AppPalette

/**
 * Persist the user's palette choice. The repo's [ThemeRepository
 * .prefsFlow] re-emits afterwards; the ThemeViewModel + any other
 * observers update their state automatically.
 */
class SetPaletteUseCase(
    private val repo: ThemeRepository,
) {
    suspend operator fun invoke(palette: AppPalette) = repo.setPalette(palette)
}
