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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme

/**
 * Dedicated wizard surface for a [CreatorSession]. Unlike the chat
 * screen, there's no free-form text input — the user answers ONLY
 * through the widgets the agent has rendered via `ui_render`.
 *
 * Visual states:
 *   1. **Thinking, no tree yet** — kickoff turn in flight. Centered
 *      spinner + "Preparing the first question…".
 *   2. **Question rendered** — host's [body] Composable renders the
 *      agent-emitted tree and accumulates field state. The screen
 *      provides the chrome (cancel, header, busy indicator).
 *   3. **Subsequent thinking** — inline spinner in the header while
 *      the next agent turn runs.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/creator/CreatorScreen.kt`. Adjustments:
 *   - `dev.weft.compose.ComposeUiBridge` + `TreeRenderer` +
 *     `WeftComponentRegistry` (all Android-only) hoisted out of the
 *     screen. The host passes the tree-rendering [body] Composable.
 *   - `ComponentEvent` (Weft contract) stays inside [body] — the
 *     host adapts events into the host's field-accumulator and
 *     action dispatch.
 *   - `Icons.Filled.Close` → Unicode "×" (same approach as
 *     `ScreenScaffold`'s "←").
 *   - Theme imports from `:core:design-system`.
 *
 * @param hasTree set by the host when the agent has emitted a
 *   render-tree event. False = show the thinking placeholder; true
 *   = render [body].
 */
@Composable
public fun CreatorScreen(
    creatorSession: CreatorSession,
    isThinking: Boolean,
    inFlight: Boolean,
    hasTree: Boolean,
    lastError: String?,
    onCancel: () -> Unit,
    body: @Composable () -> Unit,
) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography
    val kind by creatorSession.state.collectAsState()

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
                    text = kind?.screenTitle ?: "Creator",
                    style = tp.sansHeader.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                Text(
                    text = "Walk through the questions to finish.",
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
                text = "Preparing the first question…",
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
