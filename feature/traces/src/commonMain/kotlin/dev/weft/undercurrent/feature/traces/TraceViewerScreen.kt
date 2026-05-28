package dev.weft.undercurrent.feature.traces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.ui.ScaffoldTextAction
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.TokenDivider
import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.LlmCallTrace
import dev.weft.undercurrent.shared.gateway.ToolCallTrace
import dev.weft.undercurrent.shared.gateway.ToolStatus
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

/**
 * Lists recent agent traces and lets the user drill into a single turn
 * to see every LLM call + tool call. The "what just happened" view +
 * the surface that turns into a debug bundle the user can paste into a
 * chat with the SDK author when something misbehaves.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/traces/TraceViewerScreen.kt`. Adjustments:
 *   - Imports from `:core:ui` / `:core:design-system`.
 *   - Trace/LlmCall/ToolCall mirrors from `:shared`.
 *   - `java.text.SimpleDateFormat` → manual kotlinx.datetime "HH:mm:ss.SSS"
 *     formatter.
 *   - `"%.1fs".format(...)` → manual one-decimal helper (no `String.format`
 *     in commonMain stdlib).
 *   - `org.koin.compose.viewmodel.koinViewModel`.
 */
@Composable
public fun TraceViewerScreen(
    onBack: () -> Unit,
    onExportTrace: ((AgentTrace) -> Unit)? = null,
    store: TracesStore = koinViewModel(),
) {
    val s by store.state.collectAsState(); val traces = s.traces
    var selectedTraceId by remember { mutableStateOf<String?>(null) }
    val selected = selectedTraceId?.let { id -> traces.firstOrNull { it.id == id } }

    if (selected != null) {
        TraceDetail(
            t = selected,
            onBack = { selectedTraceId = null },
            onSetFeedback = { fb -> store.dispatch(TracesIntent.SetFeedback(selected.id, fb)) },
            onExport = onExportTrace?.let { exporter -> { exporter(selected) } },
        )
        return
    }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(
        title = "Traces",
        onBack = onBack,
        trailing = {
            ScaffoldTextAction(
                label = "Clear",
                onClick = { store.dispatch(TracesIntent.ClearAll) },
                enabled = traces.isNotEmpty(),
                isDestructive = true,
            )
        },
    ) {
        if (traces.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No traces yet",
                    style = typography.sansHeader.copy(color = colors.ink),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Send a message in chat to see a turn's trace here.",
                    style = typography.sansSmall.copy(color = colors.inkMuted),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                items(traces, key = { it.id }) { t ->
                    TraceRow(t, onClick = { selectedTraceId = t.id })
                    TokenDivider()
                }
            }
        }
    }
}

@Composable
private fun TraceRow(t: AgentTrace, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    val statusColor = when (t.status) {
        TraceStatus.RUNNING -> colors.inkMuted
        TraceStatus.COMPLETED -> colors.accent
        TraceStatus.FAILED -> colors.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(shapes.xsmall)
                        .background(colors.surfaceMuted)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = t.status.label,
                        style = typography.sansLabel.copy(color = statusColor),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTimeOfDay(t.startEpochMs),
                    style = typography.sansSmall.copy(color = colors.inkSubtle),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = t.userMessage,
                style = typography.serifBody.copy(color = colors.ink),
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOfNotNull(
                    "${t.llmCalls.size} LLM",
                    "${t.toolCalls.size} tool",
                    t.durationMs?.let { "${it}ms" },
                    t.totalTokens.takeIf { it > 0 }?.let { "$it tok" },
                ).joinToString(" · "),
                style = typography.sansSmall.copy(color = colors.inkSubtle),
            )
        }
    }
}

