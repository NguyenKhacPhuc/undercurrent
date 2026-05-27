package dev.weft.undercurrent.shared.agent

/**
 * Observable state of the active agent. Mirrors what feature modules
 * (chat surface, conversations drawer) need to render — *not* the
 * full Weft runtime state.
 *
 * Held by [AgentEngine.state] as a `StateFlow<AgentState>`. The
 * Android implementation in `:data:weft` derives it from WeftAgent's
 * own state flows; iOS stubs return a fixed `AgentState.Empty`.
 */
public data class AgentState(
    /**
     * True when the engine is ready to accept `send` / `sendStreaming`
     * calls. False during boot, key-paste flow, or after a provider
     * switch while the new agent is being built.
     */
    public val ready: Boolean = false,
    /**
     * Current conversation id, or null when no conversation has been
     * started yet (boot path).
     */
    public val conversationId: String? = null,
    /**
     * Name of the currently-active agent declaration (for multi-agent
     * hosts). Single-agent hosts always see "default".
     */
    public val activeAgent: String = DEFAULT_AGENT_NAME,
    /** Agent names the user can switch to. */
    public val availableAgents: List<String> = listOf(DEFAULT_AGENT_NAME),
    /**
     * True while a `send` / `sendStreaming` call is in flight. The
     * UI uses this to disable the send button + show a "sending…"
     * affordance.
     */
    public val sending: Boolean = false,
) {
    public companion object {
        public const val DEFAULT_AGENT_NAME: String = "default"
        public val Empty: AgentState = AgentState()
    }
}
