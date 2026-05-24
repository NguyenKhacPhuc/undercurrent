package dev.weft.undercurrent.ui

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
import dev.weft.undercurrent.theme.UndercurrentTheme

/**
 * Reusable "explain this control" callout. Renders a muted surface with
 * a thin accent bar on the left and one or two stacked text blocks
 * (optional uppercase title + body). Used to teach unfamiliar concepts
 * inline without forcing the user to leave the screen for a help page.
 *
 * Visual lean: same `surfaceMuted` background the existing token chips
 * and meta blocks use, plus the accent-colored left bar that signals
 * "this is informational, not interactive."
 *
 * Body text is fixed to [Typography.sansSmall] / inkMuted — tip boxes
 * are quiet supporting copy, not headers.
 */
@Composable
internal fun TipBox(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        // IntrinsicSize.Min lets the left accent bar stretch to whatever
        // height the text column ends up at — without it, fillMaxHeight()
        // on the bar collapses to 0 since the row's height is unbounded.
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
