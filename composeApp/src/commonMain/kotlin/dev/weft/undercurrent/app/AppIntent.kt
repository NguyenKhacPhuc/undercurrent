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
public sealed interface AppIntent {
    /** Boot path: look up a stored key, build the agent, hydrate the conversation. */
    public data object Resume : AppIntent

    /** Build the agent for the freshly-pasted key and navigate to Chat. */
    public data class SubmitKey(public val key: String) : AppIntent

    /** Plain navigation. Used for back-button paths that don't touch the agent. */
    public data class Navigate(public val screen: Screen) : AppIntent

    public data class SelectConversation(public val id: String) : AppIntent

    public data object NewChat : AppIntent

    public data object DeleteCurrentConversation : AppIntent

    public data class DeleteConversation(public val id: String) : AppIntent

    /**
     * Send a chat message. Routes through skill resolution first;
     * non-skill input falls through to the agent's streaming flow.
     */
    public data class SendChat(
        public val text: String,
        public val modelTier: ModelTier? = null,
    ) : AppIntent

    /** "Ask again" — re-send the last user message. */
    public data object RegenerateLast : AppIntent

    /**
     * Redact + save + share a trace JSON by id. AppStore re-resolves
     * the full trace from the runtime trace store at write time.
     */
    public data class ExportTrace(public val traceId: String) : AppIntent

    /**
     * Forwarded from the UI bridge's `renderEvents` flow. The Android
     * impl wires this; iOS no-ops.
     */
    public data class UiBridgeUpdate(public val event: UiRenderEvent?) : AppIntent

    public data class SetPalette(public val palette: AppPalette) : AppIntent

    public data class SetThemeMode(public val mode: ThemeMode) : AppIntent

    public data object CompleteOnboarding : AppIntent

    public data object DismissPermissionDialog : AppIntent

    /** Switch the active agent. */
    public data class SelectAgent(public val name: String) : AppIntent

    /**
     * Invoke a saved mini-app. The intent carries both the trigger
     * prompt (re-dispatched through the agent loop, refreshing whatever
     * data the UI reads) and the cached render-tree JSON if any. The
     * Android impl seeds the UI bridge with the cached tree synchronously
     * so the user sees the tracker UI instantly while the agent's reply
     * streams in. iOS impl ignores the cached tree.
     */
    public data class InvokeMiniApp(
        public val miniAppId: String,
        public val triggerPrompt: String,
        public val cachedRenderTreeJson: String? = null,
    ) : AppIntent

    public data class SetProvider(public val provider: ProviderKind) : AppIntent

    public data class SaveProviderKey(
        public val provider: ProviderKind,
        public val apiKey: String,
    ) : AppIntent

    public data class RemoveProviderKey(public val provider: ProviderKind) : AppIntent

    public data class SetDefaultTier(public val tier: ModelTier?) : AppIntent

    /** Begin a guided creator flow. */
    public data class StartCreator(public val kind: CreatorKind) : AppIntent

    public data object CancelCreator : AppIntent

    /** Set the model that fills a specific (provider, tier) slot. */
    public data class SetModelForTier(
        public val provider: ProviderKind,
        public val tier: ModelTier,
        public val modelId: String?,
    ) : AppIntent
}
