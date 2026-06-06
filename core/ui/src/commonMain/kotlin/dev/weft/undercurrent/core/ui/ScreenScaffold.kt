package dev.weft.undercurrent.core.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme

/**
 * Shared chrome for back-navigable secondary screens (Settings,
 * Conversations, Memories, Traces). Token-styled top bar with back
 * affordance + title + optional trailing slot, a hairline divider,
 * and the content area.
 *
 * KMP — commonMain. Moved from `app/.../ui/ScreenScaffold.kt`.
 * The back affordance uses a Unicode "←" character instead of
 * `androidx.compose.material.icons.AutoMirrored.Filled.ArrowBack` —
 * the icons-extended dependency isn't pulled into CMP modules by
 * default, and a Unicode glyph styled with `typography.sansHeader`
 * matches the rest of the screen's visual language well enough that
 * a real icon isn't worth the dep weight here. Apps with custom
 * material-icons-extended on the CMP path can wrap their own.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            Text(
                text = "←",
                style = typography.sansHeader.copy(color = colors.ink),
                modifier = Modifier
                    .clip(UndercurrentTheme.shapes.xsmall)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
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
 * Token-styled text action for headers and rows ("Clear", "Delete",
 * "+ New"). Tap target is wider than the visible text via padding.
 */
@Composable
fun ScaffoldTextAction(
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
 * Horizontal hairline divider in our token color. Used between list
 * rows and section blocks.
 */
@Composable
fun TokenDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(UndercurrentTheme.colors.divider),
    )
}

/** Section header. ALL CAPS + small label typography. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = UndercurrentTheme.typography.sansLabel.copy(
            color = UndercurrentTheme.colors.inkSubtle,
        ),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
