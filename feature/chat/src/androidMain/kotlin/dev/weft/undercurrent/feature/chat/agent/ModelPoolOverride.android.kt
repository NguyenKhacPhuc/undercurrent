package dev.weft.undercurrent.feature.chat.agent

import dev.weft.android.routing.defaultPoolFor
import dev.weft.android.routing.findModelInCatalog
import dev.weft.harness.agents.routing.ModelPool
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind

internal actual fun resolveModelPoolOverride(
    provider: ProviderKind,
    prefs: ModelPrefsRepository,
): ModelPool? {
    val anyOverride = ModelTier.entries.any { tier -> prefs.overrideFor(provider, tier) != null }
    if (!anyOverride) return null

    val weftProvider = provider.toWeft()
    val defaultPool = defaultPoolFor(weftProvider)
    fun resolve(tier: ModelTier, default: ai.koog.prompt.llm.LLModel) =
        prefs.overrideFor(provider, tier)
            ?.let { findModelInCatalog(weftProvider, it) }
            ?: default

    return ModelPool(
        cheap = resolve(ModelTier.Cheap, defaultPool.cheap),
        standard = resolve(ModelTier.Standard, defaultPool.standard),
        vision = resolve(ModelTier.Vision, defaultPool.vision),
        heavy = resolve(ModelTier.Heavy, defaultPool.heavy),
    )
}
