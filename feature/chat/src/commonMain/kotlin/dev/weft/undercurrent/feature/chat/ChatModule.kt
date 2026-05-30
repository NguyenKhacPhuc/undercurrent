package dev.weft.undercurrent.feature.chat

import org.koin.dsl.module

val chatModule = module {
    single {
        ChatViewModel(
            sendChat = get(),
            regenerateLast = get(),
            newChat = get(),
            selectConversation = get(),
            deleteConversation = get(),
            deleteCurrentConversation = get(),
            selectAgent = get(),
            resumeChat = get(),
            sendUiEventUseCase = get(),
            observeChatState = get(),
            loadMessages = get(),
        )
    }
}
