package dev.weft.undercurrent.feature.theme

import org.koin.dsl.module

val themeModule = module {
    single { ThemeViewModel(repo = get()) }
}
