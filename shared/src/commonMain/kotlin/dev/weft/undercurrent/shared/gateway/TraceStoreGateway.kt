package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Read + feedback surface for the agent-trace viewer. Backed by Weft's
 * `TraceStore` on Android; iOS stub holds an empty list.
 *
 * Writes (recordLlmStart/Complete/...) happen inside the agent loop and
 * stay inside `:data:weft` — feature code only reads and rates traces.
 */
public interface TraceStoreGateway {

    /** Snapshot of all currently-known traces, newest first. */
    public val traces: StateFlow<List<AgentTrace>>

    /** Set or clear feedback on a trace. */
    public suspend fun setFeedback(traceId: String, feedback: TraceFeedback)

    /** Wipe all traces. */
    public suspend fun clear()
}

/** Mirror of `dev.weft.harness.observability.AgentTrace`. */
@Serializable
public data class AgentTrace(
    val id: String,
    val conversationId: String,
    val startEpochMs: Long,
    val endEpochMs: Long? = null,
    val userMessage: String,
    val finalAssistantMessage: String? = null,
    val llmCalls: List<LlmCallTrace> = emptyList(),
    val toolCalls: List<ToolCallTrace> = emptyList(),
    val status: TraceStatus = TraceStatus.RUNNING,
    val errorMessage: String? = null,
    val feedback: TraceFeedback = TraceFeedback.NONE,
    val parentTraceId: String? = null,
) {
    val durationMs: Long? get() = endEpochMs?.let { it - startEpochMs }
    val totalInputTokens: Int get() = llmCalls.sumOf { it.inputTokens ?: 0 }
    val totalOutputTokens: Int get() = llmCalls.sumOf { it.outputTokens ?: 0 }
    val totalTokens: Int get() = llmCalls.sumOf { it.totalTokens ?: 0 }
}

@Serializable
public data class LlmCallTrace(
    val id: String,
    val startEpochMs: Long,
    val endEpochMs: Long? = null,
    val model: String,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val totalTokens: Int? = null,
    val cacheReadTokens: Int? = null,
    val cacheWriteTokens: Int? = null,
) {
    val durationMs: Long? get() = endEpochMs?.let { it - startEpochMs }
}

@Serializable
public data class ToolCallTrace(
    val id: String,
    val startEpochMs: Long,
    val endEpochMs: Long? = null,
    val toolName: String,
    val argsPreview: String,
    val resultPreview: String? = null,
    val status: ToolStatus = ToolStatus.RUNNING,
    val errorMessage: String? = null,
) {
    val durationMs: Long? get() = endEpochMs?.let { it - startEpochMs }
}

@Serializable
public enum class TraceStatus { RUNNING, COMPLETED, FAILED }

@Serializable
public enum class ToolStatus { RUNNING, COMPLETED, FAILED }

@Serializable
public enum class TraceFeedback { NONE, THUMBS_UP, THUMBS_DOWN }
