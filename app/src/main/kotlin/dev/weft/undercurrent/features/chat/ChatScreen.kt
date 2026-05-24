package dev.weft.undercurrent.features.chat

import dev.weft.undercurrent.features.voice.WaveformBars
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.harness.cost.UsageStore
import dev.weft.harness.reliability.CircuitBreaker
import dev.weft.harness.skills.SkillRegistry
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.features.voice.VoiceRecognizer
import dev.weft.undercurrent.features.voice.VoiceState

/**
 * Undercurrent's chat surface — document-style. Pure-view: receives
 * [displayMessages] and [inFlight] / [lastError] as inputs, calls [onSend]
 * to submit. The store (AppStore) owns the agent and the streaming reduce
 * loop; this screen only renders.
 *
 * Visual approach (the "AI-native minimal" direction):
 *  - No bubbles. Each message is a role label in small caps, then the
 *    body underneath at full width.
 *  - Assistant text goes through [MarkdownText] for **bold** / lists /
 *    `code` / fenced code blocks. User / tool / event text is plain.
 *  - Tool & event rows are single-line muted notes — they live in the
 *    conversation as inline grace notes, not full message blocks.
 *
 * Weft dialogs (yes/no, confirm, info) are rendered by `PendingRequestRenderer`
 * at the root of the composition tree.
 */
