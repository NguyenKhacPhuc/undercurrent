package dev.weft.undercurrent.core.model

/**
 * KMP-friendly mirror of Weft's `dev.weft.harness.agents.routing.ModelTier`.
 * Feature modules in commonMain use this; the Android bridge in
 * `:data:weft` translates to Weft's enum at the boundary.
 *
 * Each tier picks a different model from the provider's pool:
 *   - [Cheap] — small / fast / low-cost (Haiku-class)
 *   - [Standard] — default everyday model (Sonnet-class)
 *   - [Vision] — model with image input support
 *   - [Heavy] — frontier reasoning (Opus-class)
 */
public enum class ModelTier(public val displayName: String) {
    Cheap("Cheap"),
    Standard("Standard"),
    Vision("Vision"),
    Heavy("Heavy"),
}
