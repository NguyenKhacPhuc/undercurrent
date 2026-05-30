package dev.weft.undercurrent.core.domain.usecase.theme

import org.koin.dsl.module

val themeUseCasesModule = module {
    factory { SetPaletteUseCase(repo = get()) }
    factory { SetThemeModeUseCase(repo = get()) }
    factory { ObserveThemePrefsUseCase(repo = get()) }
}
