package dev.weft.undercurrent.core.domain

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val agentName: String? = null,
)

enum class ChatRole { USER, ASSISTANT }
