package dev.weft.undercurrent.feature.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(22.dp)
            .background(UndercurrentTheme.colors.ink.copy(alpha = alpha)),
    )
}

@Preview
@Composable
private fun BlinkingCursorPreview() {
    UndercurrentTheme {
        BlinkingCursor()
    }
}
