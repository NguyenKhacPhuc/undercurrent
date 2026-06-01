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
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.chat_tier_auto
import dev.weft.undercurrent.core.resources.chat_tier_auto_label
import dev.weft.undercurrent.core.resources.chat_tier_default_format
import dev.weft.undercurrent.core.resources.chat_tier_last_model_format
import dev.weft.undercurrent.core.resources.chat_tier_router_decides
import dev.weft.undercurrent.core.resources.model_tier_cheap
import dev.weft.undercurrent.core.resources.model_tier_heavy
import dev.weft.undercurrent.core.resources.model_tier_standard
import dev.weft.undercurrent.core.resources.model_tier_vision
import org.jetbrains.compose.resources.stringResource
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
        override != null -> stringResource(override.shortNameRes()).uppercase()
        else -> stringResource(Res.string.chat_tier_auto)
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
                            Text(stringResource(Res.string.chat_tier_auto_label), style = typography.sansHeader.copy(color = colors.ink))
                            Text(
                                text = if (default != null) {
                                    stringResource(Res.string.chat_tier_default_format, stringResource(default.shortNameRes()))
                                } else {
                                    stringResource(Res.string.chat_tier_router_decides)
                                },
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
                                stringResource(tier.shortNameRes()),
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
                text = stringResource(Res.string.chat_tier_last_model_format, lastModelId),
                style = typography.sansSmall.copy(color = colors.inkSubtle),
                maxLines = 1,
            )
        }
    }
}

private fun ModelTier.shortNameRes() = when (this) {
    ModelTier.Cheap -> Res.string.model_tier_cheap
    ModelTier.Standard -> Res.string.model_tier_standard
    ModelTier.Vision -> Res.string.model_tier_vision
    ModelTier.Heavy -> Res.string.model_tier_heavy
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
