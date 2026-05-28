package dev.weft.undercurrent.app

import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.shared.gateway.UiRenderEvent

/**
 * Intents accepted by [AppStore.dispatch]. Fire-and-forget — code paths
 * that need to await a reply call the suspend helpers on AppStore.
 *
 * KMP — commonMain. Uses mirror types only ([ProviderKind] / [ModelTier] /
 * [UiRenderEvent]); the Android impl translates to Weft enums at the
 * runtime boundary.
 */
sealed interface AppIntent {
    /** Boot path: look up a stored key, build the agent, hydrate the conversation. */
    data object Resume : AppIntent

    /** Build the agent for the freshly-pasted key and navigate to Chat. */
    data class SubmitKey(val key: String) : AppIntent

    /** Plain navigation. Used for back-button paths that don't touch the agent. */
    data class Navigate(val screen: Screen) : AppIntent

    data class SelectConversation(val id: String) : AppIntent

    data object NewChat : AppIntent

    data object DeleteCurrentConversation : AppIntent

    data class DeleteConversation(val id: String) : AppIntent

    /**
     * Send a chat message. Routes through skill resolution first;
     * non-skill input falls through to the agent's streaming flow.
     */
    data class SendChat(
        val text: String,
        val modelTier: ModelTier? = null,
    ) : AppIntent

    /** "Ask again" — re-send the last user message. */
    data object RegenerateLast : AppIntent

    /**
     * Redact + save + share a trace JSON by id. AppStore re-resolves
     * the full trace from the runtime trace store at write time.
     */
    data class ExportTrace(val traceId: String) : AppIntent

    /**
     * Forwarded from the UI bridge's `renderEvents` flow. The Android
     * impl wires this; iOS no-ops.
     */
    data class UiBridgeUpdate(val event: UiRenderEvent?) : AppIntent

    data class SetPalette(val palette: AppPalette) : AppIntent

    data class SetThemeMode(val mode: ThemeMode) : AppIntent

    data object CompleteOnboarding : AppIntent

    data object DismissPermissionDialog : AppIntent

    /** Switch the active agent. */
    data class SelectAgent(val name: String) : AppIntent

    /**
     * Invoke a saved mini-app. The intent carries both the trigger
     * prompt (re-dispatched through the agent loop, refreshing whatever
     * data the UI reads) and the cached render-tree JSON if any. The
     * Android impl seeds the UI bridge with the cached tree synchronously
     * so the user sees the tracker UI instantly while the agent's reply
     * streams in. iOS impl ignores the cached tree.
     */
    data class InvokeMiniApp(
        val miniAppId: String,
        val triggerPrompt: String,
        val cachedRenderTreeJson: String? = null,
    ) : AppIntent

    data class SetProvider(val provider: ProviderKind) : AppIntent

    data class SaveProviderKey(
        val provider: ProviderKind,
        val apiKey: String,
    ) : AppIntent

    data class RemoveProviderKey(val provider: ProviderKind) : AppIntent

    data class SetDefaultTier(val tier: ModelTier?) : AppIntent

    /** Begin a guided creator flow. */
    data class StartCreator(val kind: CreatorKind) : AppIntent

    data object CancelCreator : AppIntent

    /** Set the model that fills a specific (provider, tier) slot. */
    data class SetModelForTier(
        val provider: ProviderKind,
        val tier: ModelTier,
        val modelId: String?,
    ) : AppIntent
}
