package dev.weft.undercurrent.core.navigation

import org.koin.dsl.module

val navigationModule = module {
    single { NavigationViewModel() }
    single { NavigationChannel() }
}
