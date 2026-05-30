package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.feature.voice.WaveformBars
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun InputRow(
    inputText: String,
    onInputChange: (String) -> Unit,
    inFlight: Boolean,
    inputFocus: FocusRequester,
    showAddToChat: Boolean,
    onOpenAddToChat: () -> Unit,
    onSend: () -> Unit,
    voiceAvailable: Boolean,
    isRecording: Boolean,
    voiceRms: StateFlow<Float>,
    onMicPress: () -> Boolean,
    onMicRelease: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAddToChat) {
            AddToChatButton(onClick = onOpenAddToChat, enabled = !inFlight)
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shapes.medium)
                .border(
                    width = 1.dp,
                    color = if (isRecording) colors.accent else colors.divider,
                    shape = shapes.medium,
                )
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocus),
                textStyle = typography.serifBody.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                enabled = !inFlight,
            )
            if (inputText.isEmpty()) {
                if (isRecording) {
                    WaveformBars(rms = voiceRms)
                } else {
                    Text(
                        text = "Write a paragraph back…",
                        style = typography.serifBody.copy(color = colors.inkSubtle),
                    )
                }
            }
        }

        if (voiceAvailable) {
            Spacer(Modifier.width(4.dp))
            MicButton(
                isRecording = isRecording,
                enabled = !inFlight,
                onMicPress = onMicPress,
                onMicRelease = onMicRelease,
            )
        }

        Spacer(Modifier.width(4.dp))

        val canSend = inputText.isNotBlank() && !inFlight
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shapes.medium)
                .background(colors.ink.copy(alpha = if (canSend) 1f else 0.35f))
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↑",
                style = typography.sansHeader.copy(
                    color = colors.background,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun AddToChatButton(onClick: () -> Unit, enabled: Boolean) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shapes.medium)
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            style = typography.sansHeader.copy(
                color = if (enabled) colors.ink else colors.inkSubtle,
                fontSize = 24.sp,
            ),
        )
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    enabled: Boolean,
    onMicPress: () -> Boolean,
    onMicRelease: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val shapes = UndercurrentTheme.shapes
    val typography = UndercurrentTheme.typography

    val bg = when {
        !enabled -> colors.surface
        isRecording -> colors.accent
        else -> colors.surface
    }
    val tint = when {
        !enabled -> colors.inkSubtle
        isRecording -> colors.onAccent
        else -> colors.inkMuted
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shapes.medium)
            .background(bg)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val started = onMicPress()
                        if (started) {
                            tryAwaitRelease()
                            onMicRelease()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "●",
            style = typography.sansHeader.copy(
                color = tint,
                fontSize = 18.sp,
            ),
        )
    }
}

@Preview
@Composable
private fun InputRowPreview() {
    UndercurrentTheme {
        InputRow(
            inputText = "",
            onInputChange = { },
            inFlight = false,
            inputFocus = remember { FocusRequester() },
            showAddToChat = true,
            onOpenAddToChat = { },
            onSend = { },
            voiceAvailable = true,
            isRecording = false,
            voiceRms = remember { MutableStateFlow(0f) },
            onMicPress = { true },
            onMicRelease = { },
        )
    }
}
