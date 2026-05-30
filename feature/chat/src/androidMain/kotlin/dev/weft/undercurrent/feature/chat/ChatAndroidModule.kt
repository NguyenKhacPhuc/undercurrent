package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.feature.chat.agent.AgentSlot
import dev.weft.undercurrent.feature.chat.agent.WeftAgentFactory
import dev.weft.undercurrent.feature.chat.internal.WeftChatRepository
import org.koin.dsl.module

val chatAndroidModule = module {
    single { AgentSlot() }
    single { WeftAgentFactory(runtime = get(), modelPrefsRepo = get()) }
    single<ChatRepository> {
        WeftChatRepository(
            runtime = get(),
            agentSlot = get(),
            agentFactory = get(),
            providerPrefsRepo = get(),
            navigationVm = get(),
        )
    }
}
