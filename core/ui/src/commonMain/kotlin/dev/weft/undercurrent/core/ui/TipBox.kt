package dev.weft.undercurrent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme

/**
 * "Explain this control" callout. Muted surface with a thin accent bar
 * on the left and one or two stacked text blocks (optional title +
 * body). Used to teach unfamiliar concepts inline.
 *
 * KMP — commonMain. Moved from `app/.../ui/TipBox.kt`. Imports
 * `UndercurrentTheme` from `:core:design-system`.
 */
@Composable
fun TipBox(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shapes.small)
            .background(colors.surfaceMuted),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(colors.accent),
        )
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    style = typography.sansLabel.copy(color = colors.inkSubtle),
                )
            }
            Text(
                text = text,
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
    }
}
