package dev.weft.undercurrent.feature.traces

/**
 * Trace-surface intents. Currently just export (the rest of the
 * traces feature reads via [TracesViewModel] directly without
 * dispatching at the root level).
 */
sealed interface TraceIntent {

    /**
     * Redact + save + share a trace JSON by id. The handler
     * re-resolves the full trace from the trace store at write time
     * so traces evicted between the user tap and the write are
     * surfaced as a user-visible error instead of silently exporting
     * an empty file.
     */
    data class ExportTrace(val traceId: String) : TraceIntent
}
