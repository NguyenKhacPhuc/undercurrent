package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.feature.chat.agent.AgentSlot
import dev.weft.undercurrent.feature.chat.agent.WeftAgentFactory
import dev.weft.undercurrent.feature.chat.internal.WeftChatRepository
import org.koin.dsl.module

/**
 * Shared agent-host bindings — the agent slot, the agent factory, and the
 * streaming [WeftChatRepository]. Lives in commonMain now that the agent
 * host is shared (ios-agent-bringup 03/05), so both platforms use it.
 */
val chatHostModule = module {
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
