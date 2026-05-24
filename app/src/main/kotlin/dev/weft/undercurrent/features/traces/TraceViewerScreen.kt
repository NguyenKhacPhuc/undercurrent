package dev.weft.undercurrent.features.traces

import dev.weft.undercurrent.ui.TokenDivider
import dev.weft.undercurrent.ui.ScaffoldTextAction
import dev.weft.undercurrent.ui.ScreenScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.harness.observability.AgentTrace
import dev.weft.harness.observability.LlmCallTrace
import dev.weft.harness.observability.ToolCallTrace
import dev.weft.harness.observability.ToolStatus
import dev.weft.harness.observability.TraceFeedback
import dev.weft.harness.observability.TraceStatus
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.features.traces.TracesViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lists recent agent traces and lets the user drill into a single turn to
 * see every LLM call + tool call. Weft's "what just happened" view.
 */
@Composable
internal fun TraceViewerScreen(
    onBack: () -> Unit,
    onExportTrace: ((AgentTrace) -> Unit)? = null,
    vm: TracesViewModel = koinViewModel(),
) {
    val traces by vm.traces.collectAsState()
    var selectedTraceId by remember { mutableStateOf<String?>(null) }
    val selected = selectedTraceId?.let { id -> traces.firstOrNull { it.id == id } }

    if (selected != null) {
        TraceDetail(
            t = selected,
            onBack = { selectedTraceId = null },
            onSetFeedback = { fb -> vm.setFeedback(selected.id, fb) },
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
                onClick = { vm.clearAll() },
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
                    text = timeFormat.format(Date(t.startEpochMs)),
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

    ScreenScaffold(
        title = "Trace",
        onBack = onBack,
        trailing = {
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
                items(t.llmCalls, key = { "llm-${it.id}" }) { LlmCallBlock(it) }
            }
            if (t.toolCalls.isNotEmpty()) {
                item("tool-header") {
                    Text(
                        text = "TOOL CALLS · ${t.toolCalls.size}",
                        style = typography.sansLabel.copy(color = colors.inkSubtle),
                    )
                }
                items(t.toolCalls, key = { "tool-${it.id}" }) { ToolCallBlock(it) }
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
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(colors.surfaceMuted)
            .padding(12.dp),
    ) {
        MetaLine(label = "Status", value = t.status.label)
        MetaLine(label = "Conversation", value = "${t.conversationId.take(8)}…")
        MetaLine(label = "Started", value = timeFormat.format(Date(t.startEpochMs)))
        t.durationMs?.let { MetaLine(label = "Duration", value = "${it}ms") }
        if (t.totalTokens > 0) {
            MetaLine(
                label = "Tokens",
                value = "${t.totalInputTokens} in / ${t.totalOutputTokens} out (${t.totalTokens} total)",
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
private fun LlmCallBlock(call: LlmCallTrace) {
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
            call.durationMs?.let {
                Text(
                    text = "${it}ms",
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
    }
}

@Composable
private fun ToolCallBlock(call: ToolCallTrace) {
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
            call.durationMs?.let {
                Text(
                    text = "${it}ms",
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
            Text(
                text = "error: $it",
                style = typography.sansSmall.copy(color = colors.error),
            )
        }
    }
}

@Composable
private fun CodeLine(label: String, value: String) {
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
        Text(
            text = value,
            style = typography.mono.copy(color = colors.codeInk),
        )
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

private val TraceStatus.label: String
    get() = when (this) {
        TraceStatus.RUNNING -> "RUNNING"
        TraceStatus.COMPLETED -> "OK"
        TraceStatus.FAILED -> "FAIL"
    }