@Composable
private fun TraceDetail(
    t: AgentTrace,
    onBack: () -> Unit,
    onSetFeedback: (TraceFeedback) -> Unit,
    onExport: (() -> Unit)?,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copyAck by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Trace",
        onBack = onBack,
        trailing = {
            ScaffoldTextAction(
                label = if (copyAck) "Copied" else "Copy",
                onClick = {
                    clipboard.setText(AnnotatedString(formatTraceForClipboard(t)))
                    copyAck = true
                    scope.launch {
                        delay(1500)
                        copyAck = false
                    }
                },
            )
            if (onExport != null) ScaffoldTextAction(label = "Export", onClick = onExport)
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("feedback") { FeedbackRow(t.feedback, onSetFeedback) }
            item("meta") { TraceMetaBlock(t) }
            item("user") { LabeledBlock("User", t.userMessage) }
            t.finalAssistantMessage?.let { reply ->
                item("assistant") { LabeledBlock("Assistant", reply) }
            }
            t.errorMessage?.let { err ->
                item("error") {
                    LabeledBlock("Error", err, valueColor = colors.error)
                }
            }
            if (t.llmCalls.isNotEmpty()) {
                item("llm-header") {
                    Text(
                        text = "LLM CALLS · ${t.llmCalls.size}",
                        style = typography.sansLabel.copy(color = colors.inkSubtle),
                    )
                }
                items(t.llmCalls, key = { "llm-${it.id}" }) { call ->
                    LlmCallBlock(
                        call = call,
                        traceStartMs = t.startEpochMs,
                        onCopy = {
                            clipboard.setText(AnnotatedString(formatLlmCallForClipboard(call, t.startEpochMs)))
                        },
                    )
                }
            }
            if (t.toolCalls.isNotEmpty()) {
                item("tool-header") {
                    Text(
                        text = "TOOL CALLS · ${t.toolCalls.size}",
                        style = typography.sansLabel.copy(color = colors.inkSubtle),
                    )
                }
                items(t.toolCalls, key = { "tool-${it.id}" }) { call ->
                    ToolCallBlock(
                        call = call,
                        traceStartMs = t.startEpochMs,
                        onCopy = {
                            clipboard.setText(AnnotatedString(formatToolCallForClipboard(call, t.startEpochMs)))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackRow(current: TraceFeedback, onSet: (TraceFeedback) -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Was this turn helpful?",
            style = typography.sansSmall.copy(color = colors.inkMuted),
        )
        Spacer(modifier = Modifier.weight(1f))
        ScaffoldTextAction(
            label = if (current == TraceFeedback.THUMBS_UP) "👍 yes" else "👍",
            onClick = {
                onSet(if (current == TraceFeedback.THUMBS_UP) TraceFeedback.NONE else TraceFeedback.THUMBS_UP)
            },
        )
        ScaffoldTextAction(
            label = if (current == TraceFeedback.THUMBS_DOWN) "👎 no" else "👎",
            onClick = {
                onSet(if (current == TraceFeedback.THUMBS_DOWN) TraceFeedback.NONE else TraceFeedback.THUMBS_DOWN)
            },
        )
    }
}

@Composable
private fun TraceMetaBlock(t: AgentTrace) {
    val colors = UndercurrentTheme.colors
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(colors.surfaceMuted)
            .padding(12.dp),
    ) {
        MetaLine(label = "Trace id", value = t.id)
        MetaLine(label = "Status", value = t.status.label)
        MetaLine(label = "Conversation", value = t.conversationId)
        t.parentTraceId?.let { MetaLine(label = "Parent trace", value = it) }
        MetaLine(label = "Started", value = formatTimeOfDay(t.startEpochMs))
        t.durationMs?.let { MetaLine(label = "Duration", value = formatDuration(it)) }
        if (t.totalTokens > 0) {
            MetaLine(
                label = "Tokens",
                value = "${t.totalInputTokens} in / ${t.totalOutputTokens} out " +
                    "(${t.totalTokens} total)",
            )
        }
        val cacheRead = t.llmCalls.sumOf { it.cacheReadTokens ?: 0 }
        val cacheWrite = t.llmCalls.sumOf { it.cacheWriteTokens ?: 0 }
        if (cacheRead > 0 || cacheWrite > 0) {
            MetaLine(
                label = "Cache",
                value = "$cacheRead read / $cacheWrite write",
            )
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            text = label,
            style = typography.sansSmall.copy(color = colors.inkMuted),
            modifier = Modifier.width(108.dp),
        )
        Text(
            text = value,
            style = typography.sansSmall.copy(color = colors.ink),
        )
    }
}

@Composable
private fun LabeledBlock(
    label: String,
    text: String,
    valueColor: androidx.compose.ui.graphics.Color = UndercurrentTheme.colors.ink,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = typography.sansLabel.copy(color = colors.inkSubtle),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = typography.serifBody.copy(color = valueColor),
        )
    }
}

@Composable
private fun LlmCallBlock(call: LlmCallTrace, traceStartMs: Long, onCopy: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .border(width = 1.dp, color = colors.divider, shape = shapes.small)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = call.model,
                style = typography.sansHeader.copy(color = colors.ink),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "+${formatDuration(call.startEpochMs - traceStartMs)}",
                style = typography.sansSmall.copy(color = colors.inkSubtle),
            )
            call.durationMs?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(it),
                    style = typography.sansSmall.copy(color = colors.inkSubtle),
                )
            }
        }
        val tokens = call.totalTokens
        if (tokens != null && tokens > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${call.inputTokens ?: 0} in / ${call.outputTokens ?: 0} out (${tokens} total)",
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
        val cr = call.cacheReadTokens
        val cw = call.cacheWriteTokens
        if ((cr != null && cr > 0) || (cw != null && cw > 0)) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "cache: ${cr ?: 0} read / ${cw ?: 0} write",
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CopyChip(onClick = onCopy)
        }
    }
}

