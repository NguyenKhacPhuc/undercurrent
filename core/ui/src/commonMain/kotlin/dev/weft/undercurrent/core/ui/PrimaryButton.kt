package dev.weft.undercurrent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(if (enabled) colors.accent else colors.divider)
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.6f)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.sansLabel.copy(color = colors.onAccent),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun PrimaryButtonPreview() {
    UndercurrentTheme {
        PrimaryButton(label = "Sign In", onClick = {})
    }
}

@Preview
@Composable
private fun PrimaryButtonPreviewDisabled() {
    UndercurrentTheme {
        PrimaryButton(label = "Sign In", onClick = {}, enabled = false)
    }
}
