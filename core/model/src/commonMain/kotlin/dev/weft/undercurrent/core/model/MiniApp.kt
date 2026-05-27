package dev.weft.undercurrent.core.model

import kotlinx.serialization.Serializable

/**
 * A user-saved "mini app" — a named prompt the agent runs on demand,
 * with an optional cached `ui_render` tree shown instantly while the
 * fresh turn streams.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/miniapps/MiniApp.kt`.
 *
 * @param id stable identifier. Prefix `feature.` kept for back-compat
 *   with the original "saved features" naming; persisted rows from
 *   that era still resolve.
 * @param triggerPrompt the user message the agent receives when the
 *   mini-app is invoked.
 * @param lastRenderTreeJson cached `ui_render` payload from the most
 *   recent invocation (if any). Shown instantly on next tap so the
 *   UI doesn't blink while the agent regenerates it.
 */
@Serializable
public data class MiniApp(
    val id: String,
    val name: String,
    val emoji: String,
    val triggerPrompt: String,
    val createdAtEpochMs: Long,
    val usageCount: Int = 0,
    val lastRenderTreeJson: String? = null,
    val lastRenderedAtEpochMs: Long? = null,
)
