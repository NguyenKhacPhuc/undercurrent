package dev.weft.undercurrent.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.chat_thinking
import dev.weft.undercurrent.feature.chat.components.AddToChatSheet
import dev.weft.undercurrent.feature.chat.components.AssistantActions
import dev.weft.undercurrent.feature.chat.components.BlinkingCursor
import dev.weft.undercurrent.feature.chat.components.ChatHeader
import dev.weft.undercurrent.feature.chat.components.DegradedModeBanner
import dev.weft.undercurrent.feature.chat.components.DisplayMessage
import dev.weft.undercurrent.feature.chat.components.DisplayRole
import dev.weft.undercurrent.feature.chat.components.InputRow
import dev.weft.undercurrent.feature.chat.components.MessageBlock
import dev.weft.undercurrent.feature.chat.components.PreviewSpeechGateway
import dev.weft.undercurrent.feature.chat.components.TierChipRow
import dev.weft.undercurrent.feature.miniapps.SaveAsMiniAppDialog
import dev.weft.undercurrent.core.domain.SpeechRepository
import dev.weft.undercurrent.core.domain.VoiceState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ChatScreen(
    header: ChatHeaderConfig,
    messages: ChatMessagesConfig,
    input: ChatInputConfig,
    agent: ChatAgentConfig = ChatAgentConfig(),
    degradedMode: DegradedMode? = null,
    addToChatConfig: AddToChatConfig? = null,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    var inputText by remember { mutableStateOf("") }
    var addToChatOpen by remember { mutableStateOf(false) }
    var saveFeaturePromptDraft by remember { mutableStateOf<String?>(null) }
    val inputFocus = remember { FocusRequester() }
    var messageTierOverride by remember { mutableStateOf<ModelTier?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.displayMessages.size) {
        if (messages.displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(messages.displayMessages.size - 1)
        }
    }

    val voiceState by input.speechGateway.state.collectAsState()
    var voicePrefix by remember { mutableStateOf("") }

    LaunchedEffect(voiceState) {
        when (val s = voiceState) {
            is VoiceState.Partial -> {
                inputText = if (voicePrefix.isBlank()) s.text else "$voicePrefix ${s.text}"
            }
            is VoiceState.Final -> {
                if (s.text.isNotBlank()) {
                    inputText = if (voicePrefix.isBlank()) s.text else "$voicePrefix ${s.text}"
                }
                voicePrefix = ""
                input.speechGateway.acknowledge()
            }
            is VoiceState.Error -> {
                voicePrefix = ""
                input.speechGateway.acknowledge()
            }
            else -> Unit
        }
    }

    DisposableEffect(input.speechGateway) {
        onDispose { input.speechGateway.cancel() }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ChatHeader(
            threadTitle = header.threadTitle,
            threadSubtitle = header.threadSubtitle,
            onOpenDrawer = header.onOpenDrawer,
            onNewChat = header.onNewChat,
            onDeleteThread = header.onDeleteThread,
        )

        DegradedModeBanner(degradedMode = degradedMode)

        val lastIsAssistant = messages.displayMessages.lastOrNull()?.role == DisplayRole.ASSISTANT
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        ) {
            itemsIndexed(messages.displayMessages, key = { _, msg -> msg.id }) { idx, msg ->
                MessageBlock(
                    msg = msg,
                    activePersonaName = messages.activePersonaName,
                    onOpenUrl = messages.onOpenUrl,
                )
                if (msg.role == DisplayRole.ASSISTANT) {
                    val isLast = idx == messages.displayMessages.lastIndex
                    val isStreaming = messages.inFlight && isLast
                    if (isStreaming) {
                        BlinkingCursor()
                    } else {
                        val precedingUserPrompt = remember(messages.displayMessages, idx) {
                            (idx - 1 downTo 0)
                                .asSequence()
                                .map { messages.displayMessages[it] }
                                .firstOrNull { it.role == DisplayRole.USER }
                                ?.text
                                ?.takeIf { it.isNotBlank() }
                        }
                        val canSaveFeature = addToChatConfig != null && precedingUserPrompt != null
                        AssistantActions(
                            onCopy = { messages.onCopyText(msg.text) },
                            onRegenerate = if (isLast) messages.onRegenerate else null,
                            onSaveAsFeature = if (canSaveFeature) {
                                { saveFeaturePromptDraft = precedingUserPrompt }
                            } else null,
                        )
                    }
                }
            }
            if (messages.inFlight && !lastIsAssistant) {
                item("inflight") {
                    Text(
                        text = stringResource(Res.string.chat_thinking),
                        style = typography.sansSmall.copy(color = colors.inkMuted),
                    )
                }
            }
            messages.lastError?.let { e ->
                item("error") {
                    Text(
                        text = e,
                        style = typography.sansSmall.copy(color = colors.error),
                    )
                }
            }
        }

        TierChipRow(
            override = messageTierOverride,
            default = agent.defaultTier,
            lastModelId = agent.lastModelId,
            onSelect = { messageTierOverride = it },
        )

        AgentSelector(
            options = agent.agents,
            selectedName = agent.activeAgentName,
            onSelect = agent.onSelectAgent,
        )

        val isRecording = voiceState is VoiceState.Listening || voiceState is VoiceState.Partial
        InputRow(
            inputText = inputText,
            onInputChange = { inputText = it },
            inFlight = messages.inFlight,
            inputFocus = inputFocus,
            showAddToChat = addToChatConfig != null,
            onOpenAddToChat = { addToChatOpen = true },
            onSend = {
                input.onSend(inputText, messageTierOverride)
                inputText = ""
                messageTierOverride = null
            },
            voiceAvailable = input.speechGateway.isAvailable,
            isRecording = isRecording,
            voiceRms = input.speechGateway.rmsdB,
            onMicPress = {
                if (!input.hasMicPermission) {
                    input.onRequestMicPermission()
                    false
                } else {
                    voicePrefix = inputText.trimEnd()
                    input.speechGateway.start()
                    true
                }
            },
            onMicRelease = { input.speechGateway.stop() },
            onStop = input.onStop,
        )

        if (addToChatOpen && addToChatConfig != null) {
            AddToChatSheet(
                activePersonaLabel = messages.activePersonaName,
                activePalette = addToChatConfig.activePalette,
                activeMode = addToChatConfig.activeMode,
                connectedIntegrationsCount = addToChatConfig.connectedIntegrationsCount,
                miniApps = addToChatConfig.miniApps,
                onSelectPalette = addToChatConfig.onSelectPalette,
                onSelectMode = addToChatConfig.onSelectMode,
                onShowPersonas = addToChatConfig.onShowPersonas,
                onShowIntegrations = addToChatConfig.onShowIntegrations,
                onShowMiniApps = addToChatConfig.onShowMiniApps,
                onInvokeMiniApp = addToChatConfig.onInvokeMiniApp,
                onDismiss = { addToChatOpen = false },
            )
        }

        val saveDraft = saveFeaturePromptDraft
        if (saveDraft != null && addToChatConfig != null) {
            SaveAsMiniAppDialog(
                initial = null,
                suggestedPrompt = saveDraft,
                onDismiss = { saveFeaturePromptDraft = null },
                onSave = { name, emoji, prompt ->
                    saveFeaturePromptDraft = null
                    addToChatConfig.onAddMiniApp(name, emoji, prompt)
                },
            )
        }
    }
}

