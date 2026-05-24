package dev.weft.undercurrent.features.traces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.android.WeftRuntime
import dev.weft.harness.observability.AgentTrace
import dev.weft.harness.observability.TraceFeedback
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [dev.weft.undercurrent.features.traces.TraceViewerScreen].
 * Owns the trace-store dependency + the three suspend mutations
 * (set-feedback, clear-all) so the screen doesn't need
 * `rememberCoroutineScope` for them.
 *
 * Trace export ([dev.weft.undercurrent.core.AppIntent.ExportTrace]) still goes
 * through AppStore — exporting depends on the runtime's redactor + OS
 * file/sharing layer that the root store already owns, and the resulting
 * [dev.weft.undercurrent.core.AppEffect.Error] flows through the central
 * effects channel.
 */
internal class TracesViewModel(
    runtime: WeftRuntime,
) : ViewModel() {
    private val store = runtime.traceStore

    val traces: StateFlow<List<AgentTrace>> = store.traces

    fun setFeedback(traceId: String, feedback: TraceFeedback) {
        viewModelScope.launch { store.setFeedback(traceId, feedback) }
    }

    fun clearAll() {
        viewModelScope.launch { store.clear() }
    }
}
