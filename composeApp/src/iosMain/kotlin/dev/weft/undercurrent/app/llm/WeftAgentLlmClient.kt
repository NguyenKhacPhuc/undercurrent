package dev.weft.undercurrent.app.llm

import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.streaming.StreamChunk
import dev.weft.undercurrent.app.IosWeftAgentFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

    override fun send(history: List<LlmMessage>, systemPrompt: String): Flow<LlmChunk> = flow {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(LlmChunk.Error("No Anthropic API key configured"))
            emit(LlmChunk.Done)
            return@flow
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

        // The agent rebuilds its prompt from its own ConversationStore.
        // The new user turn is the last user-role entry in `history` —
        // anything older is the agent's own ground truth at this point.
        val userText = history.lastOrNull { it.role == LlmMessage.ROLE_USER }?.content
        if (userText.isNullOrBlank()) {
            emit(LlmChunk.Error("No user message to send"))
            emit(LlmChunk.Done)
            return@flow
        }

        agent!!.sendStreaming(userText).collect { chunk ->
            when (chunk) {
                is StreamChunk.TextDelta -> emit(LlmChunk.TextDelta(chunk.text))
                is StreamChunk.Failed -> emit(LlmChunk.Error(chunk.message))
                is StreamChunk.Done -> emit(LlmChunk.Done)
                // Tool events ignored until the substrate-on-iOS path
                // registers actual tools. They emit through `events`
                // (SharedFlow) for surfaces that want to render them;
                // chat UI doesn't need them for plain-text turns.
                is StreamChunk.ToolStarting -> Unit
                is StreamChunk.ToolCompleted -> Unit
                is StreamChunk.ToolFailed -> Unit
            }
        }
    }
}
