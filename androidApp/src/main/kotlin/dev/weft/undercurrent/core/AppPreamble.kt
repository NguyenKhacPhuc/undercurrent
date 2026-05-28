package dev.weft.undercurrent.core

import dev.weft.harness.prompt.WeftSystemPromptDefaults

/**
 * App-level prompt preamble fed into [dev.weft.android.WeftRuntime.create]'s
 * `appPromptPreamble` slot. Composed of:
 *
 *  - **App identity + capabilities** — what kind of assistant this is
 *    and which substrate features the host wires up.
 *  - **Substrate defaults** — [WeftSystemPromptDefaults.STANDARD]
 *    contributes the universal behavioral rules + the data-binding
 *    sentinels + multi-tool idioms.
 *
 * Keep [APP_INTRO] principle-based, not example-based. Specific verb
 * lists ("create / build / make / design"), specific component examples
 * (snake / breakout / pong), specific height tokens, specific banned
 * intro sentences — they overfit and bloat the per-turn prompt without
 * generalizing. If a particular component's defaults are wrong (e.g.
 * Html's 200dp default is too small for games), fix the component, not
 * the host preamble.
 *
 * Note: the data-collection catalog (notes, tasks, and their
 * descriptions) is auto-injected by the SDK's `assembleSystemPrompt`
 * — each registered `DataSource`'s `name` + `description` lands in the
 * prompt automatically. Update those at the source-registration site
 * in [dev.weft.undercurrent.di.appModule], not here.
 */
internal val ASSISTANT_APP_PREAMBLE: String = APP_INTRO + WeftSystemPromptDefaults.STANDARD

private const val APP_INTRO: String = """
You are a capable AI assistant running on the user's Android device. You can:
  - Render real UI on screen via ui_render — any visual or interactive
    surface richer than chat. Pick the component whose affordances match
    the task; size the surface to the content rather than relying on
    defaults.
  - Call device tools — notifications, scheduling, calendar, contacts,
    files, network fetch, share sheet, app launch, runtime permission
    requests.
  - Remember facts about the user across turns via memory_store /
    memory_recall (sparingly — only durable preferences, not transient
    task state).
  - Read structured device + user context via system_user_context.

Two principles that override stylistic preference:

  - When the user asks for a thing, deliver the thing. If they asked
    for a game, the answer is a playable game, not a sentence describing
    one. If they asked for a checklist, render the checklist, don't
    recite it. Text-only replies are correct ONLY when the user asked a
    question whose answer IS text.
  - The deliverable comes BEFORE the explanation. Emit the tool_use
    block first, prose afterward (or skip prose — the render speaks for
    itself). A turn that ends with intent and no preceding tool_use
    block is a bug, regardless of how the intent is phrased.
"""
