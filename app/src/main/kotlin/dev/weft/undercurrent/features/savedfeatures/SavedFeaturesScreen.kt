package dev.weft.undercurrent.features.savedfeatures

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.ui.ScreenScaffold
import org.koin.androidx.compose.koinViewModel

/**
 * Drawer-reached management screen for saved features.
 *
 * The primary surface for *invoking* features is the "Add to Chat"
 * bottom sheet; this screen exists only for rename / edit / delete.
 * Tap a row → open editor. The empty state explains how to create
 * features (since the creation flow lives in chat, not here).
 *
 * No "+ New" button — by design. Creating a feature without first
 * running it loses the "this is the prompt that worked" property
 * that makes the save-from-history flow trustworthy. A user who
 * really wants to type a prompt from scratch can do so in chat and
 * save the result.
 */
@Composable
internal fun SavedFeaturesScreen(
    onBack: () -> Unit,
    vm: SavedFeaturesViewModel = koinViewModel(),
) {
    val features by vm.features.collectAsState()
    var editing by remember { mutableStateOf<SavedFeature?>(null) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(title = "My features", onBack = onBack) {
        if (features.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item("intro") {
                    Text(
                        text = "Saved features run a stored prompt with one tap. " +
                            "Use them for things you ask often.",
                        style = typography.serifBody.copy(
                            color = colors.ink,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                items(features, key = { it.id }) { feature ->
                    FeatureRow(
                        feature = feature,
                        onClick = { editing = feature },
                    )
                }
                item("footer") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Tap a feature to edit or delete it. To create a " +
                            "new feature, run a prompt in chat and tap “Save as " +
                            "feature” on the reply.",
                        style = typography.sansSmall.copy(color = colors.inkSubtle),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    editing?.let { feature ->
        SaveAsFeatureDialog(
            initial = feature,
            suggestedPrompt = feature.triggerPrompt,
            onDismiss = { editing = null },
            onSave = { name, emoji, prompt ->
                editing = null
                vm.update(feature.id, name, emoji, prompt)
            },
            onDelete = {
                editing = null
                vm.delete(feature.id)
            },
        )
    }
}

@Composable
private fun EmptyState() {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Nothing saved yet.",
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Run a prompt in chat. If it works, tap “Save as feature” on " +
                "the reply to keep it one tap away.",
            style = typography.serifBody.copy(
                color = colors.inkMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * One row per saved feature. Emoji tile on the left, name + prompt
 * preview in the middle, usage count on the right. Whole row taps to
 * the editor — no separate "edit" or "delete" buttons here, the
 * dialog has them.
 */
@Composable
private fun FeatureRow(
    feature: SavedFeature,
    onClick: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
            .background(colors.surface)
            .pointerInput(feature.id) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiTile(emoji = feature.emoji)
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.name,
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = feature.triggerPrompt,
                style = typography.serifBody.copy(
                    color = colors.inkMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                maxLines = 2,
            )
        }
        if (feature.usageCount > 0) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = "${feature.usageCount}×",
                style = typography.sansSmall.copy(
                    color = colors.inkSubtle,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun EmojiTile(emoji: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shapes.small)
            .background(colors.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 24.sp,
            ),
        )
    }
}