@Composable
public fun ChatScreen(
    displayMessages: SnapshotStateList<DisplayMessage>,
    inFlight: Boolean,
    lastError: String?,
    /**
     * Submit the user's text. [modelTier] is the per-message override —
     * `null` means "use the default from Settings, or let the router
     * decide if no default is set."
     */
    onSend: (text: String, modelTier: ModelTier?) -> Unit,
    /**
     * Default tier from Settings. Shown next to the tier chip when no
     * per-message override is set ("Auto · Standard") so the user can
     * see what would actually run.
     */
    defaultTier: ModelTier?,
    /**
     * Title shown in the header — usually the active conversation's title,
     * or "Undercurrent" for a brand-new chat without one yet. Plumbed in
     * from `AppState.agent.currentConversationId` + the conversation
     * store's summary lookup.
     */
    threadTitle: String,
    /**
     * Sub-title displayed under [threadTitle] — typically
     * "<Provider> · <ModelFamily> · <Persona>". Composed by MainActivity
     * from active state so this screen doesn't need to know about
     * persona repo or provider name formatting.
     */
    threadSubtitle: String,
    /**
     * Active persona name (e.g. "Default", "Tech writer"). Shown after
     * the assistant role label as "UNDERCURRENT — <name>".
     */
    activePersonaName: String,
    /**
     * Open the side navigation drawer. Wired to [androidx.compose.material3.DrawerState.open]
     * in [App].
     */
    onOpenDrawer: () -> Unit,
    /** Start a fresh conversation. Wired to `AppIntent.NewChat`. */
    onNewChat: () -> Unit,
    /**
     * Delete the conversation currently on screen, then start a fresh
     * one. Wired to `AppIntent.DeleteCurrentConversation`. Surfaced
     * inside the header's overflow (⋮) menu — destructive, so it sits
     * behind a confirmation dialog inside [ChatHeader].
     */
    onDeleteThread: () -> Unit,
    /**
     * Re-send the last user message ("ask again"). Wired to
     * [dev.weft.undercurrent.core.AppIntent.RegenerateLast]. Visible only on the
     * very last assistant message.
     */
    onRegenerate: () -> Unit = {},
    skills: SkillRegistry? = null,
    usageStore: UsageStore? = null,
    circuitBreaker: CircuitBreaker? = null,
    /**
     * State + callbacks for the "Add to Chat" bottom sheet — opened from
     * the `+` button on the input row. Theme controls live inline in the
     * sheet (was previously in Settings → Appearance). Style + Connectors
     * are drill-downs to the existing Personas / Integrations screens.
     *
     * Nullable as a group: if [addToChatConfig] is null, the `+` button
     * is hidden — useful for hosts that don't want to surface the sheet.
     */
    addToChatConfig: AddToChatConfig? = null,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var quickActionsOpen by remember { mutableStateOf(false) }
    var addToChatOpen by remember { mutableStateOf(false) }
    val inputFocus = remember { FocusRequester() }
    // Per-message tier override. null = "Auto" → falls back to defaultTier
    // (or router heuristic if defaultTier is also null). Reset to null
    // after each send so the override applies to exactly one message.
    var messageTierOverride by remember { mutableStateOf<ModelTier?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) listState.animateScrollToItem(displayMessages.size - 1)
    }

    // ─── Voice input wiring ────────────────────────────────────────────
    val voice = remember(context) { VoiceRecognizer(context) }
    DisposableEffect(voice) {
        onDispose { voice.destroy() }
    }
    val voiceState by voice.state.collectAsState()
    // Text already in the field when the user pressed the mic. Live
    // partials get appended to it ("prefix + ' ' + transcript") so the
    // existing draft isn't lost.
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
                voice.acknowledge()
            }
            is VoiceState.Error -> {
                // Errors surface inline via `lastError` if the agent's
                // SendChat ever needs them; for voice we just reset so
                // the mic icon recovers. Could plumb to Snackbar later.
                voicePrefix = ""
                voice.acknowledge()
            }
            else -> Unit
        }
    }

    // RECORD_AUDIO permission state. The launcher updates `permGranted`
    // when the user responds; the mic button checks it before starting.
    var permGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permGranted = granted },
    )

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ChatHeader(
            threadTitle = threadTitle,
            threadSubtitle = threadSubtitle,
            onOpenDrawer = onOpenDrawer,
            onNewChat = onNewChat,
            onDeleteThread = onDeleteThread,
        )

        if (circuitBreaker != null) {
            DegradedModeBanner(circuitBreaker = circuitBreaker)
        }

        val lastIsAssistant = displayMessages.lastOrNull()?.role == DisplayRole.ASSISTANT
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        ) {
            itemsIndexed(displayMessages, key = { _, msg -> msg.id }) { idx, msg ->
                MessageBlock(msg, activePersonaName = activePersonaName)
                if (msg.role == DisplayRole.ASSISTANT) {
                    val isLast = idx == displayMessages.lastIndex
                    val isStreaming = inFlight && isLast
                    if (isStreaming) {
                        // Streaming cursor: only on the last assistant
                        // message while a send is in flight. Replaces the
                        // "Thinking…" indicator once the first TextDelta
                        // has arrived.
                        BlinkingCursor()
                    } else {
                        AssistantActions(
                            onCopy = { clipboard.setText(AnnotatedString(msg.text)) },
                            onRegenerate = if (isLast) onRegenerate else null,
                        )
                    }
                }
            }
            // "Thinking…" only shows while we're still waiting for the
            // first chunk (no assistant message yet, or the last message
            // is still the user's). Once streaming starts, the cursor
            // above takes over.
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

        // Per-message tier chip — sits above the input on the left, with
        // the "what model actually ran the last turn" status to its right.
        // Reads UsageStore.lastCallModelId so the user can confirm the
        // router actually picked what they intended (esp. on Auto).
        val lastModelId = usageStore?.totals
            ?.collectAsState()?.value?.lastCallModelId
        TierChipRow(
            override = messageTierOverride,
            default = defaultTier,
            lastModelId = lastModelId,
            onSelect = { messageTierOverride = it },
        )

        InputRow(
            inputText = inputText,
            onInputChange = { inputText = it },
            inFlight = inFlight,
            inputFocus = inputFocus,
            skills = skills,
            quickActionsOpen = quickActionsOpen,
            onQuickActionsToggle = { quickActionsOpen = it },
            showAddToChat = addToChatConfig != null,
            onOpenAddToChat = { addToChatOpen = true },
            onSkillSelected = { skillName ->
                inputText = "/$skillName "
                inputFocus.requestFocus()
            },
            onSend = {
                onSend(inputText, messageTierOverride)
                inputText = ""
                messageTierOverride = null  // override is one-shot
            },
            voiceAvailable = voice.isAvailable,
            isRecording = voiceState is VoiceState.Listening || voiceState is VoiceState.Partial,
            voiceRms = voice.rmsdB,
            onMicPress = {
                if (!permGranted) {
                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    false
                } else {
                    voicePrefix = inputText.trimEnd()
                    voice.start()
                    true
                }
            },
            onMicRelease = { voice.stop() },
        )

        // "Add to Chat" bottom sheet — opened from the `+` button on the
        // input row. Theme picker lives inline here; Choose style and
        // Connectors are drill-downs that close the sheet first to keep
        // the back-button semantics clean (sheet → screen → back → chat).
        val sheetCfg = addToChatConfig
        if (addToChatOpen && sheetCfg != null) {
            AddToChatSheet(
                activePersonaLabel = activePersonaName,
                activePalette = sheetCfg.activePalette,
                activeMode = sheetCfg.activeMode,
                connectedIntegrationsCount = sheetCfg.connectedIntegrationsCount,
                onSelectPalette = sheetCfg.onSelectPalette,
                onSelectMode = sheetCfg.onSelectMode,
                onShowPersonas = sheetCfg.onShowPersonas,
                onShowIntegrations = sheetCfg.onShowIntegrations,
                onDismiss = { addToChatOpen = false },
            )
        }
    }
}

