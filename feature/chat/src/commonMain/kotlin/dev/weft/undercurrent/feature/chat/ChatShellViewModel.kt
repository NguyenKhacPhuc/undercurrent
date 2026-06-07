package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.ConversationSummary
import dev.weft.undercurrent.core.domain.IntegrationsRepository
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.SpeechRepository
import dev.weft.undercurrent.core.domain.VoiceState
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * The chat screen's "chrome" state — everything around the message
 * stream that the surface used to read straight off repositories in the
 * Route: the conversation list (drawer + thread title), active provider
 * / default tier, persona label, connected-integration count, mini-apps,
 * and the speech-to-text session.
 *
 * Split from [ChatViewModel] (which owns the message stream) so the
 * heavily-tested streaming logic stays untouched; both VMs feed the one
 * stateless [ChatScreen] via the Route.
 */
data class ChatShellState(
    val conversations: List<ConversationSummary> = emptyList(),
    val activeProvider: ProviderKind = ProviderKind.Anthropic,
    val defaultTier: ModelTier? = null,
    val voiceName: String = DEFAULT_PERSONA,
    val roleName: String? = null,
    val connectedIntegrationsCount: Int = 0,
    val miniApps: List<MiniApp> = emptyList(),
    val voiceState: VoiceState = VoiceState.Idle,
) {
    val personaLabel: String
        get() = listOfNotNull(voiceName.takeIf { it != DEFAULT_PERSONA }, roleName)
            .joinToString(" + ")
            .ifEmpty { DEFAULT_PERSONA }

    val threadSubtitle: String
        get() = listOf(activeProvider.displayName, personaLabel).joinToString(" · ")

    fun conversationTitle(id: String?): String? =
        conversations.firstOrNull { it.id == id }?.title?.takeIf { it.isNotBlank() }

    companion object {
        const val DEFAULT_PERSONA: String = "Default"
    }
}

sealed interface ChatShellIntent {
    data class AddMiniApp(val name: String, val emoji: String, val prompt: String) : ChatShellIntent
    data object StartListening : ChatShellIntent
    data object StopListening : ChatShellIntent
    data object CancelListening : ChatShellIntent
    data object AcknowledgeVoice : ChatShellIntent
}

sealed interface ChatShellEffect

class ChatShellViewModel(
    private val speech: SpeechRepository,
    conversationStore: ConversationStoreRepository,
    providerPrefs: ProviderPrefsRepository,
    personaRepo: PersonaRepository,
    integrationsRepo: IntegrationsRepository,
    private val miniAppsRepo: MiniAppsRepository,
) : MviViewModel<ChatShellState, ChatShellIntent, ChatShellEffect>(
    initialState = ChatShellState(
        activeProvider = providerPrefs.activeProvider.value,
        defaultTier = providerPrefs.defaultTier.value,
        voiceName = personaRepo.activeVoice.value.name,
        roleName = personaRepo.activeRole.value?.name,
        miniApps = miniAppsRepo.miniApps.value,
    ),
) {
    /** Whether the platform has a speech recognizer. */
    val voiceAvailable: Boolean get() = speech.isAvailable

    /** Live mic RMS (dB) — drives the waveform; read directly off the gateway. */
    val voiceRms: StateFlow<Float> get() = speech.rmsdB

    init {
        conversationStore.search("").collectInto { copy(conversations = it) }
        providerPrefs.activeProvider.collectInto { copy(activeProvider = it) }
        providerPrefs.defaultTier.collectInto { copy(defaultTier = it) }
        personaRepo.activeVoice.collectInto { copy(voiceName = it.name) }
        personaRepo.activeRole.collectInto { copy(roleName = it?.name) }
        integrationsRepo.enabledIdsFlow.collectInto { copy(connectedIntegrationsCount = it.size) }
        miniAppsRepo.miniApps.collectInto { copy(miniApps = it) }
        speech.state.collectInto { copy(voiceState = it) }
    }

    override fun dispatch(intent: ChatShellIntent) = launch {
        when (intent) {
            is ChatShellIntent.AddMiniApp ->
                miniAppsRepo.add(intent.name, intent.emoji, intent.prompt)
            ChatShellIntent.StartListening -> speech.start()
            ChatShellIntent.StopListening -> speech.stop()
            ChatShellIntent.CancelListening -> speech.cancel()
            ChatShellIntent.AcknowledgeVoice -> speech.acknowledge()
        }
    }
}
