package dev.weft.undercurrent.core.domain.usecase.chat

import org.koin.dsl.module

val chatUseCasesModule = module {
    factory { SendChatUseCase(repo = get()) }
    factory { RegenerateLastUseCase(repo = get()) }
    factory { NewChatUseCase(repo = get()) }
    factory { SelectConversationUseCase(repo = get()) }
    factory { DeleteConversationUseCase(repo = get()) }
    factory { DeleteCurrentConversationUseCase(repo = get()) }
    factory { SelectAgentUseCase(repo = get()) }
    factory { LoadMessagesUseCase(repo = get()) }
    factory { ResumeChatUseCase(repo = get()) }
    factory { SendUiEventUseCase(repo = get()) }
    factory { ObserveChatStateUseCase(repo = get()) }
}
