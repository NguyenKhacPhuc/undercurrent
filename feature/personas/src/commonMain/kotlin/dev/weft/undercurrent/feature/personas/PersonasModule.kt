package dev.weft.undercurrent.feature.personas

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val personasModule = module {
    viewModel { PersonasViewModel(repo = get()) }
}
