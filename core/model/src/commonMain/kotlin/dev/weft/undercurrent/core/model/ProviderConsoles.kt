package dev.weft.undercurrent.core.model

/**
 * Pure-data per-provider helpers — where to send the user to grab a
 * key, plus the cost expectations to surface alongside the link.
 *
 * Lives in :core:model (not :feature:providers or :feature:keypaste)
 * because both features consume it. Keeping it as plain
 * [ProviderKind] extensions avoids dragging either feature module
 * into the other's dep graph.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/providers/ProviderConsoles.kt`.
 */
public fun ProviderKind.apiConsoleUrl(): String = when (this) {
    ProviderKind.Anthropic -> "https://console.anthropic.com/settings/keys"
    ProviderKind.OpenAI -> "https://platform.openai.com/api-keys"
    ProviderKind.OpenRouter -> "https://openrouter.ai/keys"
    ProviderKind.DeepSeek -> "https://platform.deepseek.com/api_keys"
}

/**
 * Cost-expectation pitch shown under "Get an API key". Phrased so the
 * user walks away with a realistic ceiling, not a worst-case fear.
 */
public fun ProviderKind.signupHint(): String = when (this) {
    ProviderKind.Anthropic ->
        "Free signup. Add \$5 to start. Typical chat use: \$1–5/month."
    ProviderKind.OpenAI ->
        "Free signup, pay as you go. Typical chat use: \$1–10/month."
    ProviderKind.OpenRouter ->
        "One key, 40+ models. Pre-pay any amount, no subscription."
    ProviderKind.DeepSeek ->
        "Lowest cost per token. Typical chat use: under \$1/month."
}

/** Hostname the provider talks to. Used in keypaste's privacy footer. */
public fun ProviderKind.hostName(): String = when (this) {
    ProviderKind.Anthropic -> "api.anthropic.com"
    ProviderKind.OpenAI -> "api.openai.com"
    ProviderKind.OpenRouter -> "openrouter.ai"
    ProviderKind.DeepSeek -> "api.deepseek.com"
}

/** Placeholder shown in the paste-your-key text field. */
public fun ProviderKind.keyPlaceholder(): String = when (this) {
    ProviderKind.Anthropic -> "sk-ant-…"
    ProviderKind.OpenAI -> "sk-…"
    ProviderKind.OpenRouter -> "sk-or-…"
    ProviderKind.DeepSeek -> "sk-…"
}
