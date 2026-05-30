package dev.weft.undercurrent.data.weft

import dev.weft.harness.agents.AgentEffect
import dev.weft.harness.agents.AgentIntent
import dev.weft.harness.agents.TurnStatus
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.harness.behavior.Turn
import dev.weft.undercurrent.shared.agent.AgentEngine
import dev.weft.undercurrent.shared.agent.AgentState
import dev.weft.undercurrent.shared.agent.ChatChunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Android implementation of [AgentEngine] that wraps a [WeftAgent].
 * Lives in `:data:weft` (Android-only) because Weft itself is
 * Android-only.
 *
 * What this bridge does:
 *   - Translates Weft's reactive surface ([WeftAgent.state] +
 *     [WeftAgent.effects]) into our KMP-shared [ChatChunk] stream
 *     shape that feature modules consume.
 *   - Projects [WeftAgent.state] into the broader [AgentState]
 *     wrapper used by commonMain feature modules.
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

    // Long-lived scope for mirroring `agent.state` into our shared
    // AgentState flow. Cancelled implicitly when the process dies.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(
        AgentState(
            ready = true,
            conversationId = agent.state.value.conversationId.takeIf { it.isNotBlank() },
        ),
    )

    override val state: StateFlow<AgentState> = _state.asStateFlow()

    init {
        // Mirror the agent's conversation id into our shared state.
        scope.launch {
            agent.state
                .map { it.conversationId }
                .distinctUntilChanged()
                .collect { convId ->
                    _state.value = _state.value.copy(
                        conversationId = convId.takeIf { it.isNotBlank() },
                    )
                }
        }
    }

    override fun sendStreaming(text: String, modelTier: String?): Flow<ChatChunk> = channelFlow {
        // Project agent state + effects into ChatChunk emissions for
        // the duration of one turn. Same pattern as IosWeftAgentLlmClient
        // — see comments there for the prefix-tracking rationale.
        var emittedPrefix = ""
        val stateJob = launch {
            agent.state.collect { st ->
                val delta = st.pendingAssistantDelta
                if (delta.length > emittedPrefix.length && delta.startsWith(emittedPrefix)) {
                    trySend(ChatChunk.TextDelta(delta.substring(emittedPrefix.length)))
                    emittedPrefix = delta
                } else if (delta != emittedPrefix && delta.isNotEmpty()) {
                    trySend(ChatChunk.TextDelta(delta))
                    emittedPrefix = delta
                }
                when (st.turnStatus) {
                    TurnStatus.Idle -> {
                        if (st.lastError == null && emittedPrefix.isNotEmpty()) {
                            trySend(ChatChunk.Done)
                            close()
                        }
                    }
                    TurnStatus.Failed -> {
                        trySend(
                            ChatChunk.ToolFailed(
                                "send",
                                st.lastError?.message ?: "send failed",
                            ),
                        )
                        close()
                    }
                    else -> Unit
                }
            }
        }
        val effectsJob = launch {
            agent.effects.collect { ef ->
                when (ef) {
                    is AgentEffect.ToolStarting -> trySend(ChatChunk.ToolStarting(ef.toolName))
                    is AgentEffect.ToolCompleted -> trySend(ChatChunk.ToolCompleted(ef.toolName))
                    is AgentEffect.ToolFailed -> trySend(
                        ChatChunk.ToolFailed(ef.toolName, ef.message),
                    )
                    // Notify / QuotaBlocked / BreakerOpened — not
                    // ChatChunk-shaped; consumers wanting these
                    // subscribe directly to `agent.effects`.
                    is AgentEffect.Notify,
                    is AgentEffect.QuotaBlocked,
                    is AgentEffect.BreakerOpened -> Unit
                }
            }
        }

        agent.dispatch(AgentIntent.Send(text, tier = modelTier.toWeftTier()))

        awaitClose {
            stateJob.cancel()
            effectsJob.cancel()
        }
    }

    override suspend fun send(text: String, modelTier: String?): String {
        agent.dispatchAndAwait(
            AgentIntent.Send(text, tier = modelTier.toWeftTier(), streaming = false),
        )
        // After the turn completes, the new assistant reply is the
        // last entry in the agent's history.
        val lastAssistant = agent.state.value.history.lastOrNull { it is Turn.Assistant } as? Turn.Assistant
        return lastAssistant?.text.orEmpty()
    }

    override suspend fun newChat() {
        agent.dispatchAndAwait(AgentIntent.NewChat)
        _state.value = _state.value.copy(conversationId = null)
    }

    override suspend fun resume(conversationId: String) {
        agent.dispatchAndAwait(AgentIntent.Resume(conversationId))
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
        agent.dispatchAndAwait(AgentIntent.Regenerate(streaming = false))
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
