package dev.weft.undercurrent.features.miniapps

import kotlinx.serialization.Serializable

/**
 * A user-saved mini-app — really just a prompt-as-shortcut plus the
 * UI tree it produced. The agent re-derives the tool sequence on every
 * invocation, so a mini-app saved before a new tool existed picks
 * that tool up automatically as the catalog grows.
 *
 * The user creates these by tapping "Save as mini-app" on a successful
 * assistant reply that emitted a `ui_render` payload. The trigger
 * prompt is the user message that preceded that reply — "the thing
 * that worked." Name + emoji are user-chosen at creation time; a
 * small dialog pre-fills both from the prompt's opening words.
 *
 * Invocation: tap a card on the Mini Apps screen (or a chip in the
 * "Add to Chat" sheet) → app seeds the UI bridge with [lastRenderTreeJson]
 * (when present) for instant render, then dispatches [triggerPrompt]
 * verbatim so the agent re-runs and refreshes whatever data the UI
 * displays.
 *
 * @property id stable identifier — `feature.<uuid8>`. Prefix is
 *   historical (predates the "mini app" naming) and kept so existing
 *   user data carries over without a DataStore migration.
 * @property name short user-facing label (e.g. "Log water"). Shown
 *   on the card + on chips in the Add-to-Chat sheet.
 * @property emoji single grapheme cluster used as the chip's visual
 *   anchor (e.g. "💧"). Free-form — letters / kanji / dots are fine.
 * @property triggerPrompt the user message dispatched on invocation.
 *   Saved verbatim. Parameterized prompts (`{amount}` placeholders)
 *   are a future enhancement; v1 is verbatim only.
 * @property createdAtEpochMs creation time for sort-by-recent on the
 *   management screen.
 * @property usageCount incremented every time the mini-app is invoked.
 *   Drives the sort order in the "Add to Chat" chip row so the user's
 *   most-used mini-apps stay at the front.
 * @property lastRenderTreeJson cached `ui_render` payload from the
 *   last invocation. JSON-encoded
 *   [dev.weft.contracts.ComponentNode]. The Mini Apps screen renders
 *   each card by decoding this tree and walking it through the
 *   substrate's [dev.weft.compose.components.TreeRenderer]. Null
 *   until first UI-producing invocation; placeholder card rendered
 *   in that state.
 * @property lastRenderedAtEpochMs wall-clock time the cached tree
 *   was last refreshed. UI hint only.
 */
@Serializable
internal data class MiniApp(
    val id: String,
    val name: String,
    val emoji: String,
    val triggerPrompt: String,
    val createdAtEpochMs: Long,
    val usageCount: Int = 0,
    val lastRenderTreeJson: String? = null,
    val lastRenderedAtEpochMs: Long? = null,
)
