package dev.weft.undercurrent.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import dev.weft.undercurrent.core.resources.common_retry
import dev.weft.undercurrent.core.resources.prompt_setup_connecting
import dev.weft.undercurrent.core.resources.prompt_setup_failed
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The cold-start gate surface: a calm "getting set up" while the base prompt
 * is fetched, and a clear "couldn't connect — retry" if it can't. Blocks the
 * user from chatting until a prompt exists (no compiled-in fallback).
 */
@Composable
fun PromptSetupScreen(
    state: PromptSetupState,
    onRetry: () -> Unit = {},
) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().background(cs.background).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.phase) {
            PromptSetupPhase.Failed -> {
                Text(
                    text = stringResource(Res.string.prompt_setup_failed),
                    style = tp.serifBody.copy(color = cs.inkMuted, fontSize = 16.sp, lineHeight = 24.sp),
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .clip(UndercurrentTheme.shapes.medium)
                        .background(cs.accent)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.common_retry),
                        style = tp.sansLabel.copy(color = cs.onAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(cs.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = cs.accent, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = stringResource(Res.string.prompt_setup_connecting),
                    style = tp.sansLabel.copy(color = cs.inkMuted, fontSize = 14.sp),
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PromptSetupConnectingPreview() {
    UndercurrentTheme { PromptSetupScreen(PromptSetupState(PromptSetupPhase.Connecting)) }
}

@Preview
@Composable
private fun PromptSetupFailedPreview() {
    UndercurrentTheme { PromptSetupScreen(PromptSetupState(PromptSetupPhase.Failed)) }
}
