package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * iOS stub. With no Weft agent on iOS for v1, the catalog is empty and
 * the default pool is a synthetic placeholder — the providers screen
 * is Android-only and shouldn't render this on iOS, but a working
 * stub keeps Koin happy.
 */
public class StubModelCatalog : ModelCatalog {
    override fun modelsForProvider(provider: ProviderKind): List<ModelInfo> = emptyList()

    override fun defaultPoolForProvider(provider: ProviderKind): ModelPool {
        val placeholder = ModelInfo(
            id = "unavailable",
            shortName = "Unavailable",
            hasVision = false,
            hasTools = false,
        )
        return ModelPool(cheap = placeholder, standard = placeholder)
    }
}
