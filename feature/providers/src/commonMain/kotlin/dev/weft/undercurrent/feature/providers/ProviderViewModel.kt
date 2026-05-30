package dev.weft.undercurrent.feature.providers

interface ProviderViewModel {
    fun dispatch(intent: ProviderIntent)
}
