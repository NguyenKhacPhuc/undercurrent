package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.ModelTier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun TierChipRow(
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

@Preview
@Composable
private fun TierChipRowPreview() {
    UndercurrentTheme {
        TierChipRow(
            override = null,
            default = ModelTier.Standard,
            lastModelId = "claude-haiku-4-5",
            onSelect = { },
        )
    }
}
