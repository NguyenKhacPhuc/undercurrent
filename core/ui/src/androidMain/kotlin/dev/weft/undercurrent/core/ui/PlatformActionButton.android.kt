package dev.weft.undercurrent.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Android rendering: a filled Material 3 [Button] — ripple, rounded
 * container, the platform's tactile press feedback.
 */
@Composable
actual fun PlatformActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = UndercurrentTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.divider,
            disabledContentColor = colors.inkSubtle,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Text(
            text = label,
            style = typography.sansLabel,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}

@Preview
@Composable
private fun PlatformActionButtonPreview() {
    UndercurrentTheme {
        PlatformActionButton(label = "Continue", onClick = {})
    }
}

@Preview
@Composable
private fun PlatformActionButtonPreviewDisabled() {
    UndercurrentTheme {
        PlatformActionButton(label = "Continue", onClick = {}, enabled = false)
    }
}