@Composable
private fun ToolCallBlock(call: ToolCallTrace, traceStartMs: Long, onCopy: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    val statusColor = when (call.status) {
        ToolStatus.RUNNING -> colors.inkMuted
        ToolStatus.COMPLETED -> colors.accent
        ToolStatus.FAILED -> colors.error
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .border(width = 1.dp, color = colors.divider, shape = shapes.small)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = call.toolName,
                style = typography.mono.copy(color = statusColor),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "+${formatDuration(call.startEpochMs - traceStartMs)}",
                style = typography.sansSmall.copy(color = colors.inkSubtle),
            )
            call.durationMs?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(it),
                    style = typography.sansSmall.copy(color = colors.inkSubtle),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CodeLine(label = "args", value = call.argsPreview)
        call.resultPreview?.let { result ->
            Spacer(modifier = Modifier.height(4.dp))
            CodeLine(label = "result", value = result)
        }
        call.errorMessage?.let {
            Spacer(modifier = Modifier.height(4.dp))
            CodeLine(label = "error", value = it, valueColor = colors.error)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CopyChip(onClick = onCopy)
        }
    }
}

@Composable
private fun CopyChip(onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .clip(shapes.xsmall)
            .background(colors.surfaceMuted)
            .clickable {
                onClick()
                pressed = true
                scope.launch {
                    delay(1200)
                    pressed = false
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (pressed) "✓ copied" else "Copy",
            style = typography.sansLabel.copy(color = colors.inkMuted),
        )
    }
}

@Composable
private fun CodeLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = UndercurrentTheme.colors.codeInk,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.xsmall)
            .background(colors.codeBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = typography.sansLabel.copy(color = colors.inkSubtle),
        )
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                text = value,
                style = typography.mono.copy(color = valueColor),
            )
        }
    }
}

// ─── Clipboard formatters ────────────────────────────────────────────

