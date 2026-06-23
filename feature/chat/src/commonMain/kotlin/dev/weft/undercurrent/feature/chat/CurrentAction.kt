package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.ChatChunk

/**
 * The assistant action the live indicator is narrating right now, keyed by
 * its [toolName] (resolved to a friendly phrase via `describeAction`).
 * [failed] flips the indicator from the present-tense form to the failure
 * form. Null means no action in progress — the indicator shows dead-air hints.
 */
data class CurrentAction(
    val toolName: String,
    val failed: Boolean = false,
)

/**
 * Fold a streaming [ChatChunk] into the current-action line: a tool start
 * sets it (running), a completion clears it (back to dead-air), a failure
 * marks it failed (so the indicator can describe the failure), and turn-end
 * clears it. All other chunks (text deltas, errors) leave it unchanged.
 */
fun nextCurrentAction(current: CurrentAction?, chunk: ChatChunk): CurrentAction? = when (chunk) {
    is ChatChunk.ToolStart -> CurrentAction(chunk.toolName, failed = false)
    is ChatChunk.ToolDone -> null
    is ChatChunk.ToolFail -> CurrentAction(chunk.toolName, failed = true)
    ChatChunk.Done -> null
    else -> current
}
