package dev.weft.undercurrent.core.domain

/**
 * Streaming-turn events emitted by [ChatRepository.send] /
 * [ChatRepository.regenerateLast]. The chat ViewModel folds these
 * into its `displayMessages` list + per-turn status.
 *
 * Pure domain (no Compose / UI types) so impls in either
 * `:androidApp` (substrate-backed) or `:composeApp/iosMain` (Ktor-
 * backed) can produce them without referencing presentation code.
 */
sealed interface ChatChunk {

    /** Append text to the in-flight assistant message. */
    data class TextDelta(val text: String) : ChatChunk

    /** A tool started running — host renders a "tool-start" bubble. */
    data class ToolStart(val toolName: String) : ChatChunk

    /** A tool completed successfully — host renders a "tool-done" bubble. */
    data class ToolDone(val toolName: String) : ChatChunk

    /**
     * A tool failed. [permissionDialog] is non-null when the failure
     * was "Permission denied" and the host should surface a dialog
     * instead of just a fail bubble — Android impl parses the
     * substrate's tool-fail message and fills this in.
     */
    data class ToolFail(
        val toolName: String,
        val message: String,
        val permissionDialog: PermissionDialogPayload? = null,
    ) : ChatChunk

    /** Turn complete (Idle). Host clears in-flight + streaming trackers. */
    data object Done : ChatChunk

    /**
     * Surface a transient error. Most often "no agent available" or
     * "LLM call failed" — the VM clears in-flight + records
     * [Error.message] as `lastError` and emits a snackbar effect.
     */
    data class Error(val message: String) : ChatChunk
}

/**
 * Payload for the "permission needed" dialog the host surfaces when
 * a tool fails because the OS denied a runtime permission. Returned
 * inline with the [ChatChunk.ToolFail] chunk so the chat ViewModel
 * doesn't have to parse failure messages itself.
 */
data class PermissionDialogPayload(
    val toolName: String,
    val friendlyTitle: String,
    val friendlyBody: String,
)
