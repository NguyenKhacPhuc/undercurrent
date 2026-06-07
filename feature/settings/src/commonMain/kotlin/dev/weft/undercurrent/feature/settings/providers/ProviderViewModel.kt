package dev.weft.undercurrent.feature.settings.providers

import kotlinx.coroutines.flow.StateFlow

interface ProviderViewModel {
    val state: StateFlow<ProviderState>
    fun dispatch(intent: ProviderIntent)
}
