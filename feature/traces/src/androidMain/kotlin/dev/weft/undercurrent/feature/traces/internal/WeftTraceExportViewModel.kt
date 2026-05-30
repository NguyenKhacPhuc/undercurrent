package dev.weft.undercurrent.feature.traces.internal

import dev.weft.android.WeftRuntime
import dev.weft.contracts.FileSaveSpec
import dev.weft.contracts.ShareContent
import dev.weft.contracts.ShareTarget
import dev.weft.harness.observability.AgentTrace
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.feature.traces.TraceExportViewModel
import dev.weft.undercurrent.feature.traces.TraceIntent
import dev.weft.undercurrent.shared.mvi.MviContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Trace-export feature ViewModel — Android impl. Look up the trace
 * by id in the runtime's store, serialize, redact, write through OS
 * bridges, hand to the system share sheet. Evicted traces / save /
 * share failures surface as snackbar [AppEffect.Error]s on the root
 * [AppState].
 *
 * Class shape: not a full [dev.weft.undercurrent.shared.mvi.MviViewModel]
 * because it has no state of its own — it reads from the substrate
 * trace store and writes one-shot error effects to the root.
 */
public class WeftTraceExportViewModel(
    private val context: MviContext<AppState, AppEffect>,
    private val runtime: WeftRuntime,
) : TraceExportViewModel {
    override fun dispatch(intent: TraceIntent) {
        when (intent) {
            is TraceIntent.ExportTrace -> context.scope.launch {
                handleExportTrace(intent.traceId)
            }
        }
    }

    private suspend fun handleExportTrace(traceId: String) {
        val trace: AgentTrace? =
            runtime.traceStore.traces.value.firstOrNull { it.id == traceId }
        if (trace == null) {
            context.emit(
                AppEffect.Error("Trace export failed: $traceId not found (evicted?)."),
            )
            return
        }
        try {
            val rawJson = TRACE_JSON.encodeToString(AgentTrace.serializer(), trace)
            val json = runtime.redactor.redact(rawJson)
            val ref = withContext(Dispatchers.IO) {
                runtime.os.files.save(
                    FileSaveSpec(
                        name = "trace-${trace.id}.json",
                        text = json,
                        mimeType = "application/json",
                    ),
                )
            }
            runtime.os.sharing.share(
                ShareContent(fileUri = ref.uri),
                ShareTarget.SystemSheet,
            )
        } catch (t: Throwable) {
            context.emit(
                AppEffect.Error(
                    "Trace export failed: ${t.message ?: t::class.simpleName.orEmpty()}",
                ),
            )
        }
    }

    private companion object {
        val TRACE_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
