package dev.weft.undercurrent.feature.chat.components

import dev.weft.undercurrent.core.model.describeAction

/**
 * One completed step in a reply's trail, keyed by its [toolName] (resolved
 * to a friendly past-tense phrase via `describeAction`). [failed] flips it
 * from the "done" form to the human failure phrase.
 */
data class TrailStep(
    val toolName: String,
    val failed: Boolean = false,
)

/**
 * Render [steps] as one compact, plain-language trail line — e.g.
 * "✓ Looked at the map · ✕ Couldn't search the web". Successful steps are
 * checked, failed steps marked distinctly with the human failure phrase
 * (no raw error text). Empty for an actionless reply. The UI shows this in
 * a low-contrast style above the answer.
 */
fun trailLine(steps: List<TrailStep>): String =
    steps.joinToString(separator = " · ") { step ->
        val described = describeAction(step.toolName)
        if (step.failed) "✕ ${described.failure}" else "✓ ${described.past}"
    }
