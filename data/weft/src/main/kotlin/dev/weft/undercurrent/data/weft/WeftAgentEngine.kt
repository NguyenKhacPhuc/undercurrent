// WeftAgentEngine wraps WeftAgent in undercurrent's own KMP-shared
// AgentEngine interface so commonMain feature modules can consume the
// agent without binding to Weft's exact API. After Weft Phase 1 added
// state/dispatch/effects on WeftAgent itself, this wrapper duplicates
// most of what WeftAgent already exposes — a future refactor will
// decide whether AgentEngine still earns its keep or whether feature
// modules should consume WeftAgent's state directly.
//
// Until that refactor, this file uses the @Deprecated WeftAgent
// methods directly. Suppressed file-wide so review noise stays low.
@file:Suppress("DEPRECATION")

package dev.weft.undercurrent.data.weft

import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.streaming.StreamChunk
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.undercurrent.shared.agent.AgentEngine
import dev.weft.undercurrent.shared.agent.AgentState
import dev.weft.undercurrent.shared.agent.ChatChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Android implementation of [AgentEngine] that wraps a [WeftAgent].
 * Lives in `:data:weft` (Android-only) because Weft itself is
 * Android-only.
 *
 * What this bridge does:
 *   - Translates Weft's [StreamChunk] sealed class into our
 *     KMP-shared [ChatChunk] (1:1 mapping plus the `Failed` →
 *     `ToolFailed("send")` collapse).
 *   - Wraps [WeftAgent.currentConversationId] into the broader
 *     [AgentState] shape feature modules consume.
 *   - Maps the `modelTier` string from common code to Weft's
 *     [ModelTier] enum.
 *
 * What this bridge DOES NOT do:
 *   - Build the agent (that's `:androidApp`'s DI module's job —
 *     calls `WeftRuntime.buildAgent(provider)` and supplies the
 *     resulting [WeftAgent] here).
 *   - Provider switching, key vault writes, OAuth — those go
 *     through other bridge classes in this module.
 *
 * Feature modules in `commonMain` depend on the [AgentEngine]
 * interface only; Koin DI resolves to this implementation on
 * Android, [dev.weft.undercurrent.shared.agent.StubAgentEngine] on
 * iOS.
 */
class WeftAgentEngine(
    private val agent: WeftAgent,
) : AgentEngine {

    // Initial state — ready=true because by the time DI builds this,
    // the agent is constructed. Provider-switch / boot screens
    // construct a different (stubbed) engine until the real agent
    // is ready.
    private val _state = MutableStateFlow(
        AgentState(
            ready = true,
            conversationId = agent.currentConversationId.value
                .takeIf { it.isNotBlank() },
        )
    )

    override val state: StateFlow<AgentState> = _state.asStateFlow()

    override fun sendStreaming(text: String, modelTier: String?): Flow<ChatChunk> =
        agent.sendStreaming(userText = text, modelTier = modelTier.toWeftTier())
            .map { it.toCommonChunk() }

    override suspend fun send(text: String, modelTier: String?): String =
        agent.send(userText = text, modelTier = modelTier.toWeftTier())

    override suspend fun newChat() {
        agent.newChat()
        _state.value = _state.value.copy(conversationId = null)
    }

    override suspend fun resume(conversationId: String) {
        agent.resume(conversationId)
        _state.value = _state.value.copy(conversationId = conversationId)
    }

    override suspend fun selectAgent(name: String) {
        // WeftAgent itself is single-declaration. Multi-agent
        // switching happens at the WeftRuntime level (rebuild the
        // agent via runtime.buildAgent(name)). The DI layer that
        // owns the runtime is the right place — pass an
        // engine-rebuild lambda here when wiring this up.
        error("selectAgent must be handled at the DI layer — rebuild the WeftAgent and inject a fresh WeftAgentEngine.")
    }

    override suspend fun regenerateLast() {
        agent.regenerate()
    }

    override suspend fun deleteConversation(id: String) {
        // WeftAgent doesn't expose a delete-conversation method
        // directly — it goes through the conversation store. The
        // DI layer that owns the ConversationStore handles this;
        // we forward via state changes only.
        if (_state.value.conversationId == id) {
            newChat()
        }
    }

    // ---- chunk translation -----------------------------------------------

    private fun StreamChunk.toCommonChunk(): ChatChunk = when (this) {
        is StreamChunk.TextDelta -> ChatChunk.TextDelta(text)
        is StreamChunk.ToolStarting -> ChatChunk.ToolStarting(toolName)
        is StreamChunk.ToolCompleted -> ChatChunk.ToolCompleted(toolName)
        is StreamChunk.ToolFailed -> ChatChunk.ToolFailed(toolName, message)
        is StreamChunk.Done -> ChatChunk.Done
        is StreamChunk.Failed -> ChatChunk.ToolFailed("send", message)
    }

    // ---- model-tier translation ------------------------------------------

    /**
     * The KMP layer uses strings for model tiers so common code
     * doesn't pull in Weft's enum. Map the string back to
     * [ModelTier] here. Unknown / null falls through to "let the
     * router decide".
     */
    private fun String?.toWeftTier(): ModelTier? = when (this?.uppercase()) {
        "CHEAP" -> ModelTier.Cheap
        "STANDARD" -> ModelTier.Standard
        "VISION" -> ModelTier.Vision
        "HEAVY" -> ModelTier.Heavy
        null, "" -> null
        else -> null
    }
}
