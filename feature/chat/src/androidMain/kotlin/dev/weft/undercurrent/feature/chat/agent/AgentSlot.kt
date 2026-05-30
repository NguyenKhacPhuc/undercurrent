package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.agents.WeftAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentSlot {
    private val _agent = MutableStateFlow<WeftAgent?>(null)

    val flow: StateFlow<WeftAgent?> = _agent.asStateFlow()

    var agent: WeftAgent?
        get() = _agent.value
        set(value) {
            _agent.value = value
        }
}
