package dev.weft.undercurrent.feature.memories

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val memoriesModule = module {
    viewModel { MemoriesViewModel(store = get()) }
}
