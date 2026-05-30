package dev.weft.undercurrent.feature.chat.internal

import dev.weft.undercurrent.core.domain.AgentSummary
import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.domain.ChatRole
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.db.UndercurrentDatabase
import dev.weft.undercurrent.feature.chat.llm.IOS_SYSTEM_PROMPT
import dev.weft.undercurrent.feature.chat.llm.LlmChunk
import dev.weft.undercurrent.feature.chat.llm.LlmClient
import dev.weft.undercurrent.feature.chat.llm.LlmMessage
import dev.weft.undercurrent.feature.chat.llm.WeftAgentLlmClient
import dev.weft.undercurrent.feature.chat.llm.deepSeekClient
import dev.weft.undercurrent.feature.chat.llm.openAIClient
import dev.weft.undercurrent.feature.chat.llm.openRouterClient
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
internal class IosChatRepository(
    private val keyVault: KeyVaultRepository,
    private val providerPrefsRepo: ProviderPrefsRepository,
    private val personaRepo: PersonaRepository,
    private val db: UndercurrentDatabase,
    private val navigationVm: NavigationViewModel,
) : ChatRepository {

    private val clients: Map<ProviderKind, LlmClient> = mapOf(
        ProviderKind.Anthropic to WeftAgentLlmClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.Anthropic) },
        ),
        ProviderKind.OpenAI to openAIClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.OpenAI) },
        ),
        ProviderKind.OpenRouter to openRouterClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.OpenRouter) },
        ),
        ProviderKind.DeepSeek to deepSeekClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.DeepSeek) },
        ),
    )

    private val history: MutableList<LlmMessage> = mutableListOf()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    override val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _activeAgentName = MutableStateFlow(DEFAULT_AGENT_NAME)
    override val activeAgentName: StateFlow<String> = _activeAgentName.asStateFlow()

    private val _availableAgents = MutableStateFlow<List<AgentSummary>>(emptyList())
    override val availableAgents: StateFlow<List<AgentSummary>> = _availableAgents.asStateFlow()

    override fun send(text: String, modelTier: ModelTier?): Flow<ChatChunk> = flow {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            emit(ChatChunk.Done)
            return@flow
        }

        val provider = providerPrefsRepo.activeProviderNow()
        val client = clients[provider] ?: run {
            emit(ChatChunk.Error("No client for $provider"))
            emit(ChatChunk.Done)
            return@flow
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val conversationId = ensureConversation(now)
        val isFirstUserTurn = history.none { it.role == LlmMessage.ROLE_USER }

        history += LlmMessage(LlmMessage.ROLE_USER, trimmed)

        withContext(Dispatchers.Default) {
            db.conversationsQueries.insertMessage(
                id = newId("msg"),
                conversation_id = conversationId,
                role = LlmMessage.ROLE_USER,
                content = trimmed,
                created_at_ms = now,
            )
            db.conversationsQueries.touchConversation(now, conversationId)
            if (isFirstUserTurn) {
                db.conversationsQueries.updateConversationTitle(
                    title = trimmed.take(40),
                    ts = now,
                    id = conversationId,
                )
            }
        }

        val replyBuilder = StringBuilder()
        var sawError = false

        client.send(history, composeSystemPrompt()).collect { chunk ->
            when (chunk) {
                is LlmChunk.TextDelta -> {
                    replyBuilder.append(chunk.text)
                    emit(ChatChunk.TextDelta(chunk.text))
                }
                is LlmChunk.Error -> {
                    sawError = true
                    emit(ChatChunk.Error(chunk.message))
                }
                LlmChunk.Done -> Unit
            }
        }

        if (sawError) {
            // Roll the user turn off model-facing history but keep the
            // DB row — failed turn stays visible on reload.
            if (history.isNotEmpty()) history.removeAt(history.size - 1)
        } else {
            val reply = replyBuilder.toString()
            if (reply.isNotEmpty()) {
                history += LlmMessage(LlmMessage.ROLE_ASSISTANT, reply)
                val replyTs = Clock.System.now().toEpochMilliseconds()
                withContext(Dispatchers.Default) {
                    db.conversationsQueries.insertMessage(
                        id = newId("msg"),
                        conversation_id = conversationId,
                        role = LlmMessage.ROLE_ASSISTANT,
                        content = reply,
                        created_at_ms = replyTs,
                    )
                    db.conversationsQueries.touchConversation(replyTs, conversationId)
                }
            }
        }

        emit(ChatChunk.Done)
    }

    override fun regenerateLast(): Flow<ChatChunk> = flow {
        while (history.isNotEmpty() && history.last().role != LlmMessage.ROLE_USER) {
            history.removeAt(history.size - 1)
        }
        val lastUserText = history.lastOrNull()?.content
        if (lastUserText.isNullOrBlank()) {
            emit(ChatChunk.Done)
            return@flow
        }
        history.removeAt(history.size - 1)
        send(lastUserText, modelTier = null).collect { emit(it) }
    }

    override suspend fun resume() {
        val provider = providerPrefsRepo.activeProviderNow()
        val hasKey = withContext(Dispatchers.Default) {
            runCatching { keyVault.hasApiKey(provider) }.getOrDefault(false)
        }
        if (!hasKey) {
            _isReady.value = false
            return
        }
        val mostRecent = withContext(Dispatchers.Default) {
            db.conversationsQueries.listConversations().executeAsList().firstOrNull()
        }
        if (mostRecent != null) {
            hydrateFromConversation(mostRecent.id)
            _currentConversationId.value = mostRecent.id
        } else {
            history.clear()
            _currentConversationId.value = null
        }
        _isReady.value = true
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
    }

    override suspend fun newChat() {
        history.clear()
        _currentConversationId.value = null
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
    }

    override suspend fun selectConversation(id: String) {
        if (_currentConversationId.value == id) return
        hydrateFromConversation(id)
        _currentConversationId.value = id
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
    }

    override suspend fun deleteConversation(id: String) {
        val wasActive = _currentConversationId.value == id
        withContext(Dispatchers.Default) {
            db.transaction {
                db.conversationsQueries.deleteMessagesForConversation(id)
                db.conversationsQueries.deleteConversation(id)
            }
        }
        if (wasActive) {
            history.clear()
            _currentConversationId.value = null
            navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))
        }
    }

    override suspend fun selectAgent(name: String): Unit = Unit

    override suspend fun loadMessages(conversationId: String): List<ChatMessage> {
        val rows = withContext(Dispatchers.Default) {
            db.conversationsQueries.listMessagesByConversation(conversationId).executeAsList()
        }
        return rows.map { row ->
            ChatMessage(
                role = when (row.role) {
                    LlmMessage.ROLE_USER -> ChatRole.USER
                    LlmMessage.ROLE_ASSISTANT -> ChatRole.ASSISTANT
                    else -> ChatRole.USER
                },
                content = row.content,
                agentName = null,
            )
        }
    }

    private suspend fun ensureConversation(nowMs: Long): String {
        val existing = _currentConversationId.value
        if (existing != null) return existing
        val id = newId("conv")
        withContext(Dispatchers.Default) {
            db.conversationsQueries.insertConversation(
                id = id,
                title = "New chat",
                created_at_ms = nowMs,
                last_message_at_ms = nowMs,
            )
        }
        _currentConversationId.value = id
        return id
    }

    private suspend fun hydrateFromConversation(id: String) {
        val rows = withContext(Dispatchers.Default) {
            db.conversationsQueries.listMessagesByConversation(id).executeAsList()
        }
        history.clear()
        for (row in rows) {
            history += LlmMessage(role = row.role, content = row.content)
        }
    }

    private fun composeSystemPrompt(): String {
        val voiceText = personaRepo.activeVoice.value.systemPromptText
        val roleText = personaRepo.activeRole.value?.systemPromptText
        return buildString {
            append(IOS_SYSTEM_PROMPT.trim())
            if (voiceText.isNotBlank()) {
                append("\n\nVoice instructions:\n")
                append(voiceText)
            }
            if (!roleText.isNullOrBlank()) {
                append("\n\nRole instructions:\n")
                append(roleText)
            }
        }
    }

    private fun newId(prefix: String): String = "$prefix.${Uuid.random().toString().take(12)}"

    private companion object {
        const val DEFAULT_AGENT_NAME = "default"
    }
}
