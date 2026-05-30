package dev.weft.undercurrent.feature.traces

import dev.weft.undercurrent.core.domain.AgentTrace
import dev.weft.undercurrent.core.domain.TraceFeedback
import dev.weft.undercurrent.core.domain.TraceStoreRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class TracesState(val traces: List<AgentTrace> = emptyList())

sealed interface TracesIntent {
    data class SetFeedback(val traceId: String, val feedback: TraceFeedback) : TracesIntent
    data object ClearAll : TracesIntent
}

sealed interface TracesEffect

class TracesViewModel(
    private val store: TraceStoreRepository,
) : MviViewModel<TracesState, TracesIntent, TracesEffect>(
    initialState = TracesState(traces = store.traces.value),
) {
    init {
        store.traces.collectInto { copy(traces = it) }
    }

    override fun dispatch(intent: TracesIntent) = launch {
        when (intent) {
            is TracesIntent.SetFeedback -> store.setFeedback(intent.traceId, intent.feedback)
            TracesIntent.ClearAll -> store.clear()
        }
    }
}
