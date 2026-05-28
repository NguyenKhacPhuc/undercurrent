package dev.weft.undercurrent.feature.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.feature.miniapps.SaveAsMiniAppDialog
import dev.weft.undercurrent.feature.voice.WaveformBars
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import dev.weft.undercurrent.shared.gateway.VoiceState

/**
 * Undercurrent's chat surface — document-style. Pure-view: receives
 * [displayMessages] + [inFlight] / [lastError] as inputs, calls [onSend]
 * to submit. The store (host's AppStore) owns the agent and the
 * streaming reduce loop; this screen only renders.
 *
 * Visual approach (the "AI-native minimal" direction):
 *  - No bubbles. Each message is a role label in small caps, then the
 *    body underneath at full width.
 *  - Assistant text goes through [MarkdownText] for **bold** / lists /
 *    `code` / fenced code blocks. User / tool / event text is plain.
 *  - Tool & event rows are single-line muted notes.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/chat/ChatScreen.kt`. Adjustments:
 *   - `dev.weft.compose.components.AgentSelector` (Android-only) →
 *     re-implementation in this module (same wire format).
 *   - `dev.weft.harness.agents.AgentDeclaration.DEFAULT_AGENT_NAME` →
 *     [DEFAULT_AGENT_NAME] constant in this module.
 *   - `dev.weft.harness.agents.routing.ModelTier` → `:core:model` mirror.
 *   - `dev.weft.harness.cost.UsageStore.totals.lastCallModelId` →
 *     [lastModelId] parameter (host reads via `UsageGateway`).
 *   - `dev.weft.harness.reliability.CircuitBreaker` → [DegradedMode]
 *     mirror, passed in as state.
 *   - `dev.weft.harness.skills.SkillRegistry` → `List<SkillSummary>?`
 *     parameter (host projects from its registry).
 *   - `dev.weft.undercurrent.features.voice.VoiceRecognizer` →
 *     [SpeechGateway].
 *   - `LocalClipboardManager` → [onCopyText] lambda.
 *   - Android permission flow → [hasMicPermission] flag +
 *     [onRequestMicPermission] lambda.
 *   - Material icons (Menu, Add, MoreVert, ArrowUpward, ArrowDropDown,
 *     AutoAwesome, Mic) → Unicode glyphs / plain text. The CMP build
 *     ships `material-icons-core` only; extended icons aren't on the
 *     commonMain classpath by default.
 *   - `MarkdownText`'s `openInBrowser` Custom Tabs call → [onOpenUrl]
 *     lambda.
 */
