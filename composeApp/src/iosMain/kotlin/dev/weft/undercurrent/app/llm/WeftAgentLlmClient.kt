package dev.weft.undercurrent.app.llm

import dev.weft.harness.agents.AgentIntent
import dev.weft.harness.agents.TurnStatus
import dev.weft.harness.agents.WeftAgent
import dev.weft.undercurrent.app.IosWeftAgentFactory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Thin [LlmClient] adapter that routes Anthropic chat through the
 * substrate's [WeftAgent] instead of the parallel hand-rolled SSE
 * client in [AnthropicLlmClient].
 *
 * Drop-in compatible with the existing IosAppStore wiring — swap
 * `AnthropicLlmClient(...)` for `WeftAgentLlmClient(getApiKey)` in the
 * `clients` map and chat for the Anthropic provider goes through the
 * substrate agent loop (retry, circuit breaker, observability, model
 * routing, cache binding) instead of the bare Ktor SSE.
 *
 * Three caveats to know before flipping it on:
 *
 *  1. **Conversation context.** The agent owns its own conversation
 *     history via an in-memory `ConversationStore`. As long as the
 *     same instance is reused across turns of the same conversation,
 *     multi-turn context works. But this client is constructed once
 *     and used for every conversation in the app — there's no
 *     per-thread `WeftAgent`. Without bridging to undercurrent's
 *     SQLDelight-backed conversation table, history won't track
 *     `IosAppStore`'s active thread. v1 is "single rolling history."
 *
 *  2. **Tools.** Empty tool registry by design — see
 *     [IosWeftAgentFactory]. Tool support is a follow-up.
 *
 *  3. **Model.** Pinned to Sonnet 4.5 via `StaticModelRouter`. Per-tier
 *     routing (cheap/standard/heavy) lights up once we have multiple
 *     models wired into the iOS pool.
 *
 * The `history` argument is intentionally ignored — the agent rebuilds
 * its prompt from its own state. The last user-role entry is what the
 * agent will see for THIS turn.
 */
internal class WeftAgentLlmClient(
    override val displayName: String = "Claude (via substrate)",
    override val modelId: String = "claude-sonnet-4-5",
    private val getApiKey: suspend () -> String?,
    private val systemPrompt: String = IOS_SYSTEM_PROMPT,
) : LlmClient {

    private var agent: WeftAgent? = null
    private var cachedKey: String? = null

    override fun send(history: List<LlmMessage>, systemPrompt: String): Flow<LlmChunk> = channelFlow {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            trySend(LlmChunk.Error("No Anthropic API key configured"))
            trySend(LlmChunk.Done)
            return@channelFlow
        }

        // Rebuild the agent when the API key rotates. Otherwise reuse
        // it so multi-turn conversation context stays inside the
        // substrate's in-memory ConversationStore.
        if (agent == null || cachedKey != apiKey) {
            agent = IosWeftAgentFactory.buildAnthropic(
                apiKey = apiKey,
                systemPrompt = systemPrompt.ifBlank { this@WeftAgentLlmClient.systemPrompt },
            )
            cachedKey = apiKey
        }
        val a = agent!!

        // The agent rebuilds its prompt from its own ConversationStore.
        // The new user turn is the last user-role entry in `history` —
        // anything older is the agent's own ground truth at this point.
        val userText = history.lastOrNull { it.role == LlmMessage.ROLE_USER }?.content
        if (userText.isNullOrBlank()) {
            trySend(LlmChunk.Error("No user message to send"))
            trySend(LlmChunk.Done)
            return@channelFlow
        }

        // Project the agent's reactive state into a one-shot LlmChunk
        // Flow. Each TextDelta is the substring of pendingAssistantDelta
        // that grew since the last emission. We close the channel when
        // the turn returns to Idle (success) or transitions to Failed.
        var emittedPrefix = ""
        val stateJob = launch {
            a.state.collect { st ->
                val delta = st.pendingAssistantDelta
                if (delta.length > emittedPrefix.length && delta.startsWith(emittedPrefix)) {
                    trySend(LlmChunk.TextDelta(delta.substring(emittedPrefix.length)))
                    emittedPrefix = delta
                } else if (delta != emittedPrefix && delta.isNotEmpty()) {
                    // Defensive — handle the rare case where the delta
                    // doesn't share our captured prefix (e.g. a brand-
                    // new turn started after we missed the reset).
                    trySend(LlmChunk.TextDelta(delta))
                    emittedPrefix = delta
                }
                when (st.turnStatus) {
                    TurnStatus.Idle -> {
                        if (st.lastError == null && emittedPrefix.isNotEmpty()) {
                            trySend(LlmChunk.Done)
                            close()
                        }
                    }
                    TurnStatus.Failed -> {
                        trySend(LlmChunk.Error(st.lastError?.message ?: "send failed"))
                        close()
                    }
                    else -> Unit
                }
            }
        }

        a.dispatch(AgentIntent.Send(userText))

        awaitClose {
            stateJob.cancel()
        }
    }
}
