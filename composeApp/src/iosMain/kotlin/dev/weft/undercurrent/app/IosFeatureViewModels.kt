package dev.weft.undercurrent.app

import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.providers.ProviderIntent
import dev.weft.undercurrent.feature.providers.ProviderViewModel
import dev.weft.undercurrent.feature.traces.TraceExportViewModel
import dev.weft.undercurrent.feature.traces.TraceIntent

internal class IosProviderViewModel(
    private val app: IosAppViewModel,
) : ProviderViewModel {
    override fun dispatch(intent: ProviderIntent) = app.dispatchProvider(intent)
}

internal class IosMiniAppViewModel(
    private val app: IosAppViewModel,
) : MiniAppViewModel {
    override fun dispatch(intent: MiniAppIntent) = app.dispatchMiniApp(intent)
}

internal class NoOpCreatorViewModel : CreatorViewModel {
    override fun dispatch(intent: CreatorIntent) = Unit
}

internal class NoOpTraceExportViewModel : TraceExportViewModel {
    override fun dispatch(intent: TraceIntent) = Unit
}
