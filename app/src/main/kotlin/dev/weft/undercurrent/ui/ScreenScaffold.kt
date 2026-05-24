package dev.weft.undercurrent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.theme.UndercurrentTheme

/**
 * Shared chrome for back-navigable secondary screens (Settings, Conversations,
 * Memories, Traces). Provides a token-styled top bar with back arrow + title
 * + optional trailing slot, a hairline divider, and the content area.
 *
 * Pulled out so the three list screens stay visually consistent — change the
 * scaffold and every screen follows.
 */
@Composable
internal fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Trailing actions, laid out as a horizontal row at the right edge
     * of the header. The lambda runs inside a `RowScope` so emitting
     * multiple [ScaffoldTextAction]s back-to-back flows them
     * left-to-right with a small gap between them (rather than stacking
     * them on top of each other, which was the pre-fix shape).
     */
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.ink,
                )
            }
            Text(
                text = title,
                style = typography.sansHeader.copy(color = colors.ink),
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (trailing != null) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        content()
    }
}

/**
 * Token-styled text action for headers and rows ("Clear", "Delete", "+ New").
 * Tap target is wider than the visible text via padding.
 */
@Composable
internal fun ScaffoldTextAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val tint = when {
        !enabled -> colors.inkSubtle
        isDestructive -> colors.error
        else -> colors.accent
    }
    Text(
        text = label,
        style = typography.sansSmall.copy(color = tint),
        modifier = Modifier
            .clip(UndercurrentTheme.shapes.xsmall)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

/**
 * Horizontal hairline divider in our token color. Used between list rows
 * and section blocks. Skip [Spacer] padding around it — let neighbours add
 * their own breathing room.
 */
@Composable
internal fun TokenDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(UndercurrentTheme.colors.divider),
    )
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = UndercurrentTheme.typography.sansLabel.copy(
            color = UndercurrentTheme.colors.inkSubtle,
        ),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
