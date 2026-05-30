package dev.weft.undercurrent.feature.theme

import org.koin.dsl.module

val themeModule = module {
    single {
        ThemeViewModel(
            setPalette = get(),
            setThemeMode = get(),
            observeThemePrefs = get(),
        )
    }
}
