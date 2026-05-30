package dev.weft.undercurrent.feature.conversations

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    viewModel { ConversationsViewModel(store = get()) }
}
