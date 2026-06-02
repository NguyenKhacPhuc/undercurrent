package dev.weft.undercurrent.core.navigation

import org.koin.dsl.module

val navigationModule = module {
    single { Navigator() }
    single { NavigationChannel() }
}
