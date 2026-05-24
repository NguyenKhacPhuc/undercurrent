package dev.weft.undercurrent.core

import dev.weft.harness.prompt.WeftSystemPromptDefaults

/**
 * App-level prompt preamble fed into [dev.weft.android.WeftRuntime.create]'s
 * `appPromptPreamble` slot. Composed of:
 *
 *  - **App identity + capabilities** — what kind of assistant this is
 *    and which substrate features the host wires up. Stays small so
 *    every conversation pays a low static-prefix cost.
 *  - **Substrate defaults** — [WeftSystemPromptDefaults.STANDARD]
 *    contributes the universal behavioral rules + the data-binding
 *    sentinels + multi-tool idioms. Same guidance every Weft host
 *    benefits from; concatenated rather than copy-pasted so a
 *    substrate-side change (new operator, tighter wording) reaches
 *    every consuming app on the next build.
 *
 * Note: the data-collection catalog (notes, tasks, and their
 * descriptions) is now auto-injected by the SDK's
 * `assembleSystemPrompt` — each registered `DataSource`'s `name` +
 * `description` lands in the prompt automatically. Update the
 * descriptions on the source-registration site in [appModule], not
 * here, when adding new collection categories.
 *
 * Lives in its own file so [UndercurrentApp] can read it without
 * depending on [MainActivity].
 */
internal val ASSISTANT_APP_PREAMBLE: String = APP_INTRO + WeftSystemPromptDefaults.STANDARD

/**
 * Identity + capability sketch. Small because the tool catalog and
 * UI-component catalog already enumerate the specifics; this block
 * just frames the assistant's role and orients the model toward the
 * available capability surfaces.
 */
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

