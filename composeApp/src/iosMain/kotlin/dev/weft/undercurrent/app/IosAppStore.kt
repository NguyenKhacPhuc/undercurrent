package dev.weft.undercurrent.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.ThemePrefs
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.DisplayMessage
import dev.weft.undercurrent.feature.chat.SkillSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS placeholder [AppStore]. In-memory state only — survives within a
 * session but resets on app restart. Implements just enough of the
 * dispatch surface that the user can navigate end-to-end through the
 * app: Onboarding → KeyPaste → Chat (placeholder) → drawer items.
 *
 * Intents that need a real agent runtime (SendChat, RegenerateLast,
 * SelectAgent across rebuilds, ExportTrace, …) still no-op. The chat
 * surface renders the "Chat — coming to iOS" placeholder from
 * iosPlatformAdapter, so the no-op is visible / honest.
 *
 * Replace with a real implementation when the iOS-side agent runtime
 * lands (Phase 1 in docs/kmp-migration-status.md).
 */
public class IosAppStore : AppStore {
    private val _state = MutableStateFlow(AppState.initial())
    override val state: StateFlow<AppState> = _state.asStateFlow()
    override val effects: Flow<AppEffect> = emptyFlow()
    override val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()
    override val skills: List<SkillSummary> = emptyList()

    override fun dispatch(intent: AppIntent) {
        when (intent) {
            // Boot: land on Onboarding so the user sees real UI on
            // first frame.
            AppIntent.Resume -> _state.update {
                it.copy(screen = Screen.Onboarding)
            }

            // ── Onboarding handshake ────────────────────────────────
            // The provider picker on the last onboarding step dispatches
            // SetProvider + CompleteOnboarding. Both need to actually
            // mutate state or the user is stuck tapping "Let's go".
            is AppIntent.SetProvider -> _state.update {
                it.copy(activeProvider = intent.provider)
            }
            AppIntent.CompleteOnboarding -> _state.update {
                it.copy(
                    onboardingCompleted = true,
                    screen = Screen.KeyPaste,
                )
            }
            // KeyPaste's "Next" pretends the key was accepted, sets
            // agentReady so the Chat screen mounts (and shows the
            // "Chat — coming to iOS" placeholder via PlatformAdapter).
            is AppIntent.SubmitKey -> _state.update {
                it.copy(agentReady = true, screen = Screen.Chat)
            }

            // ── Plain navigation + state mutations (no agent needed) ─
            is AppIntent.Navigate -> _state.update {
                if (it.screen == intent.screen) it
                else it.copy(screen = intent.screen, previousScreen = it.screen)
            }
            is AppIntent.SetPalette -> _state.update {
                it.copy(themePrefs = it.themePrefs.copy(palette = intent.palette))
            }
            is AppIntent.SetThemeMode -> _state.update {
                it.copy(themePrefs = it.themePrefs.copy(mode = intent.mode))
            }
            is AppIntent.SetDefaultTier -> _state.update {
                it.copy(defaultTier = intent.tier)
            }
            is AppIntent.SelectAgent -> _state.update {
                it.copy(activeAgentName = intent.name)
            }
            AppIntent.DismissPermissionDialog -> _state.update {
                it.copy(pendingPermissionDialog = null)
            }

            // ── Stuff that legitimately needs an agent ──────────────
            // These no-op on iOS until a real agent runtime lands.
            // The UI surfaces a "coming to iOS" placeholder for chat
            // anyway, so the no-op is visible / honest.
            AppIntent.NewChat,
            AppIntent.DeleteCurrentConversation,
            AppIntent.RegenerateLast,
            AppIntent.CancelCreator,
            is AppIntent.SelectConversation,
            is AppIntent.DeleteConversation,
            is AppIntent.SendChat,
            is AppIntent.ExportTrace,
            is AppIntent.UiBridgeUpdate,
            is AppIntent.InvokeMiniApp,
            is AppIntent.SaveProviderKey,
            is AppIntent.RemoveProviderKey,
            is AppIntent.SetModelForTier,
            is AppIntent.StartCreator -> Unit
        }
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        // No-op until a real iOS agent lands.
    }

    override suspend fun saveKey(key: String) {
        // No-op until the iOS Keychain `KeyVaultGateway` lands.
        // SubmitKey will still flip agentReady so the user advances
        // past KeyPaste into the Chat placeholder.
    }
}

private fun MutableStateFlow<AppState>.update(reducer: (AppState) -> AppState) {
    value = reducer(value)
}
