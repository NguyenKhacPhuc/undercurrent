package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AssistantActions(
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

@Preview
@Composable
private fun AssistantActionsPreview() {
    UndercurrentTheme {
        AssistantActions(
            onCopy = { },
            onRegenerate = { },
            onSaveAsFeature = { },
        )
    }
}
