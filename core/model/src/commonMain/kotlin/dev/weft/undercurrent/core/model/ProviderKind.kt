package dev.weft.undercurrent.core.model

/**
 * KMP-friendly mirror of Weft's `dev.weft.contracts.ProviderKind`.
 * Feature modules in commonMain import this instead of Weft's enum
 * so they compile against iOS without dragging the substrate in.
 *
 * The Android bridge in `:data:weft` translates this to Weft's
 * actual ProviderKind at the surface where it crosses into agent /
 * credential code; iOS code uses this directly (without the
 * downstream agent calls).
 */
enum class ProviderKind(val displayName: String) {
    /** Anthropic native or Anthropic-API-compatible proxy. */
    Anthropic("Anthropic"),

    /** OpenAI native (api.openai.com). */
    OpenAI("OpenAI"),

    /** OpenRouter — one key to dozens of upstream models. */
    OpenRouter("OpenRouter"),

    /** DeepSeek (api.deepseek.com). OpenAI-compatible wire protocol. */
    DeepSeek("DeepSeek"),
}
