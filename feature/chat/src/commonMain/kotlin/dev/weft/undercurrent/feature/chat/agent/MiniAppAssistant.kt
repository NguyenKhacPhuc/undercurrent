package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.agents.WeftAgent

/**
 * Ask the assistant [text] off-the-record and return its reply. Runs an
 * isolated turn ([WeftAgent.ask]) so the question and answer do NOT enter
 * the visible conversation or its saved history — the host DI on both
 * platforms wraps this to back a mini-app's `window.weft.sendMessage`,
 * keeping the user's chat clean.
 *
 * Throws when no agent is ready (or the turn produced no reply) — the
 * bridge surfaces either as a rejected Promise to the mini-app.
 */
public suspend fun askAssistant(agent: WeftAgent?, text: String): String {
    val live = agent ?: throw IllegalStateException("assistant not ready")
    return live.ask(text)
}
