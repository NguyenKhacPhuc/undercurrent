package dev.weft.undercurrent.feature.miniapps

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val miniAppsModule = module {
    viewModel { MiniAppsViewModel(repo = get()) }
}
