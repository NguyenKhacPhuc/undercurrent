package dev.weft.undercurrent.feature.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.creator_subtitle
import dev.weft.undercurrent.core.resources.creator_thinking
import dev.weft.undercurrent.core.resources.creator_title_fallback
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreatorScreen(
    kind: CreatorKind?,
    isThinking: Boolean,
    inFlight: Boolean,
    hasTree: Boolean,
    lastError: String?,
    onCancel: () -> Unit,
    body: @Composable () -> Unit,
) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = "×",
                style = tp.sansHeader.copy(
                    color = cs.inkMuted,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier
                    .clickable(enabled = !inFlight, onClick = onCancel)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = kind?.screenTitle ?: stringResource(Res.string.creator_title_fallback),
                    style = tp.sansHeader.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                Text(
                    text = stringResource(Res.string.creator_subtitle),
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                )
            }
            if (isThinking || inFlight) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = cs.accent,
                    modifier = Modifier.padding(end = 12.dp).size(18.dp),
                )
            }
        }
        HorizontalDivider(color = cs.divider)

        if (hasTree) {
            body()
        } else {
            CreatorThinking(error = lastError)
        }
    }
}

@Composable
private fun CreatorThinking(error: String?) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = cs.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(Res.string.creator_thinking),
                style = tp.sansLabel.copy(fontSize = 14.sp),
                color = cs.inkMuted,
            )
            if (error != null) {
                Text(
                    text = error,
                    style = tp.sansSmall,
                    color = cs.error,
                )
            }
        }
    }
}
