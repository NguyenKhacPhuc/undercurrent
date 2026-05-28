package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Per-provider model directory. The Android impl in `:data:weft` wraps
 * Weft's `catalogFor` / `defaultPoolFor` functions which sit on top of
 * Koog's `LLModel` registry. iOS stub returns empty / falls back to
 * a synthetic placeholder pool so screens can render but don't claim
 * any model is usable.
 *
 * Mirror types here are intentionally lossy — feature code only needs
 * id, display name, and the two capability bits the provider screen
 * surfaces ("no vision", "no tools — limited agent use"). The full Koog
 * `LLModel` (and the executor build) stays on Android.
 */
public interface ModelCatalog {

    /** Full set of models the provider supports. */
    public fun modelsForProvider(provider: ProviderKind): List<ModelInfo>

    /** Default model-tier assignments for the provider. */
    public fun defaultPoolForProvider(provider: ProviderKind): ModelPool
}

/**
 * Lossy mirror of Koog's `LLModel`. The id round-trips through the
 * agent build path — when the user picks a model the id is what gets
 * written to prefs and resolved back by Weft.
 */
public data class ModelInfo(
    val id: String,
    val shortName: String,
    val hasVision: Boolean,
    val hasTools: Boolean,
) {
    /**
     * Provider-screen badge text for a model-in-tier combination, or
     * null when the model is a clean fit. Encodes the two warnings the
     * UI surfaces today.
     */
    public fun limitationNote(tier: ModelTier): String? = when {
        tier == ModelTier.Vision && !hasVision -> "no vision"
        !hasTools -> "no tools — limited agent use"
        else -> null
    }
}

/** Mirror of `dev.weft.harness.agents.routing.ModelPool` — typed by tier. */
public data class ModelPool(
    val cheap: ModelInfo,
    val standard: ModelInfo,
    val vision: ModelInfo = standard,
    val heavy: ModelInfo = standard,
) {
    public fun tierModel(tier: ModelTier): ModelInfo = when (tier) {
        ModelTier.Cheap -> cheap
        ModelTier.Standard -> standard
        ModelTier.Vision -> vision
        ModelTier.Heavy -> heavy
    }
}
