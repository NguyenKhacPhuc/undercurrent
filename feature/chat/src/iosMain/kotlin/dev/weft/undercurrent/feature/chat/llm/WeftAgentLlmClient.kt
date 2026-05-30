package dev.weft.undercurrent.feature.chat.llm

import dev.weft.harness.agents.AgentIntent
import dev.weft.harness.agents.TurnStatus
import dev.weft.harness.agents.WeftAgent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Thin [LlmClient] adapter that routes Anthropic chat through the
 * substrate's [WeftAgent] instead of the parallel hand-rolled SSE
 * client in [AnthropicLlmClient].
 *
 * Caveats:
 *  1. Single rolling history — the agent owns its own ConversationStore
 *     (in-memory). Multi-thread history requires bridging the iOS
 *     SQLDelight conversations table to the substrate's
 *     [dev.weft.harness.conversation.ConversationStore].
 *  2. Empty tool registry (iOS OsCapabilities still scaffolding).
 *  3. Pinned to Sonnet 4.5.
 *
 * The `history` argument is ignored — the agent rebuilds its prompt
 * from its own state. The last user-role entry is what the agent
 * sees for THIS turn.
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

        if (agent == null || cachedKey != apiKey) {
            agent = IosWeftAgentFactory.buildAnthropic(
                apiKey = apiKey,
                systemPrompt = systemPrompt.ifBlank { this@WeftAgentLlmClient.systemPrompt },
            )
            cachedKey = apiKey
        }
        val a = agent!!

        val userText = history.lastOrNull { it.role == LlmMessage.ROLE_USER }?.content
        if (userText.isNullOrBlank()) {
            trySend(LlmChunk.Error("No user message to send"))
            trySend(LlmChunk.Done)
            return@channelFlow
        }

        var emittedPrefix = ""
        val stateJob = launch {
            a.state.collect { st ->
                val delta = st.pendingAssistantDelta
                if (delta.length > emittedPrefix.length && delta.startsWith(emittedPrefix)) {
                    trySend(LlmChunk.TextDelta(delta.substring(emittedPrefix.length)))
                    emittedPrefix = delta
                } else if (delta != emittedPrefix && delta.isNotEmpty()) {
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
