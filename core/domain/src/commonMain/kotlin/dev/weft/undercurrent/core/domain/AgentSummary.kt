package dev.weft.undercurrent.core.domain

/**
 * Mirror for the substrate's `AgentDeclaration` — the lossy subset
 * the chat surface's agent selector needs. Pure domain so
 * `:core:domain` consumers don't pull substrate types.
 *
 * Hosts adapt their substrate's `runtime.agentDeclarations.values`
 * into a `List<AgentSummary>` at boot inside [ChatRepository]'s
 * platform impl.
 */
data class AgentSummary(
    val name: String,
    val displayName: String,
    val description: String,
)
