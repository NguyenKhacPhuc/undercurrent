package dev.weft.undercurrent.feature.traces

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.launch

data class TracesState(val traces: List<AgentTrace> = emptyList())

sealed interface TracesIntent {
    data class SetFeedback(val traceId: String, val feedback: TraceFeedback) : TracesIntent
    data object ClearAll : TracesIntent
}

sealed interface TracesEffect

class TracesViewModel(
    private val store: TraceStoreGateway,
) : MviViewModel<TracesState, TracesIntent, TracesEffect>(
    initialState = TracesState(traces = store.traces.value),
) {
    init {
        viewModelScope.launch {
            store.traces.collect { ts -> update { it.copy(traces = ts) } }
        }
    }

    override fun dispatch(intent: TracesIntent) {
        when (intent) {
            is TracesIntent.SetFeedback -> viewModelScope.launch {
                store.setFeedback(intent.traceId, intent.feedback)
            }
            TracesIntent.ClearAll -> viewModelScope.launch { store.clear() }
        }
    }
}
