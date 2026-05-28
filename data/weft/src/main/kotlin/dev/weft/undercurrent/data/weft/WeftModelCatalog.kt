package dev.weft.undercurrent.data.weft

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import dev.weft.android.routing.catalogFor
import dev.weft.android.routing.defaultPoolFor
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.shared.gateway.ModelCatalog
import dev.weft.undercurrent.shared.gateway.ModelInfo
import dev.weft.undercurrent.shared.gateway.ModelPool
import dev.weft.contracts.ProviderKind as WeftProviderKind
import dev.weft.harness.agents.routing.ModelPool as WeftModelPool

/**
 * Android impl of [ModelCatalog]. Delegates to the substrate's
 * `catalogFor` / `defaultPoolFor` functions (which materialize Koog
 * [LLModel] instances) and projects them down to the commonMain mirror.
 */
class WeftModelCatalog : ModelCatalog {

    override fun modelsForProvider(provider: ProviderKind): List<ModelInfo> =
        catalogFor(provider.toWeft()).map { it.toCommon() }

    override fun defaultPoolForProvider(provider: ProviderKind): ModelPool =
        defaultPoolFor(provider.toWeft()).toCommon()

    private fun ProviderKind.toWeft(): WeftProviderKind = when (this) {
        ProviderKind.Anthropic -> WeftProviderKind.Anthropic
        ProviderKind.OpenAI -> WeftProviderKind.OpenAI
        ProviderKind.OpenRouter -> WeftProviderKind.OpenRouter
        ProviderKind.DeepSeek -> WeftProviderKind.DeepSeek
    }

    private fun LLModel.toCommon(): ModelInfo = ModelInfo(
        id = id,
        shortName = id,
        hasVision = capabilities.orEmpty().any { it is LLMCapability.Vision },
        hasTools = capabilities.orEmpty().any { it == LLMCapability.Tools },
    )

    private fun WeftModelPool.toCommon(): ModelPool = ModelPool(
        cheap = cheap.toCommon(),
        standard = standard.toCommon(),
        vision = vision.toCommon(),
        heavy = heavy.toCommon(),
    )
}
