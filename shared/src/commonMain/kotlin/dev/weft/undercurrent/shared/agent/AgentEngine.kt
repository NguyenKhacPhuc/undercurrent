package dev.weft.undercurrent.shared.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * KMP-shared contract that feature modules consume to talk to the
 * agent. Decouples `commonMain` from Weft, which is Android-only.
 *
 * Implementations:
 *   - **`:data:weft` (androidMain)**: wraps `dev.weft.harness.agents.WeftAgent`
 *     + `dev.weft.android.WeftRuntime`. Real streaming, real tools.
 *   - **iosMain stub**: throws `NotImplementedError("Agent not supported on iOS yet")`
 *     for v1. Later iterations can wire a different LLM client or
 *     ship Weft to KMP.
 *
 * Feature modules type their dependencies as [AgentEngine] (not
 * `WeftAgent`); Koin DI injects the platform-appropriate
 * implementation at runtime.
 */
public interface AgentEngine {

    /** Observable runtime state for chat / conversations UI. */
    public val state: StateFlow<AgentState>

    /**
     * Streaming send — emits [ChatChunk] events as the LLM responds.
     * The flow completes with [ChatChunk.Done] after the assistant
     * turn finishes (text + any tool round-trips).
     *
     * [modelTier] is an optional per-call model-tier override. Pass
     * null for the default tier (driven by the agent's `WeftStrategy`).
     */
    public fun sendStreaming(text: String, modelTier: String? = null): Flow<ChatChunk>

    /**
     * Non-streaming send — suspends until the full assistant reply
     * is available and returns it. Use when streaming UX isn't worth
     * the complexity (one-shot prompts, agent-to-agent handoffs).
     */
    public suspend fun send(text: String, modelTier: String? = null): String

    /**
     * Start a fresh conversation. Clears in-memory history; the
     * conversation store persists the previous thread independently.
     */
    public suspend fun newChat()

    /**
     * Resume a previously-stored conversation by id. Loads history
     * from the conversation store into the active agent.
     */
    public suspend fun resume(conversationId: String)

    /**
     * Switch to a different named agent declaration. Multi-agent
     * hosts only — single-agent hosts can ignore. Throws if [name]
     * isn't in [AgentState.availableAgents].
     */
    public suspend fun selectAgent(name: String)

    /**
     * "Ask again" semantics — re-runs the last user message with a
     * fresh assistant turn. The previous assistant reply stays in
     * history. No-op if there's no last user message or a send is
     * already in flight.
     */
    public suspend fun regenerateLast()

    /**
     * Delete a conversation by id. If [id] matches the active
     * conversation the engine starts a fresh chat (same outcome as
     * [newChat]). Inactive threads are removed silently.
     */
    public suspend fun deleteConversation(id: String)
}
