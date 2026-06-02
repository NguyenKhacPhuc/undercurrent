package dev.weft.undercurrent.core.domain

import dev.weft.harness.observability.TraceStore
import dev.weft.undercurrent.core.domain.AgentTrace
import dev.weft.undercurrent.core.domain.LlmCallTrace
import dev.weft.undercurrent.core.domain.ToolCallTrace
import dev.weft.undercurrent.core.domain.ToolStatus
import dev.weft.undercurrent.core.domain.TraceFeedback
import dev.weft.undercurrent.core.domain.TraceStatus
import dev.weft.undercurrent.core.domain.TraceStoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dev.weft.harness.observability.AgentTrace as WeftAgentTrace
import dev.weft.harness.observability.LlmCallTrace as WeftLlmCallTrace
import dev.weft.harness.observability.ToolCallTrace as WeftToolCallTrace
import dev.weft.harness.observability.ToolStatus as WeftToolStatus
import dev.weft.harness.observability.TraceFeedback as WeftTraceFeedback
import dev.weft.harness.observability.TraceStatus as WeftTraceStatus

/**
 * Android impl of [TraceStoreRepository] backed by Weft's [TraceStore].
 * Maps every Weft observability type to its commonMain mirror; feature
 * code never touches `dev.weft.harness.observability.*`.
 */
class WeftTraceStoreRepository(
    private val store: TraceStore,
) : TraceStoreRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val traces: StateFlow<List<AgentTrace>> = store.traces
        .map { list -> list.map { it.toCommon() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun setFeedback(traceId: String, feedback: TraceFeedback) {
        store.setFeedback(traceId, feedback.toWeft())
    }

    override suspend fun clear() {
        store.clear()
    }

    private fun WeftAgentTrace.toCommon(): AgentTrace = AgentTrace(
        id = id,
        conversationId = conversationId,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        userMessage = userMessage,
        finalAssistantMessage = finalAssistantMessage,
        llmCalls = llmCalls.map { it.toCommon() },
        toolCalls = toolCalls.map { it.toCommon() },
        status = status.toCommon(),
        errorMessage = errorMessage,
        feedback = feedback.toCommon(),
        parentTraceId = parentTraceId,
    )

    private fun WeftLlmCallTrace.toCommon(): LlmCallTrace = LlmCallTrace(
        id = id,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        model = model,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens,
        cacheReadTokens = cacheReadTokens,
        cacheWriteTokens = cacheWriteTokens,
    )

    private fun WeftToolCallTrace.toCommon(): ToolCallTrace = ToolCallTrace(
        id = id,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        toolName = toolName,
        argsPreview = argsPreview,
        resultPreview = resultPreview,
        status = status.toCommon(),
        errorMessage = errorMessage,
    )

    private fun WeftTraceStatus.toCommon(): TraceStatus = when (this) {
        WeftTraceStatus.RUNNING -> TraceStatus.RUNNING
        WeftTraceStatus.COMPLETED -> TraceStatus.COMPLETED
        WeftTraceStatus.FAILED -> TraceStatus.FAILED
    }

    private fun WeftToolStatus.toCommon(): ToolStatus = when (this) {
        WeftToolStatus.RUNNING -> ToolStatus.RUNNING
        WeftToolStatus.COMPLETED -> ToolStatus.COMPLETED
        WeftToolStatus.FAILED -> ToolStatus.FAILED
    }

    private fun WeftTraceFeedback.toCommon(): TraceFeedback = when (this) {
        WeftTraceFeedback.NONE -> TraceFeedback.NONE
        WeftTraceFeedback.THUMBS_UP -> TraceFeedback.THUMBS_UP
        WeftTraceFeedback.THUMBS_DOWN -> TraceFeedback.THUMBS_DOWN
    }

    private fun TraceFeedback.toWeft(): WeftTraceFeedback = when (this) {
        TraceFeedback.NONE -> WeftTraceFeedback.NONE
        TraceFeedback.THUMBS_UP -> WeftTraceFeedback.THUMBS_UP
        TraceFeedback.THUMBS_DOWN -> WeftTraceFeedback.THUMBS_DOWN
    }
}
