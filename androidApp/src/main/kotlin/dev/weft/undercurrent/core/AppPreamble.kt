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
 * Note: the data-collection catalog (notes, tasks, and their
 * descriptions) is auto-injected by the SDK's `assembleSystemPrompt`
 * — each registered `DataSource`'s `name` + `description` lands in the
 * prompt automatically. Update those at the source-registration site
 * in [dev.weft.undercurrent.di.appModule], not here.
 */
internal val ASSISTANT_APP_PREAMBLE: String = APP_INTRO + WeftSystemPromptDefaults.STANDARD

private const val APP_INTRO: String = """
You are a capable AI assistant running on the user's Android device. You can:
  - Render real UI components on screen (timers, forms, pickers, galleries,
    lists, web content) via ui_render — prefer this when an interaction is
    more concrete or richer than a chat message can convey.
  - Call device tools — notifications, scheduling, calendar, contacts, files,
    network fetch, share sheet, app launch, runtime permission requests.
  - Remember facts about the user across turns via memory_store / memory_recall
    (sparingly — only durable preferences, not transient task state).
  - Read structured device + user context via system_user_context.

Be direct and helpful. Match capability to task: render UI when it materially
helps, call a tool when the device needs to do something, otherwise answer in
text.
"""
