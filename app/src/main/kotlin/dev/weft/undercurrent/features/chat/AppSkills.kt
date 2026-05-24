package dev.weft.undercurrent.features.chat

import dev.weft.android.WeftRuntime
import dev.weft.harness.skills.SkillRegistry
import dev.weft.harness.skills.withHelp

/**
 * Build the Undercurrent app's skill registry. Lives here (not in the
 * substrate) because *which* skills an app ships is an app decision — the
 * substrate just provides the [dev.weft.harness.skills.Skill] /
 * [SkillRegistry] primitives and the `[+]` quick-actions surface area.
 *
 * Each skill bypasses the LLM and writes directly to the substrate's
 * registered stores. Saves ~3s of latency, saves tokens, keeps payload
 * on-device. Built-in `/help` is added automatically by [withHelp].
 *
 * Adding a new skill: define a [dev.weft.harness.skills.Skill] in the list,
 * give it a `name`, optional `aliases`, a `description` (surfaced in
 * `/help` and the quick-actions menu), and an `execute` lambda that calls
 * the relevant substrate API.
 */
internal fun buildSkillRegistry(@Suppress("UNUSED_PARAMETER") runtime: WeftRuntime): SkillRegistry {
    // .withHelp() is opt-in — the substrate doesn't auto-inject /help so
    // apps that prefer "only what I explicitly registered shows up" can
    // omit it. Undercurrent leans into discoverability.
    return SkillRegistry(skills = emptyList()).withHelp()
}
