package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.chat_assistant_app_label
import dev.weft.undercurrent.core.resources.chat_tool_running
import dev.weft.undercurrent.feature.chat.DEFAULT_AGENT_NAME
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MessageBlock(
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
                text = stringResource(Res.string.chat_assistant_app_label),
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
                text = stringResource(Res.string.chat_tool_running),
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

@Preview
@Composable
private fun MessageBlockPreview() {
    UndercurrentTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
            MessageBlock(
                msg = DisplayMessage.user("How do I migrate to KMP?"),
                activePersonaName = "Default",
                onOpenUrl = { },
            )
            MessageBlock(
                msg = DisplayMessage.assistant(
                    "Start by extracting your domain into commonMain, then add platform impls.",
                ),
                activePersonaName = "Default",
                onOpenUrl = { },
            )
            MessageBlock(
                msg = DisplayMessage(
                    role = DisplayRole.TOOL,
                    text = "→ search_docs",
                    tool = ToolInfo(name = "search_docs", status = ToolStatus.RUNNING),
                ),
                activePersonaName = "Default",
                onOpenUrl = { },
            )
            MessageBlock(
                msg = DisplayMessage.toolDone("search_docs"),
                activePersonaName = "Default",
                onOpenUrl = { },
            )
            MessageBlock(
                msg = DisplayMessage.toolFail("search_docs", "404 not found"),
                activePersonaName = "Default",
                onOpenUrl = { },
            )
            MessageBlock(
                msg = DisplayMessage.event("submit", label = "Save", fields = mapOf("name" to "Phuc")),
                activePersonaName = "Default",
                onOpenUrl = { },
            )
        }
    }
}