@Composable
fun ChatScreen(
    displayMessages: SnapshotStateList<DisplayMessage>,
    inFlight: Boolean,
    lastError: String?,
    onSend: (text: String, modelTier: ModelTier?) -> Unit,
    defaultTier: ModelTier?,
    threadTitle: String,
    threadSubtitle: String,
    activePersonaName: String,
    lastModelId: String?,
    degradedMode: DegradedMode?,
    speechGateway: SpeechGateway,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onCopyText: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteThread: () -> Unit,
    onRegenerate: () -> Unit = {},
    skills: List<SkillSummary>? = null,
    addToChatConfig: AddToChatConfig? = null,
    agents: List<AgentOption> = emptyList(),
    activeAgentName: String = DEFAULT_AGENT_NAME,
    onSelectAgent: (String) -> Unit = {},
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    var inputText by remember { mutableStateOf("") }
    var addToChatOpen by remember { mutableStateOf(false) }
    var saveFeaturePromptDraft by remember { mutableStateOf<String?>(null) }
    val inputFocus = remember { FocusRequester() }
    var messageTierOverride by remember { mutableStateOf<ModelTier?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) listState.animateScrollToItem(displayMessages.size - 1)
    }

    val voiceState by speechGateway.state.collectAsState()
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
                speechGateway.acknowledge()
            }
            is VoiceState.Error -> {
                voicePrefix = ""
                speechGateway.acknowledge()
            }
            else -> Unit
        }
    }

    DisposableEffect(speechGateway) {
        onDispose { speechGateway.cancel() }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ChatHeader(
            threadTitle = threadTitle,
            threadSubtitle = threadSubtitle,
            onOpenDrawer = onOpenDrawer,
            onNewChat = onNewChat,
            onDeleteThread = onDeleteThread,
        )

        DegradedModeBanner(degradedMode = degradedMode)

        val lastIsAssistant = displayMessages.lastOrNull()?.role == DisplayRole.ASSISTANT
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        ) {
            itemsIndexed(displayMessages, key = { _, msg -> msg.id }) { idx, msg ->
                MessageBlock(
                    msg = msg,
                    activePersonaName = activePersonaName,
                    onOpenUrl = onOpenUrl,
                )
                if (msg.role == DisplayRole.ASSISTANT) {
                    val isLast = idx == displayMessages.lastIndex
                    val isStreaming = inFlight && isLast
                    if (isStreaming) {
                        BlinkingCursor()
                    } else {
                        val precedingUserPrompt = remember(displayMessages, idx) {
                            (idx - 1 downTo 0)
                                .asSequence()
                                .map { displayMessages[it] }
                                .firstOrNull { it.role == DisplayRole.USER }
                                ?.text
                                ?.takeIf { it.isNotBlank() }
                        }
                        val canSaveFeature = addToChatConfig != null && precedingUserPrompt != null
                        AssistantActions(
                            onCopy = { onCopyText(msg.text) },
                            onRegenerate = if (isLast) onRegenerate else null,
                            onSaveAsFeature = if (canSaveFeature) {
                                { saveFeaturePromptDraft = precedingUserPrompt }
                            } else null,
                        )
                    }
                }
            }
            if (inFlight && !lastIsAssistant) {
                item("inflight") {
                    Text(
                        text = "Thinking…",
                        style = typography.sansSmall.copy(color = colors.inkMuted),
                    )
                }
            }
            lastError?.let { e ->
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
            default = defaultTier,
            lastModelId = lastModelId,
            onSelect = { messageTierOverride = it },
        )

        AgentSelector(
            options = agents,
            selectedName = activeAgentName,
            onSelect = onSelectAgent,
        )

        val isRecording = voiceState is VoiceState.Listening || voiceState is VoiceState.Partial
        InputRow(
            inputText = inputText,
            onInputChange = { inputText = it },
            inFlight = inFlight,
            inputFocus = inputFocus,
            showAddToChat = addToChatConfig != null,
            onOpenAddToChat = { addToChatOpen = true },
            onSend = {
                onSend(inputText, messageTierOverride)
                inputText = ""
                messageTierOverride = null
            },
            voiceAvailable = speechGateway.isAvailable,
            isRecording = isRecording,
            voiceRms = speechGateway.rmsdB,
            onMicPress = {
                if (!hasMicPermission) {
                    onRequestMicPermission()
                    false
                } else {
                    voicePrefix = inputText.trimEnd()
                    speechGateway.start()
                    true
                }
            },
            onMicRelease = { speechGateway.stop() },
        )

        val sheetCfg = addToChatConfig
        if (addToChatOpen && sheetCfg != null) {
            AddToChatSheet(
                activePersonaLabel = activePersonaName,
                activePalette = sheetCfg.activePalette,
                activeMode = sheetCfg.activeMode,
                connectedIntegrationsCount = sheetCfg.connectedIntegrationsCount,
                miniApps = sheetCfg.miniApps,
                onSelectPalette = sheetCfg.onSelectPalette,
                onSelectMode = sheetCfg.onSelectMode,
                onShowPersonas = sheetCfg.onShowPersonas,
                onShowIntegrations = sheetCfg.onShowIntegrations,
                onShowMiniApps = sheetCfg.onShowMiniApps,
                onInvokeMiniApp = sheetCfg.onInvokeMiniApp,
                onDismiss = { addToChatOpen = false },
            )
        }

        val saveDraft = saveFeaturePromptDraft
        if (saveDraft != null && sheetCfg != null) {
            SaveAsMiniAppDialog(
                initial = null,
                suggestedPrompt = saveDraft,
                onDismiss = { saveFeaturePromptDraft = null },
                onSave = { name, emoji, prompt ->
                    saveFeaturePromptDraft = null
                    sheetCfg.onAddMiniApp(name, emoji, prompt)
                },
            )
        }
    }
}

