package dev.weft.undercurrent.feature.chat

/**
 * Mirror for the agent's circuit-breaker "degraded mode" state. Set
 * by the host when Weft's [dev.weft.harness.reliability.CircuitBreaker]
 * is `Open`; null when closed / half-open. Drives the
 * [DegradedModeBanner] above the chat surface.
 */
public data class DegradedMode(
    val openedAtEpochMs: Long,
    val cooldownMs: Long,
)

/**
 * Mirror for `dev.weft.harness.skills.Skill` — the lossy subset the
 * chat surface needs to render its quick-actions menu. Hosts produce
 * the list from their `SkillRegistry`.
 */
public data class SkillSummary(
    val name: String,
    val description: String,
)

/**
 * Mirror for `dev.weft.compose.components.AgentOption` from Weft's
 * `:android-compose-defaults`. Pure data — hosts adapt
 * `runtime.agentDeclarations.values` into the list.
 */
public data class AgentOption(
    val name: String,
    val displayName: String,
    val description: String = "",
)

/** Default agent name. Mirrors `AgentDeclaration.DEFAULT_AGENT_NAME`. */
public const val DEFAULT_AGENT_NAME: String = "default"
