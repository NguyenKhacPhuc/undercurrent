package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.chat_empty_greeting
import dev.weft.undercurrent.core.resources.chat_empty_subtitle
import dev.weft.undercurrent.core.ui.components.undercurrentIcon
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A warm centered greeting shown on a fresh thread (no messages yet),
 * instead of a cold blank pane. A soft accent mark + a friendly hello so
 * the first impression invites a first message.
 */
@Composable
internal fun ChatEmptyState() {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = undercurrentIcon("bolt"),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(Res.string.chat_empty_greeting),
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.chat_empty_subtitle),
            style = typography.serifBody.copy(
                color = colors.inkMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun ChatEmptyStatePreview() {
    UndercurrentTheme {
        ChatEmptyState()
    }
}
