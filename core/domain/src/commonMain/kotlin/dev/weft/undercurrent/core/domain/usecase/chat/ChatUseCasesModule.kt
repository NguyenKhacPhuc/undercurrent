package dev.weft.undercurrent.core.domain.usecase.chat

import org.koin.dsl.module

val chatUseCasesModule = module {
    factory { SelectConversationUseCase(repo = get()) }
    factory { DeleteCurrentConversationUseCase(repo = get()) }
    factory { SelectAgentUseCase(repo = get()) }
    factory { ObserveChatStateUseCase(repo = get()) }
}
