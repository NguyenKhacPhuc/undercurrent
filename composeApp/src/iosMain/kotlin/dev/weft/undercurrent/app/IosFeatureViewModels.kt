package dev.weft.undercurrent.app

import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import dev.weft.undercurrent.feature.settings.providers.ProviderIntent
import dev.weft.undercurrent.feature.settings.providers.ProviderState
import dev.weft.undercurrent.feature.settings.providers.ProviderStateStore
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import dev.weft.undercurrent.feature.traces.TraceExportViewModel
import dev.weft.undercurrent.feature.traces.TraceIntent
import kotlinx.coroutines.flow.StateFlow

internal class IosProviderViewModel(
    private val app: IosAppViewModel,
    private val store: ProviderStateStore,
) : ProviderViewModel {
    override val state: StateFlow<ProviderState> = store.state

    override fun dispatch(intent: ProviderIntent) {
        when (intent) {
            is ProviderIntent.ValidateAndSaveProviderKey ->
                store.validateAndSave(intent.provider, intent.apiKey) {
                    app.dispatchProvider(
                        ProviderIntent.SaveProviderKey(intent.provider, intent.apiKey),
                    )
                }
            is ProviderIntent.ClearKeyValidation -> store.clearValidation()
            is ProviderIntent.RemoveProviderKey -> {
                app.dispatchProvider(intent)
                store.markKeyRemoved(intent.provider)
            }
            else -> app.dispatchProvider(intent)
        }
    }
}

internal class NoOpCreatorViewModel : CreatorViewModel {
    override fun dispatch(intent: CreatorIntent) = Unit
}

internal class NoOpTraceExportViewModel : TraceExportViewModel {
    override fun dispatch(intent: TraceIntent) = Unit
}