private fun formatTraceForClipboard(t: AgentTrace): String = buildString {
    appendLine("=== Trace ${t.id} ===")
    appendLine("Status:       ${t.status.label}")
    appendLine("Conversation: ${t.conversationId}")
    t.parentTraceId?.let { appendLine("Parent:       $it") }
    appendLine("Started:      ${formatTimeOfDay(t.startEpochMs)}")
    t.durationMs?.let { appendLine("Duration:     ${formatDuration(it)}") }
    if (t.totalTokens > 0) {
        appendLine("Tokens:       ${t.totalInputTokens} in / ${t.totalOutputTokens} out (${t.totalTokens})")
    }
    val cacheRead = t.llmCalls.sumOf { it.cacheReadTokens ?: 0 }
    val cacheWrite = t.llmCalls.sumOf { it.cacheWriteTokens ?: 0 }
    if (cacheRead > 0 || cacheWrite > 0) {
        appendLine("Cache:        $cacheRead read / $cacheWrite write")
    }
    appendLine()
    appendLine("--- USER ---")
    appendLine(t.userMessage)
    appendLine()
    t.finalAssistantMessage?.let {
        appendLine("--- ASSISTANT ---")
        appendLine(it)
        appendLine()
    }
    t.errorMessage?.let {
        appendLine("--- ERROR ---")
        appendLine(it)
        appendLine()
    }
    if (t.llmCalls.isNotEmpty()) {
        appendLine("--- LLM CALLS (${t.llmCalls.size}) ---")
        t.llmCalls.forEachIndexed { i, call ->
            appendLine("[${i + 1}] ${formatLlmCallForClipboard(call, t.startEpochMs).prependIndent("    ").trimStart()}")
        }
        appendLine()
    }
    if (t.toolCalls.isNotEmpty()) {
        appendLine("--- TOOL CALLS (${t.toolCalls.size}) ---")
        t.toolCalls.forEachIndexed { i, call ->
            appendLine("[${i + 1}] ${formatToolCallForClipboard(call, t.startEpochMs).prependIndent("    ").trimStart()}")
        }
    }
}.trimEnd()

private fun formatLlmCallForClipboard(call: LlmCallTrace, traceStartMs: Long): String = buildString {
    appendLine("${call.model} (+${formatDuration(call.startEpochMs - traceStartMs)}" +
        (call.durationMs?.let { " · ${formatDuration(it)}" } ?: "") + ")")
    val total = call.totalTokens ?: 0
    if (total > 0) {
        appendLine("    tokens: ${call.inputTokens ?: 0} in / ${call.outputTokens ?: 0} out ($total total)")
    }
    val cr = call.cacheReadTokens
    val cw = call.cacheWriteTokens
    if ((cr != null && cr > 0) || (cw != null && cw > 0)) {
        appendLine("    cache:  ${cr ?: 0} read / ${cw ?: 0} write")
    }
}.trimEnd()

private fun formatToolCallForClipboard(call: ToolCallTrace, traceStartMs: Long): String = buildString {
    val status = call.status.name
    val timing = "+${formatDuration(call.startEpochMs - traceStartMs)}" +
        (call.durationMs?.let { " · ${formatDuration(it)}" } ?: "")
    appendLine("${call.toolName} [$status] ($timing)")
    appendLine("    args:")
    appendLine(call.argsPreview.prependIndent("        "))
    call.resultPreview?.let {
        appendLine("    result:")
        appendLine(it.prependIndent("        "))
    }
    call.errorMessage?.let {
        appendLine("    error:")
        appendLine(it.prependIndent("        "))
    }
}.trimEnd()

/**
 * Friendlier duration formatting. Short (<1s) → millis with `ms`;
 * 1s–60s → seconds with one decimal; ≥60s → m:ss.
 *
 * `String.format("%.1f")` isn't in commonMain stdlib so we build the
 * one-decimal form manually.
 */
private fun formatDuration(ms: Long): String = when {
    ms < 1_000 -> "${ms}ms"
    ms < 60_000 -> "${oneDecimal(ms / 1000.0)}s"
    else -> "${ms / 60_000}m${(ms % 60_000) / 1000}s"
}

private fun oneDecimal(v: Double): String {
    val tenths = (v * 10.0 + if (v >= 0) 0.5 else -0.5).toLong()
    val whole = tenths / 10
    val frac = (if (tenths < 0) -tenths else tenths) % 10
    return "$whole.$frac"
}

@OptIn(ExperimentalTime::class)
private fun formatTimeOfDay(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val h = ldt.hour.toString().padStart(2, '0')
    val m = ldt.minute.toString().padStart(2, '0')
    val s = ldt.second.toString().padStart(2, '0')
    val ms = (epochMs % 1000).let { if (it < 0) it + 1000 else it }
        .toString().padStart(3, '0')
    return "$h:$m:$s.$ms"
}

private val TraceStatus.label: String
    get() = when (this) {
        TraceStatus.RUNNING -> "RUNNING"
        TraceStatus.COMPLETED -> "OK"
        TraceStatus.FAILED -> "FAIL"
    }
