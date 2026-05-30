package dev.weft.undercurrent.feature.creator.internal

import dev.weft.harness.agents.AgentIntent
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.model.ChatStatus
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.chat.agent.AgentSession
import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.CreatorSession
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import dev.weft.undercurrent.shared.mvi.MviContext
import kotlinx.coroutines.launch

/**
 * Creator-flow lifecycle (Android impl). Each kickoff:
 *
 *   1. Starts a fresh [CreatorSession] for [CreatorKind].
 *   2. Clears the chat history + bumps the agent to a new
 *      conversation so the creator dialog starts from zero.
 *   3. Pushes the Creator screen.
 *   4. Dispatches a kind-specific kickoff prompt through the chat
 *      pipeline so the agent's `ui_render` payloads drive the flow.
 *
 * Cancel routes back to the screen the creator was launched from
 * (Personas / MiniApps / Settings as fallback).
 */
public class WeftCreatorViewModel(
    private val context: MviContext<AppState, AppEffect>,
    private val agentSession: AgentSession,
    private val chatVm: ChatViewModel,
    private val creatorSession: CreatorSession,
    private val navigationVm: NavigationViewModel,
) : CreatorViewModel {
    override fun dispatch(intent: CreatorIntent) {
        when (intent) {
            is CreatorIntent.StartCreator ->
                context.scope.launch { handleStartCreator(intent.kind) }
            CreatorIntent.CancelCreator ->
                context.scope.launch { handleCancelCreator() }
        }
    }

    private suspend fun handleStartCreator(kind: CreatorKind) {
        val a = agentSession.currentAgent ?: return
        creatorSession.start(kind)
        a.dispatchAndAwait(AgentIntent.NewChat)
        chatVm.clear()
        navigationVm.dispatch(NavigationIntent.Navigate(Screen.Creator))
        context.update {
            it.copy(
                chat = ChatStatus(),
                currentConversationId = a.state.value.conversationId,
            )
        }
        val kickoff = when (kind) {
            CreatorKind.PersonaVoice ->
                "I want to create a new voice persona. Ask me what I need."
            CreatorKind.PersonaRole ->
                "I want to create a new role persona. Ask me what I need."
            CreatorKind.MiniApp ->
                "I want to create a new mini-app. Ask me what I need."
        }
        chatVm.send(kickoff, modelTier = null)
    }

    private fun handleCancelCreator() {
        val kind = creatorSession.current()
        creatorSession.clear()
        val back = when (kind) {
            CreatorKind.PersonaVoice, CreatorKind.PersonaRole -> Screen.Personas
            CreatorKind.MiniApp -> Screen.MiniApps
            null -> Screen.Settings
        }
        agentSession.setRootScreen(back)
    }
}
