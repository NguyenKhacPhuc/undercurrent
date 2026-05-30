package dev.weft.undercurrent.feature.traces

import dev.weft.undercurrent.feature.traces.internal.WeftTraceExportViewModel
import org.koin.dsl.module

val traceExportAndroidModule = module {
    single<TraceExportViewModel> {
        WeftTraceExportViewModel(
            context = get(),
            runtime = get(),
        )
    }
}
