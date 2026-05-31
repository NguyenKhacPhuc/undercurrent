package dev.weft.undercurrent.feature.traces

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
public fun TracesRoute() {
    val nav: NavigationViewModel = koinInject()
    val export: TraceExportViewModel = koinInject()
    TraceViewerScreen(
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onExportTrace = { trace -> export.dispatch(TraceIntent.ExportTrace(trace.id)) },
    )
}