/**
 * Bundle of state + callbacks the host passes in to wire the "Add to
 * Chat" bottom sheet. Grouping them keeps [ChatScreen]'s signature from
 * ballooning further. Null = no `+` button.
 */
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

// ─────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(
    threadTitle: String,
    threadSubtitle: String,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteThread: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    var overflowOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "☰",
                style = typography.sansHeader.copy(
                    color = colors.ink,
                    fontSize = 22.sp,
                ),
                modifier = Modifier
                    .clickable(onClick = onOpenDrawer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = threadTitle,
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                )
                if (threadSubtitle.isNotBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = threadSubtitle,
                        style = typography.sansSmall.copy(color = colors.inkSubtle),
                        maxLines = 1,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(UndercurrentTheme.shapes.xsmall)
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "+",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontSize = 18.sp,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "New",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Box {
                Text(
                    text = "⋮",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontSize = 22.sp,
                    ),
                    modifier = Modifier
                        .clickable { overflowOpen = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                DropdownMenu(
                    expanded = overflowOpen,
                    onDismissRequest = { overflowOpen = false },
                    containerColor = colors.surface,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete thread",
                                style = typography.sansHeader.copy(
                                    color = colors.error,
                                    fontSize = 15.sp,
                                ),
                            )
                        },
                        onClick = {
                            overflowOpen = false
                            confirmDelete = true
                        },
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this thread?") },
            text = {
                val label = threadTitle.ifBlank { "(untitled)" }
                Text("\"$label\" and all its messages will be permanently removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteThread()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Message block
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun MessageBlock(
    msg: DisplayMessage,
    activePersonaName: String,
    onOpenUrl: (String) -> Unit,
) {
    when (msg.role) {
        DisplayRole.USER -> UserCard(text = msg.text)
        DisplayRole.ASSISTANT -> AssistantBlock(
            text = msg.text,
            personaName = activePersonaName,
            agentName = msg.agentName,
            onOpenUrl = onOpenUrl,
        )
        DisplayRole.TOOL -> {
            val info = msg.tool
            if (info != null) ToolPill(info) else InlineNote(text = msg.text, mono = true)
        }
        DisplayRole.EVENT -> InlineNote(text = msg.text, mono = false)
    }
}

@Composable
private fun UserCard(text: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surfaceMuted)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = text,
            style = typography.serifBody.copy(color = colors.ink),
        )
    }
}

@Composable
private fun AssistantBlock(
    text: String,
    personaName: String,
    agentName: String?,
    onOpenUrl: (String) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "UNDERCURRENT",
                style = typography.sansLabel.copy(
                    color = colors.inkSubtle,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "  —  ${personaName.uppercase()}",
                style = typography.sansLabel.copy(
                    color = colors.inkSubtle,
                    fontWeight = FontWeight.Normal,
                ),
            )
            if (!agentName.isNullOrBlank() && agentName != DEFAULT_AGENT_NAME) {
                Text(
                    text = "  ·  ${agentName.uppercase()}",
                    style = typography.sansLabel.copy(
                        color = colors.inkSubtle,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        MarkdownText(text = text, onLinkClick = onOpenUrl)
    }
}

@Composable
private fun ToolPill(info: ToolInfo) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .clip(shapes.large)
            .background(colors.surfaceMuted)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(status = info.status)
        Spacer(Modifier.width(8.dp))
        Text(
            text = info.name,
            style = typography.mono.copy(color = colors.ink),
        )
        info.argsPreview?.let { args ->
            DotSeparator()
            Text(
                text = "\"$args\"",
                style = typography.mono.copy(color = colors.inkMuted),
            )
        }
        info.resultPreview?.let { result ->
            DotSeparator()
            Text(
                text = result,
                style = typography.mono.copy(color = colors.inkMuted),
            )
        }
        if (info.status == ToolStatus.RUNNING && info.argsPreview == null && info.resultPreview == null) {
            DotSeparator()
            Text(
                text = "running…",
                style = typography.mono.copy(color = colors.inkMuted),
            )
        }
    }
}

@Composable
private fun DotSeparator() {
    Text(
        text = " · ",
        style = UndercurrentTheme.typography.mono.copy(
            color = UndercurrentTheme.colors.inkSubtle,
        ),
    )
}

@Composable
private fun StatusDot(status: ToolStatus) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val size = 16.dp
    when (status) {
        ToolStatus.DONE -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.inkMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                style = typography.sansLabel.copy(
                    color = colors.background,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        ToolStatus.RUNNING -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = colors.inkMuted,
                    shape = CircleShape,
                ),
        )
        ToolStatus.FAILED -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.error),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✗",
                style = typography.sansLabel.copy(
                    color = colors.background,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun InlineNote(text: String, mono: Boolean) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Text(
        text = text,
        style = (if (mono) typography.mono else typography.sansSmall)
            .copy(color = colors.inkMuted),
    )
}

