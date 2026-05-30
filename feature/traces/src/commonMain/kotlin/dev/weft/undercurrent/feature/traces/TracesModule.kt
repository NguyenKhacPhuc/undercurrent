package dev.weft.undercurrent.feature.traces

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tracesModule = module {
    viewModel { TracesViewModel(store = get()) }
}
