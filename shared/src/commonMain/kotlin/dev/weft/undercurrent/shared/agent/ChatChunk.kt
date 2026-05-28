package dev.weft.undercurrent.shared.agent

/**
 * KMP-friendly mirror of Weft's `StreamChunk`. Feature modules in
 * `commonMain` can subscribe to a `Flow<ChatChunk>` without pulling
 * Weft into their compile classpath (which would break iOS).
 *
 * The Android implementation of [AgentEngine] (in `:data:weft`)
 * translates Weft's `StreamChunk` into this type on the way out.
 * iOS implementations either route to a different LLM client or
 * stub the flow entirely.
 */
sealed interface ChatChunk {
    /** Streaming assistant text — append to the current message. */
    data class TextDelta(val text: String) : ChatChunk

    /**
     * The agent started invoking a tool. Use to render an inline
     * "Calling X…" affordance. [toolName] is the substrate-level
     * tool name (e.g. "calendar_read", "ui_render").
     */
    data class ToolStarting(val toolName: String) : ChatChunk

    /**
     * Tool finished. [resultSummary] is a short human-readable
     * summary the agent provided; full results go through the
     * substrate's trace store, not this channel.
     */
    data class ToolCompleted(
        val toolName: String,
        val resultSummary: String? = null,
    ) : ChatChunk

    /**
     * Tool execution failed. Surfaces to the LLM as a tool-failed
     * message; the UI can render an inline error chip.
     */
    data class ToolFailed(
        val toolName: String,
        val message: String,
    ) : ChatChunk

    /**
     * Turn finished. Emitted exactly once at the end of each
     * `sendStreaming` flow. After this, the flow completes.
     */
    data object Done : ChatChunk
}
