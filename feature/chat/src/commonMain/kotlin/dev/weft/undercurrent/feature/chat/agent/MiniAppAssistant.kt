package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.agents.AgentIntent
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.behavior.Turn

/**
 * Run [text] as a one-shot, non-streaming turn on [agent] and return the
 * assistant's reply. The host DI on both platforms wraps this to back a
 * mini-app's `window.weft.sendMessage`; supplying the current agent from
 * the shared [AgentSlot] keeps Android and iOS on one implementation.
 *
 * Throws when no agent is ready, or when the turn produced no assistant
 * text — the bridge surfaces either as a rejected Promise to the mini-app.
 *
 * v1: the turn runs on the agent's current conversation, so the exchange
 * lands in chat history. Isolating it on an ephemeral conversation is a
 * tracked follow-up (changes both platforms together).
 */
public suspend fun askAssistant(agent: WeftAgent?, text: String): String {
    val live = agent ?: throw IllegalStateException("assistant not ready")
    live.dispatchAndAwait(AgentIntent.Send(text = text, streaming = false))
    return lastAssistantText(live.state.value.history)
        ?: throw IllegalStateException("assistant returned no reply")
}

internal fun lastAssistantText(history: List<Turn>): String? =
    history.filterIsInstance<Turn.Assistant>().lastOrNull()?.text
