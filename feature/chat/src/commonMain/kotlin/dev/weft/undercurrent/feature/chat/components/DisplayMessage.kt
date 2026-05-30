package dev.weft.undercurrent.feature.chat.components

data class DisplayMessage(
    val id: Long = nextId(),
    val role: DisplayRole,
    val text: String,
    val tool: ToolInfo? = null,
    val agentName: String? = null,
) {
    companion object {
        private var counter = 0L
        private fun nextId(): Long = ++counter

        fun user(text: String): DisplayMessage =
            DisplayMessage(role = DisplayRole.USER, text = text)

        fun assistant(text: String, agentName: String? = null): DisplayMessage =
            DisplayMessage(role = DisplayRole.ASSISTANT, text = text, agentName = agentName)

        fun toolStart(name: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "→ $name",
                tool = ToolInfo(name = name, status = ToolStatus.RUNNING),
            )

        fun toolDone(name: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "✓ $name",
                tool = ToolInfo(name = name, status = ToolStatus.DONE),
            )

        fun toolFail(name: String, message: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "✗ $name — $message",
                tool = ToolInfo(
                    name = name,
                    status = ToolStatus.FAILED,
                    resultPreview = message,
                ),
            )

        fun event(
            action: String,
            label: String?,
            fields: Map<String, String>,
        ): DisplayMessage {
            val text = buildString {
                append("Tapped")
                if (!label.isNullOrBlank()) append(" '$label'")
                append(" (action=$action)")
                if (fields.isNotEmpty()) {
                    append(" — ")
                    append(fields.entries.joinToString { (k, v) -> "$k=\"$v\"" })
                }
            }
            return DisplayMessage(role = DisplayRole.EVENT, text = text)
        }
    }
}

enum class DisplayRole(val label: String) {
    USER("You"),
    ASSISTANT("Undercurrent"),
    TOOL("Tool"),
    EVENT("UI event"),
}

data class ToolInfo(
    val name: String,
    val status: ToolStatus,
    val argsPreview: String? = null,
    val resultPreview: String? = null,
)

enum class ToolStatus { RUNNING, DONE, FAILED }
