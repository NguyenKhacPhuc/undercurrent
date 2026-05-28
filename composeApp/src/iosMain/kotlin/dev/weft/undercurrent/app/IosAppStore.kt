package dev.weft.undercurrent.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.feature.chat.DisplayMessage
import dev.weft.undercurrent.feature.chat.SkillSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS placeholder [AppStore]. Emits a fixed loading state and ignores
 * every intent. Replace once iOS gets a real agent runtime (Weft Swift
 * client, or a different agent SDK on the iOS side).
 *
 * Until then, the iOS app boots cleanly, the App composable renders,
 * and every screen sits in its empty / loading state. That's enough
 * to exercise the screen wiring + theme + per-feature commonMain
 * Composable correctness without needing the agent backbone.
 */
public class IosAppStore : AppStore {
    private val _state = MutableStateFlow(AppState.initial())
    override val state: StateFlow<AppState> = _state.asStateFlow()
    override val effects: Flow<AppEffect> = emptyFlow()
    override val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()
    override val skills: List<SkillSummary> = emptyList()

    override fun dispatch(intent: AppIntent) {
        when (intent) {
            // Land on Onboarding so the user sees real UI immediately
            // instead of the loading placeholder. Once an iOS agent
            // runtime exists, this dispatches into the real boot path.
            AppIntent.Resume -> _state.value = _state.value.copy(
                screen = dev.weft.undercurrent.core.navigation.Screen.Onboarding,
            )
            // Plain navigation works without an agent — supports the
            // KMP-clean screens (Settings, Personas, etc.) on iOS.
            is AppIntent.Navigate -> _state.value = _state.value.copy(
                screen = intent.screen,
                previousScreen = _state.value.screen,
            )
            else -> {
                // Every other intent is a no-op until iOS gets a real
                // AppStore. Most need the agent runtime anyway
                // (SendChat / RegenerateLast / SelectAgent / …).
            }
        }
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        // No-op.
    }

    override suspend fun saveKey(key: String) {
        // No-op. iOS key-vault lands when the iOS agent does.
    }
}
