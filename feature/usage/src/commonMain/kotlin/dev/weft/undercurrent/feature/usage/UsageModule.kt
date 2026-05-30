package dev.weft.undercurrent.feature.usage

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val usageModule = module {
    viewModel { UsageViewModel(gateway = get()) }
}
