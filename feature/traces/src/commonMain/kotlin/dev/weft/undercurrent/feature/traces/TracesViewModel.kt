package dev.weft.undercurrent.feature.traces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [TraceViewerScreen]. Owns the gateway
 * dependency + the two suspend mutations (set-feedback, clear-all) so
 * the screen doesn't need `rememberCoroutineScope` for them.
 *
 * Trace export still goes through the root app store — exporting
 * depends on the runtime's redactor + OS file/sharing layer that the
 * root store already owns.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/traces/TracesViewModel.kt`. Now consumes
 * [TraceStoreGateway] (was Weft's `TraceStore` directly).
 */
public class TracesViewModel(
    private val store: TraceStoreGateway,
) : ViewModel() {

    public val traces: StateFlow<List<AgentTrace>> = store.traces

    public fun setFeedback(traceId: String, feedback: TraceFeedback) {
        viewModelScope.launch { store.setFeedback(traceId, feedback) }
    }

    public fun clearAll() {
        viewModelScope.launch { store.clear() }
    }
}
