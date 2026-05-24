package dev.weft.undercurrent.features.savedfeatures

import kotlinx.serialization.Serializable

/**
 * A user-saved prompt-as-shortcut. The "feature" is really just the
 * [triggerPrompt] — the agent re-derives the tool sequence each time
 * the feature is invoked. This trades per-invocation LLM cost for
 * forward-compatibility: a feature saved before a new tool existed
 * picks up that tool automatically once the catalog grows.
 *
 * The user creates these by tapping "Save as feature" on a successful
 * assistant reply. The trigger prompt is the user message that
 * preceded the reply — i.e. "the thing that worked." Name + emoji
 * are user-chosen at creation time; a small dialog pre-fills both
 * from the prompt's opening words.
 *
 * Invocation: tap a chip in the "Add to Chat" sheet (or a row on the
 * management screen) → app dispatches [triggerPrompt] verbatim as if
 * the user had typed it. The agent loop runs end-to-end.
 *
 * @property id stable identifier — `feature.<uuid8>`. Used as the
 *   key in the persisted list + the bridge into the chip's hash key
 *   so re-ordering doesn't break recomposition state.
 * @property name short user-facing label (e.g. "Log water"). Shown
 *   on chips + on the management screen.
 * @property emoji single grapheme cluster used as the chip's visual
 *   anchor (e.g. "💧"). Free-form text — we don't enforce that it's
 *   an emoji because some users might prefer letters / kanji / dots.
 * @property triggerPrompt the user message dispatched on invocation.
 *   Saved verbatim. Parameterized prompts (`{amount}` placeholders)
 *   are a future enhancement; v1 is verbatim only.
 * @property createdAtEpochMs creation time for sort-by-recent on
 *   the management screen.
 * @property usageCount incremented every time the feature is invoked.
 *   Drives the sort order in the "Add to Chat" chip row so the user's
 *   most-used features stay at the front.
 */
@Serializable
internal data class SavedFeature(
    val id: String,
    val name: String,
    val emoji: String,
    val triggerPrompt: String,
    val createdAtEpochMs: Long,
    val usageCount: Int = 0,
    /**
     * Cached `ui_render` payload from the last time this feature was
     * invoked. JSON-encoded [dev.weft.contracts.ComponentNode]. When
     * non-null, tapping the feature seeds the Compose UI bridge with
     * this tree *immediately* and navigates to the rendered-tree
     * screen — the user sees the mini-app instantly while the agent
     * re-runs the trigger prompt to refresh whatever data the UI
     * displays.
     *
     * Null until the feature's first UI-producing invocation. Stale
     * data is acceptable here because the underlying data sources
     * are persistent (SQLDelight) — the UI shows correct values from
     * disk; only the agent's text narration drifts until the refresh
     * lands.
     */
    val lastRenderTreeJson: String? = null,
    /** Wall-clock time the cached tree was last refreshed. UI hint only. */
    val lastRenderedAtEpochMs: Long? = null,
)
