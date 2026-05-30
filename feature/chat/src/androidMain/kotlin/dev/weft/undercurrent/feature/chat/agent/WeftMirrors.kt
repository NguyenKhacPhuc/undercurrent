package dev.weft.undercurrent.feature.chat.agent

import dev.weft.android.WeftRuntime
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.contracts.ProviderKind as WeftProviderKind
import dev.weft.harness.agents.routing.ModelTier as WeftModelTier

fun ProviderKind.keyAlias(): String = when (this) {
    ProviderKind.Anthropic -> WeftRuntime.ANTHROPIC_KEY_ALIAS
    ProviderKind.OpenAI -> WeftRuntime.OPENAI_KEY_ALIAS
    ProviderKind.OpenRouter -> WeftRuntime.OPENROUTER_KEY_ALIAS
    ProviderKind.DeepSeek -> WeftRuntime.DEEPSEEK_KEY_ALIAS
}

fun ProviderKind.toWeft(): WeftProviderKind = when (this) {
    ProviderKind.Anthropic -> WeftProviderKind.Anthropic
    ProviderKind.OpenAI -> WeftProviderKind.OpenAI
    ProviderKind.OpenRouter -> WeftProviderKind.OpenRouter
    ProviderKind.DeepSeek -> WeftProviderKind.DeepSeek
}

fun ModelTier.toWeft(): WeftModelTier = when (this) {
    ModelTier.Cheap -> WeftModelTier.Cheap
    ModelTier.Standard -> WeftModelTier.Standard
    ModelTier.Vision -> WeftModelTier.Vision
    ModelTier.Heavy -> WeftModelTier.Heavy
}
