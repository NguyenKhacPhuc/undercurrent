package dev.weft.undercurrent.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub. The trace viewer is Android-only for v1 — iOS doesn't run
 * the Weft agent loop yet, so there are never any traces to show.
 */
class StubTraceStoreRepository : TraceStoreRepository {
    override val traces: StateFlow<List<AgentTrace>> =
        MutableStateFlow<List<AgentTrace>>(emptyList()).asStateFlow()

    override suspend fun setFeedback(traceId: String, feedback: TraceFeedback) = Unit

    override suspend fun clear() = Unit
}
