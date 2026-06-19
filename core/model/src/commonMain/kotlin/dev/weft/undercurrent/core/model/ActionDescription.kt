package dev.weft.undercurrent.core.model

/**
 * Plain-language description of an assistant action, for the live activity
 * indicator and the per-reply step trail. [present] is the in-progress
 * form (ends with an ellipsis — "Looking at the map…"), [past] the settled
 * form ("Looked at the map"), [failure] the human failure form ("Couldn't
 * reach the map"). [icon] is an icon-name token resolved by the UI layer
 * (`undercurrentIcon`) — it is the only field that may contain underscores.
 *
 * Resolve via [describeAction]; an unrecognized action (including ones a
 * mini-app introduces) gets [GenericActionDescription], so a raw
 * `snake_case` tool name is never shown to the user.
 *
 * Copy here is a sensible starter set; final tone is a content pass
 * (live-activity open-questions Q2).
 */
data class ActionDescription(
    val present: String,
    val past: String,
    val failure: String,
    val icon: String,
)

/** Shown for any action without an explicit entry. Never leaks a raw name. */
val GenericActionDescription: ActionDescription = ActionDescription(
    present = "Working…",
    past = "Done",
    failure = "Something went wrong",
    icon = "bolt",
)

/** Friendly description for [toolName], falling back to [GenericActionDescription]. */
fun describeAction(toolName: String): ActionDescription =
    ACTION_DESCRIPTIONS[toolName] ?: GenericActionDescription

private val ACTION_DESCRIPTIONS: Map<String, ActionDescription> = mapOf(
    "open_map" to ActionDescription(
        present = "Looking at the map…",
        past = "Looked at the map",
        failure = "Couldn't reach the map",
        icon = "open",
    ),
    "web_search" to ActionDescription(
        present = "Searching the web…",
        past = "Searched the web",
        failure = "Couldn't search the web",
        icon = "search",
    ),
    "set_theme_palette" to ActionDescription(
        present = "Changing the colours…",
        past = "Changed the colours",
        failure = "Couldn't change the colours",
        icon = "settings",
    ),
    "create_html_mini_app" to ActionDescription(
        present = "Building your mini-app…",
        past = "Built your mini-app",
        failure = "Couldn't build the mini-app",
        icon = "bolt",
    ),
)
