package dev.weft.undercurrent.feature.chat

import org.koin.dsl.module

val chatModule = module {
    single {
        ChatViewModel(
            repo = get(),
            selectConversation = get(),
            deleteCurrentConversation = get(),
            selectAgent = get(),
            observeChatState = get(),
        )
    }
}