/**
 * Bundle of state + callbacks the host passes in to wire the "Add to
 * Chat" bottom sheet. Grouping them keeps [ChatScreen]'s signature from
 * ballooning further — and lets a host that doesn't want the sheet pass
 * `null` to suppress the `+` button entirely.
 */
public class AddToChatConfig internal constructor(
    internal val activePalette: dev.weft.undercurrent.theme.AppPalette,
    internal val activeMode: dev.weft.undercurrent.theme.ThemeMode,
    internal val connectedIntegrationsCount: Int,
    internal val onSelectPalette: (dev.weft.undercurrent.theme.AppPalette) -> Unit,
    internal val onSelectMode: (dev.weft.undercurrent.theme.ThemeMode) -> Unit,
    internal val onShowPersonas: () -> Unit,
    internal val onShowIntegrations: () -> Unit,
)

/**
 * Internal helper for hosts inside this module — composes an
 * [AddToChatConfig] without exposing the constructor publicly.
 */
internal fun addToChatConfig(
    activePalette: dev.weft.undercurrent.theme.AppPalette,
    activeMode: dev.weft.undercurrent.theme.ThemeMode,
    connectedIntegrationsCount: Int,
    onSelectPalette: (dev.weft.undercurrent.theme.AppPalette) -> Unit,
    onSelectMode: (dev.weft.undercurrent.theme.ThemeMode) -> Unit,
    onShowPersonas: () -> Unit,
    onShowIntegrations: () -> Unit,
): AddToChatConfig = AddToChatConfig(
    activePalette = activePalette,
    activeMode = activeMode,
    connectedIntegrationsCount = connectedIntegrationsCount,
    onSelectPalette = onSelectPalette,
    onSelectMode = onSelectMode,
    onShowPersonas = onShowPersonas,
    onShowIntegrations = onShowIntegrations,
)

// ─────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────

