package dev.weft.undercurrent.feature.chat.internal

import dev.weft.android.WeftRuntime
import dev.weft.harness.agents.AgentEffect
import dev.weft.harness.agents.AgentIntent
import dev.weft.harness.agents.TurnStatus
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.conversation.PersistedRole
import dev.weft.undercurrent.core.domain.AgentSummary
import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.domain.ChatRole
import dev.weft.undercurrent.core.domain.PermissionDialogPayload
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.agent.AgentSlot
import dev.weft.undercurrent.feature.chat.agent.WeftAgentFactory
import dev.weft.undercurrent.feature.chat.agent.keyAlias
import dev.weft.undercurrent.feature.chat.agent.toWeft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class WeftChatRepository(
    private val runtime: WeftRuntime,
    private val agentSlot: AgentSlot,
    private val agentFactory: WeftAgentFactory,
    private val providerPrefsRepo: ProviderPrefsRepository,
    private val navigationVm: NavigationViewModel,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Suppress("UNCHECKED_CAST")
    private val agentNull = flowOf<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentConversationId: StateFlow<String?> =
        agentSlot.flow
            .flatMapLatest { a ->
                a?.state?.map { it.conversationId as String? } ?: agentNull
            }
            .stateIn(scope, SharingStarted.Eagerly, null)

    override val isReady: StateFlow<Boolean> =
        agentSlot.flow
            .map { it != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    private val _activeAgentName = MutableStateFlow(DEFAULT_AGENT_NAME)
    override val activeAgentName: StateFlow<String> = _activeAgentName.asStateFlow()

    private val _availableAgents = MutableStateFlow<List<AgentSummary>>(emptyList())
    override val availableAgents: StateFlow<List<AgentSummary>> = _availableAgents.asStateFlow()

    override fun send(text: String, modelTier: ModelTier?): Flow<ChatChunk> =
        streamingTurn { agent ->
            agent.dispatch(AgentIntent.Send(text.trim(), tier = modelTier?.toWeft()))
        }

    override fun regenerateLast(): Flow<ChatChunk> =
        streamingTurn { agent -> agent.dispatch(AgentIntent.Regenerate()) }

    private fun streamingTurn(
        kickoff: (WeftAgent) -> Unit,
    ): Flow<ChatChunk> = channelFlow {
        val agent = agentSlot.agent
        if (agent == null) {
            send(ChatChunk.Error("No agent available"))
            return@channelFlow
        }

        var lastDelta = ""
        var sawNonIdle = false

        val observerJobs = mutableListOf<kotlinx.coroutines.Job>()

        observerJobs += launch {
            agent.state
                .map { it.pendingAssistantDelta to it.turnStatus }
                .distinctUntilChanged()
                .collect { (delta, status) ->
                    if (delta.length > lastDelta.length && delta.startsWith(lastDelta)) {
                        val newChars = delta.substring(lastDelta.length)
                        if (newChars.isNotEmpty()) {
                            send(ChatChunk.TextDelta(newChars))
                        }
                    } else if (delta.isNotEmpty() && delta != lastDelta) {
                        send(ChatChunk.TextDelta(delta))
                    }
                    lastDelta = delta

                    if (status != TurnStatus.Idle) sawNonIdle = true
                    if (sawNonIdle && (status == TurnStatus.Idle || status == TurnStatus.Failed)) {
                        send(ChatChunk.Done)
                        close()
                    }
                }
        }

        observerJobs += launch {
            agent.state
                .map { it.lastError }
                .distinctUntilChanged()
                .collect { err ->
                    if (err != null) {
                        send(ChatChunk.Error(err.message ?: err::class.simpleName.orEmpty()))
                    }
                }
        }

        observerJobs += launch {
            agent.effects.collect { ef ->
                when (ef) {
                    is AgentEffect.ToolStarting ->
                        send(ChatChunk.ToolStart(ef.toolName))

                    is AgentEffect.ToolCompleted ->
                        send(ChatChunk.ToolDone(ef.toolName))

                    is AgentEffect.ToolFailed -> {
                        if (ef.toolName == "llm.retry") return@collect
                        val parsed = PermissionFailureParser.parse(ef.toolName, ef.message)
                        val payload = parsed?.let {
                            PermissionDialogPayload(
                                toolName = it.toolName,
                                friendlyTitle = it.friendlyTitle,
                                friendlyBody = it.friendlyBody,
                            )
                        }
                        send(ChatChunk.ToolFail(ef.toolName, ef.message, payload))
                    }

                    is AgentEffect.Notify,
                    is AgentEffect.QuotaBlocked,
                    is AgentEffect.BreakerOpened -> Unit
                }
            }
        }

        kickoff(agent)

        awaitClose {
            observerJobs.forEach { it.cancel() }
        }
    }

    override suspend fun resume() {
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val storedKey = withContext(Dispatchers.IO) {
            runtime.keyVault.get(activeProvider.keyAlias())
        }
        if (storedKey == null) {
            return
        }
        val summaries = runtime.agentDeclarations.values
            .filter { it.userAddressable }
            .map { AgentSummary(it.name, it.displayName, it.description) }
        _availableAgents.value = summaries

        val a = agentFactory.build(
            agentName = _activeAgentName.value,
            provider = activeProvider,
            apiKey = storedKey,
        )
        a.dispatchAndAwait(AgentIntent.Resume())
        agentSlot.agent = a
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
    }

    override suspend fun newChat() {
        val a = agentSlot.agent ?: return
        a.dispatchAndAwait(AgentIntent.NewChat)
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
    }

    override suspend fun selectConversation(id: String) {
        val a = agentSlot.agent ?: return
        a.dispatchAndAwait(AgentIntent.Resume(id))
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
    }

    override suspend fun deleteConversation(id: String) {
        val a = agentSlot.agent ?: return
        val wasActive = a.state.value.conversationId == id
        withContext(Dispatchers.IO) {
            runtime.conversationStore.deleteConversation(id)
        }
        if (wasActive) {
            a.dispatchAndAwait(AgentIntent.NewChat)
            navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
        }
    }

    override suspend fun selectAgent(name: String) {
        if (name == _activeAgentName.value) return
        if (_availableAgents.value.none { it.name == name }) return
        val provider = providerPrefsRepo.activeProviderNow()
        val key = withContext(Dispatchers.IO) {
            runtime.keyVault.get(provider.keyAlias())
        } ?: return
        val a = agentFactory.build(agentName = name, provider = provider, apiKey = key)
        a.dispatchAndAwait(AgentIntent.Resume())
        _activeAgentName.value = name
        agentSlot.agent = a
    }

    override suspend fun loadMessages(conversationId: String): List<ChatMessage> {
        val rows = withContext(Dispatchers.IO) {
            runtime.conversationStore.loadMessages(conversationId)
        }
        return rows.map { m ->
            ChatMessage(
                role = when (m.role) {
                    PersistedRole.USER -> ChatRole.USER
                    PersistedRole.ASSISTANT -> ChatRole.ASSISTANT
                },
                content = m.content,
                agentName = m.agentName,
            )
        }
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        val a = agentSlot.agent ?: return
        a.dispatch(AgentIntent.SendEvent(action, sourceLabel, fieldValues))
    }

    fun dispose() {
        scope.cancel()
    }

    private companion object {
        const val DEFAULT_AGENT_NAME = "default"
    }
}
