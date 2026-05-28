package dev.weft.undercurrent.shared.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS placeholder implementation of [AgentEngine]. Returns
 * [AgentState.Empty] forever, throws on any send.
 *
 * v1 of the iOS variant ships *without* agent capabilities —
 * Undercurrent on iOS is a read-only / coming-soon experience until
 * one of:
 *
 *   a) Weft itself migrates to KMP (large, separate effort)
 *   b) The iOS variant ships a different LLM client (e.g. native
 *      Anthropic SDK in Swift, bridged via expect/actual)
 *   c) The iOS variant proxies to a server backend
 *
 * Until then, any code path that reaches into sending throws — the
 * UI layer is responsible for guarding `state.ready` before
 * inviting the user to type.
 *
 * Kept in iosMain (not iosArm64Main + iosSimulatorArm64Main) so both
 * iOS targets share it via the default hierarchy template.
 */
class StubAgentEngine : AgentEngine {

    private val _state: MutableStateFlow<AgentState> =
        MutableStateFlow(AgentState.Empty.copy(ready = false))

    override val state: StateFlow<AgentState> = _state.asStateFlow()

    override fun sendStreaming(text: String, modelTier: String?): Flow<ChatChunk> =
        flowOf(ChatChunk.ToolFailed("send", REASON), ChatChunk.Done)

    override suspend fun send(text: String, modelTier: String?): String =
        error(REASON)

    override suspend fun newChat(): Unit = Unit

    override suspend fun resume(conversationId: String): Unit = Unit

    override suspend fun selectAgent(name: String) {
        error("selectAgent not supported on iOS yet")
    }

    override suspend fun regenerateLast(): Unit = Unit

    override suspend fun deleteConversation(id: String): Unit = Unit

    private companion object {
        const val REASON: String = "Agent not supported on iOS yet — see :data:weft Android impl."
    }
}
