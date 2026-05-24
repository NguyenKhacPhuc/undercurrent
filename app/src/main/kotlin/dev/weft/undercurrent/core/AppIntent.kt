package dev.weft.undercurrent.core

import dev.weft.contracts.ProviderKind
import dev.weft.contracts.UIUpdate
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.harness.observability.AgentTrace
import dev.weft.undercurrent.theme.AppPalette
import dev.weft.undercurrent.theme.ThemeMode

/**
 * Intents accepted by [AppStore.dispatch]. Fire-and-forget — for code paths
 * that need to await a reply (the suspend `onAction` callback of
 * [dev.weft.compose.components.AgentRenderedTreeScreen], or persisting a
 * pasted API key), call the suspend helpers on [AppStore] directly instead.
 */
internal sealed interface AppIntent {
    /** Boot path: look up a stored key, build the agent, hydrate the most-recent conversation. */
    data object Resume : AppIntent

    /** Key paste flow: build the agent for the freshly-pasted key and navigate to Chat. */
    data class SubmitKey(val key: String) : AppIntent

    /** Plain navigation. Used for back-button paths that don't touch the agent. */
    data class Navigate(val screen: Screen) : AppIntent

    /** Switch the active conversation, re-hydrate the message list, navigate to Chat. */
    data class SelectConversation(val id: String) : AppIntent

    /** Start a fresh conversation on the current agent, clear the message list. */
    data object NewChat : AppIntent

    /**
     * Delete the conversation currently in view. After deletion the agent
     * starts a fresh conversation (same as [NewChat]) so the user isn't
     * stranded looking at a thread that no longer exists in the store.
     *
     * No-op when the agent isn't initialized yet (boot path) — the chat
     * surface that dispatches this intent is unreachable until the agent
     * is up.
     */
    data object DeleteCurrentConversation : AppIntent

    /**
     * Delete an arbitrary conversation by id. Used by the drawer's
     * long-press → confirm flow where the user picks which thread to
     * delete. If [id] happens to match the currently-active conversation,
     * the chat surface resets (agent starts a fresh chat, displayMessages
     * cleared) — same outcome as [DeleteCurrentConversation]. If it's an
     * inactive thread, the active one is untouched.
     */
    data class DeleteConversation(val id: String) : AppIntent

    /**
     * Send a chat message. Routes through skill resolution first; non-skill
     * input falls through to the agent's streaming flow, whose chunks the
     * store reduces into [AppStore.displayMessages] and [ChatStatus].
     *
     * [modelTier] is a per-call override of the model router (per
     * [dev.weft.harness.agents.routing.ModelTier]). `null` means "use the
     * default tier pref from Settings, or fall back to the router's
     * automatic heuristic if no default is set."
     */
    data class SendChat(
        val text: String,
        val modelTier: ModelTier? = null,
    ) : AppIntent

    /**
     * Re-send the last user message. "Ask again" semantics — the previous
     * assistant reply stays in the history; a fresh reply streams in below
     * it. Does not roll back the conversation store. No-op if there's no
     * last user message or a send is already in flight.
     */
    data object RegenerateLast : AppIntent

    /** Redact + save + share a trace JSON. Emits [AppEffect.Error] on failure. */
    data class ExportTrace(val trace: AgentTrace) : AppIntent

    /**
     * Forwarded from the App composable's `snapshotFlow { uiBridge.lastUpdate }`.
     * The store reacts to [UIUpdate.RenderTree] by navigating to [Screen.RenderedTree].
     */
    data class UiBridgeUpdate(val update: UIUpdate?) : AppIntent

    /** Pick a new color palette. Persists via [theme.ThemeRepository]. */
    data class SetPalette(val palette: AppPalette) : AppIntent

    /** Pick Auto / Light / Dark. Persists via [theme.ThemeRepository]. */
    data class SetThemeMode(val mode: ThemeMode) : AppIntent

    /**
     * Mark the first-launch onboarding as finished. Persists via
     * [onboarding.OnboardingRepository], then the boot path lands the user
     * on [Screen.KeyPaste].
     */
    data object CompleteOnboarding : AppIntent

    /** Dismiss the "permission needed" AlertDialog without opening Settings. */
    data object DismissPermissionDialog : AppIntent

    /**
     * Switch the active agent. The chat surface's selector dispatches
     * this; AppStore validates the name against [AppState.availableAgents],
     * updates [AppState.activeAgentName], and rebuilds the [WeftAgent]
     * via the named overload so the next turn carries the new
     * declaration's tool allowlist + system fragment + strategy.
     */
    data class SelectAgent(val name: String) : AppIntent

    /**
     * Invoke a saved feature — dispatches the feature's trigger prompt
     * through the agent loop and (separately) caches whatever
     * `ui_render` payload the agent emits so the next invocation can
     * show the UI instantly. Persists usage count too.
     *
     * The cached-tree seeding happens in the UI layer (MainActivity)
     * because that's where the [dev.weft.compose.ComposeUiBridge]
     * lives. AppStore only handles the agent-turn side.
     */
    data class InvokeSavedFeature(
        val featureId: String,
        val triggerPrompt: String,
    ) : AppIntent

    // Persona intents (SetActivePersona / AddCustomPersona /
    // DeleteCustomPersona) moved to dev.weft.undercurrent.features.personas.PersonasViewModel
    // — the Personas screen was their only caller, and PersonasViewModel
    // talks to PersonaRepository directly. The runtime's
    // `extraVolatilePrefix` lambda still reads from the same repo
    // singleton, so the agent picks up persona changes on the next turn.

    /**
     * Persist a new LLM provider. If a key is already stored for that
     * provider, AppStore also rebuilds the agent so the next send uses
     * the new backend. If no key is stored, the user lands on a paste UI
     * (handled in Settings — no separate route).
     */
    data class SetProvider(val provider: ProviderKind) : AppIntent

    /** Save an API key for a specific provider (key-vault write). */
    data class SaveProviderKey(val provider: ProviderKind, val apiKey: String) : AppIntent

    /**
     * Delete a provider's stored key from the vault. If [provider] matches
     * the active one, AppStore also drops the agent and routes the user
     * back to [Screen.KeyPaste]; otherwise this is silent (the inactive
     * row just shows "No key set" on next read).
     */
    data class RemoveProviderKey(val provider: ProviderKind) : AppIntent

    /** Pick the default model tier ([null] = router decides). */
    data class SetDefaultTier(val tier: ModelTier?) : AppIntent

    /**
     * Set the model that fills a specific (provider, tier) slot in the
     * runtime's ModelPool. Passing [modelId] = null clears the override
     * and falls back to the provider's default for that slot. If
     * [provider] is the active one, AppStore rebuilds the agent so the
     * next send uses the new model.
     */
    data class SetModelForTier(
        val provider: ProviderKind,
        val tier: ModelTier,
        val modelId: String?,
    ) : AppIntent
}
