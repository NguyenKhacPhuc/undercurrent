package dev.weft.undercurrent.feature.chat.internal

import dev.weft.undercurrent.core.model.PermissionDialogState

internal object PermissionFailureParser {

    fun parse(toolName: String, message: String): PermissionDialogState? {
        val marker = "Permission denied"
        if (!message.contains(marker)) return null
        val tail = message.substringAfter(marker)
        val perms = tail.substringAfter(": ", missingDelimiterValue = "")
            .trimEnd('.', '!', ' ', '"', '\'')
        return PermissionDialogState(
            toolName = toolName,
            friendlyTitle = friendlyTitle(toolName),
            friendlyBody = friendlyBody(toolName, perms),
        )
    }

    private fun friendlyTitle(toolName: String): String = when {
        toolName.startsWith("location_") -> "Location access needed"
        toolName.startsWith("calendar_") -> "Calendar access needed"
        toolName.startsWith("contacts_") -> "Contacts access needed"
        toolName.startsWith("camera_") -> "Camera access needed"
        toolName == "notify_show" || toolName.startsWith("schedule_") -> "Notification permission needed"
        toolName.startsWith("bluetooth_") -> "Bluetooth permission needed"
        toolName.startsWith("audio_") -> "Microphone access needed"
        else -> "Permission needed"
    }

    private fun friendlyBody(toolName: String, perms: String): String {
        val action = when {
            toolName.startsWith("location_") -> "find your location"
            toolName.startsWith("calendar_") -> "read or update your calendar"
            toolName.startsWith("contacts_") -> "look up your contacts"
            toolName.startsWith("camera_") -> "take a photo"
            toolName == "notify_show" -> "post notifications"
            toolName.startsWith("schedule_") -> "schedule a notification"
            toolName.startsWith("bluetooth_") -> "see your paired Bluetooth devices"
            toolName.startsWith("audio_") -> "record audio"
            else -> "use this capability ($perms)"
        }
        return "Undercurrent needs permission to $action. Android won't show the system prompt again — " +
            "open Settings to grant the permission, then try once more."
    }
}