class ChatHeaderConfig(
    val threadTitle: String,
    val threadSubtitle: String,
    val onOpenDrawer: () -> Unit,
    val onNewChat: () -> Unit,
    val onDeleteThread: () -> Unit,
)

class ChatMessagesConfig(
    val displayMessages: SnapshotStateList<DisplayMessage>,
    val inFlight: Boolean,
    val lastError: String?,
    val activePersonaName: String,
    val onCopyText: (String) -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onRegenerate: () -> Unit = {},
)

class ChatInputConfig(
    val speechGateway: SpeechRepository,
    val hasMicPermission: Boolean,
    val onRequestMicPermission: () -> Unit,
    val onSend: (text: String, modelTier: ModelTier?) -> Unit,
    val onStop: () -> Unit = {},
)

class ChatAgentConfig(
    val agents: List<AgentOption> = emptyList(),
    val activeAgentName: String = DEFAULT_AGENT_NAME,
    val onSelectAgent: (String) -> Unit = {},
    val defaultTier: ModelTier? = null,
    val lastModelId: String? = null,
)

class AddToChatConfig(
    val activePalette: AppPalette,
    val activeMode: ThemeMode,
    val connectedIntegrationsCount: Int,
    val miniApps: List<MiniApp>,
    val onSelectPalette: (AppPalette) -> Unit,
    val onSelectMode: (ThemeMode) -> Unit,
    val onShowPersonas: () -> Unit,
    val onShowIntegrations: () -> Unit,
    val onShowMiniApps: () -> Unit,
    val onInvokeMiniApp: (MiniApp) -> Unit,
    val onAddMiniApp: (name: String, emoji: String, triggerPrompt: String) -> Unit,
)

@Preview
@Composable
private fun ChatScreenPreview() {
    UndercurrentTheme {
        ChatScreen(
            header = ChatHeaderConfig(
                threadTitle = "Migrating to KMP",
                threadSubtitle = "Claude Haiku 4.5 · Default",
                onOpenDrawer = { },
                onNewChat = { },
                onDeleteThread = { },
            ),
            messages = ChatMessagesConfig(
                displayMessages = mutableStateListOf(
                    DisplayMessage.user("How do I migrate to KMP?"),
                    DisplayMessage.assistant(
                        "Start by extracting your domain into commonMain, then add platform impls.",
                    ),
                ),
                inFlight = false,
                lastError = null,
                activePersonaName = "Default",
                onCopyText = { },
                onOpenUrl = { },
                onRegenerate = { },
            ),
            input = ChatInputConfig(
                speechGateway = PreviewSpeechGateway,
                hasMicPermission = true,
                onRequestMicPermission = { },
                onSend = { _, _ -> },
            ),
        )
    }
}