/**
 * Two-line header: drawer button + (title / subtitle stack) + "+ New".
 * Subtitle is the provider · model · persona breadcrumb so the user can
 * see at a glance which backend + voice is in play without diving into
 * Settings. Bottom divider establishes the boundary; no shadow.
 */
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

    // Overflow menu state lives in the header — the dialog renders
    // alongside so confirmation isn't yanked out to a different surface.
    var overflowOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open drawer",
                    tint = colors.ink,
                )
            }
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
            // "+ New" text button — starts a fresh conversation. Plain,
            // borderless; the leading "+" telegraphs "create new" without
            // needing a chip.
            Row(
                modifier = Modifier
                    .clip(UndercurrentTheme.shapes.xsmall)
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(16.dp),
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
            // Overflow menu — currently only houses Delete thread. As
            // more thread-level actions arrive (rename, pin, export…)
            // they hang here too so the header doesn't bloat with chips.
            Box {
                IconButton(onClick = { overflowOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = colors.ink,
                    )
                }
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

// ─────────────────────────────────────────────────────────────────────────
// Message block
// ─────────────────────────────────────────────────────────────────────────

/**
 * Dispatches on role. User messages get the "pull quote" card treatment
 * (a soft-tinted full-width card with rounded corners) — they read like
 * a question lifted into the margin. Assistant messages stay un-bubbled
 * with a UPPERCASE role label naming the active persona. Tool calls
 * render as inline pills; UI events as muted notes.
 */
@Composable
private fun MessageBlock(msg: DisplayMessage, activePersonaName: String) {
    when (msg.role) {
        DisplayRole.USER -> UserCard(text = msg.text)
        DisplayRole.ASSISTANT -> AssistantBlock(
            text = msg.text,
            personaName = activePersonaName,
        )
        DisplayRole.TOOL -> {
            val info = msg.tool
            if (info != null) ToolPill(info) else InlineNote(text = msg.text, mono = true)
        }
        DisplayRole.EVENT -> InlineNote(text = msg.text, mono = false)
    }
}

/**
 * User message — wrapped in a soft surfaceMuted card with large rounded
 * corners. No "YOU" label; the visual distinction is enough. Reads as a
 * pulled quote rather than a chat bubble (no tail, fills the row).
 */
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

/**
 * Assistant message — role label (UNDERCURRENT — PERSONA) above
 * full-width markdown body. No card treatment; this is where the
 * "document feel" pays off most.
 */
@Composable
private fun AssistantBlock(text: String, personaName: String) {
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
            // Em-dash separator + persona name in matching label style —
            // keeps the badge feeling like one continuous metadata line.
            Text(
                text = "  —  ${personaName.uppercase()}",
                style = typography.sansLabel.copy(
                    color = colors.inkSubtle,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        MarkdownText(text = text)
    }
}

/**
 * Tool-call pill. Status icon (filled dot / check / cross) + mono name,
 * with optional args + result previews separated by middle-dots when the
 * SDK forwards them (currently always null — the layout is ready for
 * when those slots get populated).
 */
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
        // Running with no other data → trailing "running…" so the pill
        // doesn't look frozen on long-running tools (e.g. memory_save).
        if (info.status == ToolStatus.RUNNING && info.argsPreview == null && info.resultPreview == null) {
            DotSeparator()
            Text(
                text = "running…",
                style = typography.mono.copy(color = colors.inkMuted),
            )
        }
    }
}

/** ` · ` separator between segments in a tool pill. */
@Composable
private fun DotSeparator() {
    Text(
        text = " · ",
        style = UndercurrentTheme.typography.mono.copy(
            color = UndercurrentTheme.colors.inkSubtle,
        ),
    )
}

/**
 * 14dp circular status indicator. DONE shows a check on inkMuted fill;
 * RUNNING is an unfilled outline (the "active" affordance is the trailing
 * "running…" text); FAILED shows a cross on error fill.
 */
@Composable
private fun StatusDot(status: ToolStatus) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val size = 16.dp
    when (status) {
        ToolStatus.DONE -> Box(
            modifier = Modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
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
                .clip(androidx.compose.foundation.shape.CircleShape)
                .border(
                    width = 1.5.dp,
                    color = colors.inkMuted,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ),
        )
        ToolStatus.FAILED -> Box(
            modifier = Modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
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

/**
 * UI events ("Tapped 'Foo'") render as compact muted notes. Distinct
 * from tool pills since they're triggered by the user clicking a
 * rendered-tree element, not by the agent.
 */
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

// ─────────────────────────────────────────────────────────────────────────
// Input row
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun InputRow(
    inputText: String,
    onInputChange: (String) -> Unit,
    inFlight: Boolean,
    inputFocus: FocusRequester,
    skills: SkillRegistry?,
    quickActionsOpen: Boolean,
    onQuickActionsToggle: (Boolean) -> Unit,
    /** When true, render the `+` button that opens the "Add to Chat" sheet. */
    showAddToChat: Boolean,
    onOpenAddToChat: () -> Unit,
    onSkillSelected: (String) -> Unit,
    onSend: () -> Unit,
    voiceAvailable: Boolean,
    isRecording: Boolean,
    /** Audio-level stream for the in-field waveform. Unused when not recording. */
    voiceRms: kotlinx.coroutines.flow.StateFlow<Float>,
    /**
     * Returns true if recording actually started (permission granted +
     * recognizer available). False means the press was consumed for
     * permission-request purposes only — don't bother awaiting release.
     */
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
        // Leading "+" — opens the "Add to Chat" bottom sheet. Hidden
        // when the host didn't supply an AddToChatConfig (lets embedders
        // suppress the feature without forking the input row).
        if (showAddToChat) {
            AddToChatButton(onClick = onOpenAddToChat, enabled = !inFlight)
            Spacer(Modifier.width(6.dp))
        }
        // Custom BasicTextField so we can fully theme the border + placeholder
        // — Material's OutlinedTextField bakes in chrome (label, indicator
        // line, focus halo) that fights the minimal document aesthetic.
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
                    // Waveform fills the placeholder slot while we're
                    // listening but no partial transcript has arrived
                    // yet. As soon as text comes in, BasicTextField
                    // takes over the slot naturally (inputText != empty).
                    WaveformBars(rms = voiceRms)
                } else {
                    Text(
                        text = "Write a paragraph back…",
                        style = typography.serifBody.copy(color = colors.inkSubtle),
                    )
                }
            }
        }

        // Mic — plain icon, no surface background, sits to the right of
        // the field. Stays push-and-hold for recording. The waveform inside
        // the field handles the "is listening" affordance.
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

        // Send — always ink-filled (cross-palette safe). Disabled state
        // dims via lower alpha so the affordance reads consistently.
        val canSend = inputText.isNotBlank() && !inFlight
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shapes.medium)
                .background(colors.ink.copy(alpha = if (canSend) 1f else 0.35f))
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Send",
                tint = colors.background,
            )
        }
    }
}

