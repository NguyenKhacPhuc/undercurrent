package dev.weft.undercurrent.features.providers

import dev.weft.contracts.ProviderKind

/**
 * Where to send a user who needs to grab an API key. Deep-links to each
 * provider's API-keys page directly; if the user isn't signed in yet,
 * the provider's site handles the login → signup → back-to-keys
 * redirect chain automatically (we don't have to model multi-step flows).
 *
 * The URL opens in a Chrome Custom Tab — keeps the user inside the app's
 * task, returns via the system back button with whatever key they
 * created waiting in their clipboard.
 *
 * Update if a provider changes its console URL. Test by tapping "Get an
 * API key" in [KeyPasteScreen] for each ProviderKind after upgrades.
 */
internal fun ProviderKind.apiConsoleUrl(): String = when (this) {
    ProviderKind.Anthropic -> "https://console.anthropic.com/settings/keys"
    ProviderKind.OpenAI -> "https://platform.openai.com/api-keys"
    ProviderKind.OpenRouter -> "https://openrouter.ai/keys"
    ProviderKind.DeepSeek -> "https://platform.deepseek.com/api_keys"
}

/**
 * Short signup pitch shown under the "Get an API key" button. Sets cost
 * expectations honestly without overpromising — the actual bill depends
 * on usage, but these ranges cover typical chat-app patterns.
 *
 * Phrased to ease the user's "is this going to cost me a lot?" worry —
 * they should walk away thinking "fine, a few bucks a month tops."
 */
internal fun ProviderKind.signupHint(): String = when (this) {
    ProviderKind.Anthropic ->
        "Free signup. Add $5 to start. Typical chat use: $1–5/month."
    ProviderKind.OpenAI ->
        "Free signup, pay as you go. Typical chat use: $1–10/month."
    ProviderKind.OpenRouter ->
        "One key, 40+ models. Pre-pay any amount, no subscription."
    ProviderKind.DeepSeek ->
        "Lowest cost per token. Typical chat use: under $1/month."
}
