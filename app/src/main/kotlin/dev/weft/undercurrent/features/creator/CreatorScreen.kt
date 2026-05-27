package dev.weft.undercurrent.features.creator

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.components.TreeRenderer
import dev.weft.compose.components.WeftComponentRegistry
import dev.weft.contracts.ComponentEvent
import dev.weft.contracts.UIUpdate
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.coroutines.launch

/**
 * Dedicated wizard surface for a [CreatorSession].
 *
 * Unlike the chat screen, there's no free-form text input — the user
 * answers ONLY through the widgets the agent has rendered via
 * `ui_render`. This enforces "must go through QnA sections" from the
 * settings entry points while still letting the agent generate dynamic
 * questions each turn.
 *
 * Visual states:
 *   1. **Thinking, no tree yet** — the kickoff turn is in flight. Shows
 *      a centered spinner + "Preparing the first question…".
 *   2. **Question rendered** — the agent has emitted a
 *      [UIUpdate.RenderTree]. The tree renders via the standard
 *      substrate [TreeRenderer]; Field / Toggle changes accumulate in
 *      [fieldValues]; Action events route to [onAction] with the full
 *      field-values map (same pattern as
 *      [dev.weft.compose.components.AgentRenderedTreeScreen]).
 *   3. **Subsequent thinking** — between turns, a small inline spinner
 *      sits below the tree so the user sees that their answer was
 *      received and the next question is coming.
 *
 * Cancel routes to [onCancel]; the agent's `create_persona` /
 * `create_mini_app` finalize tools handle dismissal on success
 * themselves (via [NavigationChannel]).
 */
@Composable
internal fun CreatorScreen(
    creatorSession: CreatorSession,
    uiBridge: ComposeUiBridge,
    componentRegistry: WeftComponentRegistry,
    isThinking: Boolean,
    lastError: String?,
    onAction: suspend (action: String, sourceLabel: String?, fieldValues: Map<String, String>) -> Unit,
    onCancel: () -> Unit,
) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography
    val kind by creatorSession.state.collectAsState()
    val update = uiBridge.lastUpdate
    val tree = (update as? UIUpdate.RenderTree)?.tree
    val scope = rememberCoroutineScope()
    // Reset field accumulator every time a new tree arrives so previous
    // answers don't leak into the next step's submission.
    val fieldValues = remember(tree) { mutableStateMapOf<String, String>() }
    var inFlight by remember { mutableStateOf(false) }

    val onEvent: (ComponentEvent) -> Unit = handler@{ event ->
        when (event) {
            is ComponentEvent.TextChanged -> {
                fieldValues[event.sourceId] = event.value
            }
            is ComponentEvent.ToggleChanged -> {
                fieldValues[event.sourceId] = event.value.toString()
            }
            is ComponentEvent.Action -> {
                if (inFlight) return@handler
                inFlight = true
                scope.launch {
                    try {
                        onAction(event.action, event.sourceLabel, fieldValues.toMap())
                    } finally {
                        inFlight = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Header — Cancel + title.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onCancel, enabled = !inFlight) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel",
                    tint = cs.inkMuted,
                )
            }
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

        // Body.
        if (tree != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TreeRenderer(tree = tree, registry = componentRegistry, onEvent = onEvent)
                if (lastError != null) {
                    Text(
                        text = lastError,
                        style = tp.sansSmall,
                        color = cs.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
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