/**
 * Tier chip on the left + "what model actually ran the last turn" status
 * on the right. Shows the current per-message override (or "AUTO" when
 * none), with a sparkle icon + dropdown caret to invite the tap. After
 * the agent finishes a turn, the right-side status updates so the user
 * can confirm the router actually picked what they intended.
 *
 * The selection here is **per-message** — the calling code resets the
 * override to null after each send, so the chip naturally returns to
 * Auto without us managing reset state.
 */
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

    // Chip label — short and uppercase to fit the badge feel.
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
                // Sparkle icon — signals "model picker" without text labels.
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.inkMuted,
                    modifier = Modifier.size(14.dp),
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
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Pick model tier",
                    tint = colors.inkMuted,
                    modifier = Modifier.size(16.dp),
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
        // Last-turn model status — explains what the router actually picked.
        // Hidden until we have a value; appears after the first turn completes.
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

/**
 * Leading `+` button on the input row — opens the "Add to Chat" sheet.
 *
 * Uses the same 44dp size + medium-rounded surface as [MicButton] so the
 * two flanking affordances on the input row read as a matched pair (left:
 * add, right: mic). Disabled while a turn is in flight to discourage
 * mid-stream config tweaks (which would surface mid-response — confusing).
 */
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

/**
 * Push-and-hold mic button. Uses [detectTapGestures]'s `onPress` lambda
 * which suspends until the gesture ends — we call [onMicPress] when the
 * press starts, then [tryAwaitRelease] until the user lifts (or the
 * gesture is cancelled by a parent), then [onMicRelease].
 *
 * Visual: filled-circle when recording (accent bg, on-accent icon),
 * outlined when idle.
 */
