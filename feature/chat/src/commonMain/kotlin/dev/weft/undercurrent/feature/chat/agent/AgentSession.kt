package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.agents.WeftAgent
import dev.weft.undercurrent.core.model.AgentSummary
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.shared.mvi.MviContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AgentSession(
    private val context: MviContext<AppState, AppEffect>,
    private val navigationVm: NavigationViewModel,
    private val agentSlot: AgentSlot,
) {

    val currentAgentFlow: StateFlow<WeftAgent?> get() = agentSlot.flow

    val currentAgent: WeftAgent? get() = agentSlot.agent

    fun setRootScreen(screen: Screen) {
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(screen))
    }

    fun forgetAgent() {
        agentSlot.agent = null
    }

    fun setAgent(
        a: WeftAgent,
        screen: Screen = context.current.screen,
        availableAgents: List<AgentSummary> = context.current.availableAgents,
        activeAgentName: String = context.current.activeAgentName,
    ) {
        agentSlot.agent = a
        setRootScreen(screen)
        context.update {
            it.copy(
                agentReady = true,
                currentConversationId = a.state.value.conversationId,
                availableAgents = availableAgents,
                activeAgentName = activeAgentName,
            )
        }
        context.scope.launch {
            a.state
                .map { it.conversationId }
                .distinctUntilChanged()
                .collect { convId ->
                    if (currentAgent === a) {
                        context.update { current ->
                            if (current.currentConversationId == convId) current
                            else current.copy(currentConversationId = convId)
                        }
                    }
                }
        }
    }
}
