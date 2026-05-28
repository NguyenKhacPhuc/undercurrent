package dev.weft.undercurrent.feature.traces

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.launch

public data class TracesState(public val traces: List<AgentTrace> = emptyList())

public sealed interface TracesIntent {
    public data class SetFeedback(public val traceId: String, public val feedback: TraceFeedback) : TracesIntent
    public data object ClearAll : TracesIntent
}

public sealed interface TracesEffect

public class TracesStore(
    private val store: TraceStoreGateway,
) : Store<TracesState, TracesIntent, TracesEffect>(
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
