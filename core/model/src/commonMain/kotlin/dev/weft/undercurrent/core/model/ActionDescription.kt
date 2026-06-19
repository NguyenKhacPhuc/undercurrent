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

/** Actions with an explicit (non-generic) description. */
val knownActions: Set<String> get() = ACTION_DESCRIPTIONS.keys

private fun phrases(present: String, past: String, failure: String, icon: String) =
    ActionDescription(present, past, failure, icon)

private val ACTION_DESCRIPTIONS: Map<String, ActionDescription> = mapOf(
    // — Maps & web —
    "open_map" to phrases("Looking at the map…", "Looked at the map", "Couldn't reach the map", "open"),
    "maps_open_directions" to phrases("Finding directions…", "Found directions", "Couldn't find directions", "open"),
    "web_search" to phrases("Searching the web…", "Searched the web", "Couldn't search the web", "search"),
    "http_fetch" to phrases("Fetching from the web…", "Fetched from the web", "Couldn't reach the web", "sync"),
    "network_fetch" to phrases("Fetching from the web…", "Fetched from the web", "Couldn't reach the web", "sync"),
    // — Theme —
    "set_theme_palette" to phrases("Changing the colours…", "Changed the colours", "Couldn't change the colours", "settings"),
    "set_theme_mode" to phrases("Switching the theme…", "Switched the theme", "Couldn't switch the theme", "settings"),
    // — Creating —
    "create_html_mini_app" to phrases("Building your mini-app…", "Built your mini-app", "Couldn't build the mini-app", "bolt"),
    "create_mini_app" to phrases("Building your mini-app…", "Built your mini-app", "Couldn't build the mini-app", "bolt"),
    "create_persona" to phrases("Creating a persona…", "Created a persona", "Couldn't create the persona", "add"),
    // — Navigation —
    "open_personas" to phrases("Opening your personas…", "Opened your personas", "Couldn't open personas", "open"),
    "open_memories" to phrases("Opening your memories…", "Opened your memories", "Couldn't open memories", "open"),
    "open_conversations" to phrases("Opening your conversations…", "Opened your conversations", "Couldn't open conversations", "open"),
    "open_integrations" to phrases("Opening integrations…", "Opened integrations", "Couldn't open integrations", "open"),
    "open_usage" to phrases("Opening usage…", "Opened usage", "Couldn't open usage", "open"),
    "ui_navigate" to phrases("Taking you there…", "Took you there", "Couldn't get there", "open"),
    // — Memory / notes —
    "data_upsert" to phrases("Remembering that…", "Remembered that", "Couldn't save that", "bookmark"),
    "data_query" to phrases("Looking through your notes…", "Looked through your notes", "Couldn't read your notes", "search"),
    "data_delete" to phrases("Forgetting that…", "Forgot that", "Couldn't forget that", "delete"),
    // — Sharing & clipboard —
    "share" to phrases("Sharing…", "Shared", "Couldn't share", "share"),
    "external_share" to phrases("Sharing…", "Shared", "Couldn't share", "share"),
    "files_share" to phrases("Sharing the file…", "Shared the file", "Couldn't share the file", "share"),
    "clipboard_read" to phrases("Reading the clipboard…", "Read the clipboard", "Couldn't read the clipboard", "notes"),
    "clipboard_write" to phrases("Copying to the clipboard…", "Copied to the clipboard", "Couldn't copy", "notes"),
    // — Device & info —
    "system_user_context" to phrases("Checking your context…", "Checked your context", "Couldn't read your context", "info"),
    "location_current" to phrases("Finding where you are…", "Found your location", "Couldn't find your location", "open"),
    "calendar_read" to phrases("Checking your calendar…", "Checked your calendar", "Couldn't read your calendar", "notes"),
    "calendar_create" to phrases("Adding to your calendar…", "Added to your calendar", "Couldn't add the event", "add"),
)
