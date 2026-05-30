package dev.weft.undercurrent.feature.chat

data class DegradedMode(
    val openedAtEpochMs: Long,
    val cooldownMs: Long,
)

data class SkillSummary(
    val name: String,
    val description: String,
)

data class AgentOption(
    val name: String,
    val displayName: String,
    val description: String = "",
)

const val DEFAULT_AGENT_NAME: String = "default"
