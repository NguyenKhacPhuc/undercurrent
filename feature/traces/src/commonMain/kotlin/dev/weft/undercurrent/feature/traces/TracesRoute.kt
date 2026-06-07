package dev.weft.undercurrent.feature.traces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun TracesRoute() {
    val nav: NavigationViewModel = koinInject()
    val export: TraceExportViewModel = koinInject()
    val vm: TracesViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    TraceViewerScreen(
        state = state,
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onExportTrace = { trace -> export.dispatch(TraceIntent.ExportTrace(trace.id)) },
        onSetFeedback = { traceId, fb -> vm.dispatch(TracesIntent.SetFeedback(traceId, fb)) },
        onClearAll = { vm.dispatch(TracesIntent.ClearAll) },
    )
}