// ─────────────────────────────────────────────────────────────────────
// Input row
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun InputRow(
    inputText: String,
    onInputChange: (String) -> Unit,
    inFlight: Boolean,
    inputFocus: FocusRequester,
    showAddToChat: Boolean,
    onOpenAddToChat: () -> Unit,
    onSend: () -> Unit,
    voiceAvailable: Boolean,
    isRecording: Boolean,
    voiceRms: kotlinx.coroutines.flow.StateFlow<Float>,
    onMicPress: () -> Boolean,
    onMicRelease: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAddToChat) {
            AddToChatButton(onClick = onOpenAddToChat, enabled = !inFlight)
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shapes.medium)
                .border(
                    width = 1.dp,
                    color = if (isRecording) colors.accent else colors.divider,
                    shape = shapes.medium,
                )
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocus),
                textStyle = typography.serifBody.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                enabled = !inFlight,
            )
            if (inputText.isEmpty()) {
                if (isRecording) {
                    WaveformBars(rms = voiceRms)
                } else {
                    Text(
                        text = "Write a paragraph back…",
                        style = typography.serifBody.copy(color = colors.inkSubtle),
                    )
                }
            }
        }

        if (voiceAvailable) {
            Spacer(Modifier.width(4.dp))
            MicButton(
                isRecording = isRecording,
                enabled = !inFlight,
                onMicPress = onMicPress,
                onMicRelease = onMicRelease,
            )
        }

        Spacer(Modifier.width(4.dp))

        val canSend = inputText.isNotBlank() && !inFlight
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shapes.medium)
                .background(colors.ink.copy(alpha = if (canSend) 1f else 0.35f))
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↑",
                style = typography.sansHeader.copy(
                    color = colors.background,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun TierChipRow(
    override: ModelTier?,
    default: ModelTier?,
    lastModelId: String?,
    onSelect: (ModelTier?) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    var menuOpen by remember { mutableStateOf(false) }

    val chipLabel = when {
        override != null -> override.shortName().uppercase()
        else -> "AUTO"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(shapes.medium)
                    .border(
                        width = 1.dp,
                        color = colors.divider,
                        shape = shapes.medium,
                    )
                    .background(colors.surface)
                    .clickable { menuOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sparkle glyph as model-picker affordance — material-icons-
                // extended's AutoAwesome isn't in CMP commonMain.
                Text(
                    text = "✦",
                    style = typography.sansLabel.copy(color = colors.inkMuted),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = chipLabel,
                    style = typography.sansLabel.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "▾",
                    style = typography.sansLabel.copy(color = colors.inkMuted),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = colors.surface,
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Auto", style = typography.sansHeader.copy(color = colors.ink))
                            Text(
                                text = if (default != null) "Default: ${default.shortName()}" else "Router decides",
                                style = typography.sansSmall.copy(color = colors.inkMuted),
                            )
                        }
                    },
                    onClick = {
                        menuOpen = false
                        onSelect(null)
                    },
                )
                listOf(ModelTier.Cheap, ModelTier.Standard, ModelTier.Heavy).forEach { tier ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                tier.shortName(),
                                style = typography.sansHeader.copy(color = colors.ink),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSelect(tier)
                        },
                    )
                }
            }
        }
        if (lastModelId != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = "· $lastModelId chosen this turn",
                style = typography.sansSmall.copy(color = colors.inkSubtle),
                maxLines = 1,
            )
        }
    }
}

