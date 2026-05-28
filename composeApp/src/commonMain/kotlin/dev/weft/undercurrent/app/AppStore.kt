package dev.weft.undercurrent.app

import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.feature.chat.DisplayMessage
import dev.weft.undercurrent.feature.chat.SkillSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Root MVI surface consumed by the App composable. The Android impl
 * (`WeftAppStore` in `:androidApp`) closes over `WeftRuntime` directly;
 * the iOS impl (`StubAppStore` in `:composeApp/iosMain`) emits a fixed
 * loading state until iOS gets a real agent runtime.
 *
 * KMP — commonMain. The interface deliberately exposes only mirror
 * types ([ProviderKind] / [ModelTier] / [SkillSummary] / [DisplayMessage])
 * so the App composable depends on nothing platform-specific.
 *
 * `displayMessages` is a `SnapshotStateList` so streaming chunks can
 * append directly — Compose recomposes the chat surface incrementally
 * without rebuilding the immutable state. Same trade-off as the
 * pre-KMP shape.
 */
public interface AppStore {

    public val state: StateFlow<AppState>

    /** One-shot side effects (errors). Collected via LaunchedEffect. */
    public val effects: Flow<AppEffect>

    /**
     * The streaming chat history. Kept outside [state] because mutations
     * are append-heavy and the surface re-reads continuously.
     */
    public val displayMessages: SnapshotStateList<DisplayMessage>

    /**
     * Registered skills, projected to the mirror DTO. Drives the chat
     * input's `[+]` quick-actions menu.
     */
    public val skills: List<SkillSummary>

    public fun dispatch(intent: AppIntent)

    /**
     * Fire a user-initiated event from the rendered-tree screen
     * (button tap, form submit). Suspend because the call goes
     * through the agent loop on the Android impl; the iOS stub
     * returns immediately.
     */
    public suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    )

    /** Persist a freshly-pasted API key against the active provider. */
    public suspend fun saveKey(key: String)
}
