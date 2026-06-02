package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.agents.routing.ModelPool
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Per-provider model-pool override from the user's model prefs. Backed by
 * the Koog model catalog on Android; `null` on iOS (no catalog there —
 * Koog is JVM-only, see ios-agent-bringup out-of-scope), so the agent
 * uses the runtime's default pool.
 */
internal expect fun resolveModelPoolOverride(
    provider: ProviderKind,
    prefs: ModelPrefsRepository,
): ModelPool?