private fun ModelTier.shortName(): String = when (this) {
    ModelTier.Cheap -> "Cheap"
    ModelTier.Standard -> "Standard"
    ModelTier.Vision -> "Vision"
    ModelTier.Heavy -> "Heavy"
}

@Composable
private fun AddToChatButton(onClick: () -> Unit, enabled: Boolean) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shapes.medium)
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            style = typography.sansHeader.copy(
                color = if (enabled) colors.ink else colors.inkSubtle,
                fontSize = 24.sp,
            ),
        )
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    enabled: Boolean,
    onMicPress: () -> Boolean,
    onMicRelease: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val shapes = UndercurrentTheme.shapes
    val typography = UndercurrentTheme.typography

    val bg = when {
        !enabled -> colors.surface
        isRecording -> colors.accent
        else -> colors.surface
    }
    val tint = when {
        !enabled -> colors.inkSubtle
        isRecording -> colors.onAccent
        else -> colors.inkMuted
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shapes.medium)
            .background(bg)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val started = onMicPress()
                        if (started) {
                            tryAwaitRelease()
                            onMicRelease()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Mic glyph — material-icons-extended's Mic isn't in CMP common
        // by default. The bullet plus the "Hold to record" affordance
        // gets the meaning across.
        Text(
            text = "●",
            style = typography.sansHeader.copy(
                color = tint,
                fontSize = 18.sp,
            ),
        )
    }
}

@Composable
private fun AssistantActions(
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)?,
    onSaveAsFeature: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ActionLink(label = "Copy", onClick = onCopy)
        if (onRegenerate != null) {
            ActionLink(label = "Regenerate", onClick = onRegenerate)
        }
        if (onSaveAsFeature != null) {
            ActionLink(label = "Save as feature", onClick = onSaveAsFeature)
        }
    }
}

@Composable
private fun ActionLink(label: String, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Text(
        text = label,
        style = typography.sansSmall.copy(color = colors.inkMuted),
        modifier = Modifier
            .clip(UndercurrentTheme.shapes.xsmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(22.dp)
            .background(UndercurrentTheme.colors.ink.copy(alpha = alpha)),
    )
}

// ─────────────────────────────────────────────────────────────────────
// Display model — public so the host's store can construct messages.
// ─────────────────────────────────────────────────────────────────────

data class DisplayMessage(
    val id: Long = nextId(),
    val role: DisplayRole,
    val text: String,
    val tool: ToolInfo? = null,
    val agentName: String? = null,
) {
    companion object {
        private var counter = 0L
        private fun nextId(): Long = ++counter
        fun user(text: String): DisplayMessage =
            DisplayMessage(role = DisplayRole.USER, text = text)

        fun assistant(text: String, agentName: String? = null): DisplayMessage =
            DisplayMessage(role = DisplayRole.ASSISTANT, text = text, agentName = agentName)

        fun toolStart(name: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "→ $name",
                tool = ToolInfo(name = name, status = ToolStatus.RUNNING),
            )

        fun toolDone(name: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "✓ $name",
                tool = ToolInfo(name = name, status = ToolStatus.DONE),
            )

        fun toolFail(name: String, message: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "✗ $name — $message",
                tool = ToolInfo(
                    name = name,
                    status = ToolStatus.FAILED,
                    resultPreview = message,
                ),
            )

        fun event(
            action: String,
            label: String?,
            fields: Map<String, String>,
        ): DisplayMessage {
            val text = buildString {
                append("Tapped")
                if (!label.isNullOrBlank()) append(" '$label'")
                append(" (action=$action)")
                if (fields.isNotEmpty()) {
                    append(" — ")
                    append(fields.entries.joinToString { (k, v) -> "$k=\"$v\"" })
                }
            }
            return DisplayMessage(role = DisplayRole.EVENT, text = text)
        }
    }
}

enum class DisplayRole(val label: String) {
    USER("You"),
    ASSISTANT("Undercurrent"),
    TOOL("Tool"),
    EVENT("UI event"),
}

data class ToolInfo(
    val name: String,
    val status: ToolStatus,
    val argsPreview: String? = null,
    val resultPreview: String? = null,
)

enum class ToolStatus { RUNNING, DONE, FAILED }