@Composable
private fun MicButton(
    isRecording: Boolean,
    enabled: Boolean,
    onMicPress: () -> Boolean,
    onMicRelease: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val shapes = UndercurrentTheme.shapes

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
                            // Suspend until release (or gesture cancel).
                            tryAwaitRelease()
                            onMicRelease()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isRecording) "Recording" else "Hold to record",
            tint = tint,
        )
    }
}

/**
 * Small Copy / Regenerate actions rendered under an assistant message.
 * [onRegenerate] is null on every message except the final one — only the
 * latest reply can be regenerated.
 */
@Composable
private fun AssistantActions(
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ActionLink(label = "Copy", onClick = onCopy)
        if (onRegenerate != null) {
            ActionLink(label = "Regenerate", onClick = onRegenerate)
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

/**
 * Thin blinking ink caret rendered below the currently-streaming
 * assistant message. Visually a 2dp×20dp line — same character as a
 * print cursor, no font dependency. Stops with the composable when the
 * cursor leaves the screen (last message changes, or send finishes).
 */
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

// ─────────────────────────────────────────────────────────────────────────
// Display model (kept public so the store can construct messages)
// ─────────────────────────────────────────────────────────────────────────

public data class DisplayMessage(
    val id: Long = nextId(),
    val role: DisplayRole,
    val text: String,
    /**
     * Structured data for TOOL-role messages — name, status, optional
     * args/result previews. Populated by AppStore's stream consumer.
     * When non-null, [MessageBlock] renders a pill instead of the plain
     * `text` fallback. Args/result preview slots are reserved for when
     * the SDK starts forwarding them through StreamChunk.
     */
    val tool: ToolInfo? = null,
) {
    public companion object {
        private var counter = 0L
        private fun nextId(): Long = ++counter
        public fun user(text: String): DisplayMessage =
            DisplayMessage(role = DisplayRole.USER, text = text)

        public fun assistant(text: String): DisplayMessage =
            DisplayMessage(role = DisplayRole.ASSISTANT, text = text)

        public fun toolStart(name: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "→ $name",
                tool = ToolInfo(name = name, status = ToolStatus.RUNNING),
            )

        public fun toolDone(name: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "✓ $name",
                tool = ToolInfo(name = name, status = ToolStatus.DONE),
            )

        public fun toolFail(name: String, message: String): DisplayMessage =
            DisplayMessage(
                role = DisplayRole.TOOL,
                text = "✗ $name — $message",
                tool = ToolInfo(
                    name = name,
                    status = ToolStatus.FAILED,
                    resultPreview = message,
                ),
            )

        public fun event(
            action: String,
            label: String?,
            fields: Map<String, String>
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

public enum class DisplayRole(public val label: String) {
    USER("You"),
    ASSISTANT("Undercurrent"),
    TOOL("Tool"),
    EVENT("UI event"),
}

/**
 * Structured tool-call data carried by TOOL-role [DisplayMessage]s. Drives
 * the pill rendering in the message stream. Args/result previews are
 * currently always null — the SDK doesn't forward them yet, but the
 * renderer is wired to show them when they arrive.
 */
public data class ToolInfo(
    val name: String,
    val status: ToolStatus,
    val argsPreview: String? = null,
    val resultPreview: String? = null,
)

public enum class ToolStatus { RUNNING, DONE, FAILED }
