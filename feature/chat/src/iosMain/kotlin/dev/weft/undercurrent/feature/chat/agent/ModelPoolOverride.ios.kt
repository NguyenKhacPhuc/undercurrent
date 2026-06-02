package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.agents.routing.ModelPool
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.model.ProviderKind

// iOS has no model catalog (Koog is JVM-only; see ios-agent-bringup
// out-of-scope). No overrides → the agent uses the runtime's default pool.
internal actual fun resolveModelPoolOverride(
    provider: ProviderKind,
    prefs: ModelPrefsRepository,
): ModelPool? = null
