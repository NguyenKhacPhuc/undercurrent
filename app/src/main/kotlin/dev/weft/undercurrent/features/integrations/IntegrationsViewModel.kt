package dev.weft.undercurrent.features.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.oauth.OAuthClient
import dev.weft.oauth.OAuthResult
import dev.weft.oauth.OAuthTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the Integrations screen. Owns three concerns:
 *
 *  1. **Status** — derives the per-integration enabled/disabled state from
 *     [IntegrationsRepository.enabledIdsFlow]. Surfaced to the UI as
 *     [statusFor].
 *  2. **OAuth flow** — calls into the substrate's [OAuthClient.authorize]
 *     on Connect. Suspends across Custom Tabs; on success, persists the
 *     token bundle via [OAuthTokenStore] and marks the integration
 *     enabled in the repo.
 *  3. **Pending-changes flag** — flips [pendingRestart] whenever the
 *     enabled set diverges from what the WeftRuntime was built with.
 *     The Integrations screen shows a "Restart Undercurrent to apply"
 *     banner while this is true.
 */
internal class IntegrationsViewModel(
    private val repo: IntegrationsRepository,
    private val oauthClient: OAuthClient,
    private val tokenStore: OAuthTokenStore,
    /**
     * The set of integration ids the *currently running* WeftRuntime was
     * built with. Captured at boot and held constant for this VM's
     * lifetime — comparing it against the live enabled set is how we
     * know whether a restart is needed.
     */
    private val initialEnabled: Set<String>,
) : ViewModel() {

    /** Live enabled set — flips immediately when Connect/Disconnect persists. */
    val enabledIds: StateFlow<Set<String>> = repo.enabledIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialEnabled)

    /**
     * True when the live enabled set differs from the runtime's. The
     * screen surfaces a restart banner while this is true; tapping it
     * runs [requestRestart] (provided by the host MainActivity since
     * killing the process is a UI-layer concern).
     */
    val pendingRestart: StateFlow<Boolean> = enabledIds
        .map { it != initialEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Transient per-action status. Connect/Disconnect set this to
     * Connecting/Disconnecting briefly; OAuth failures land here for
     * the UI to surface inline. Resets to Idle once the screen reads it.
     */
    private val _lastAction = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val lastAction: StateFlow<ActionStatus> = _lastAction.asStateFlow()

    /**
     * Resolve the surface-level status of one integration. Connected
     * means tokens are persisted AND the user has the integration toggled
     * on; the two could diverge if a disconnect failed to clear tokens
     * (rare but possible), so we treat the repo flag as authoritative.
     */
    fun statusFor(integration: Integration, enabled: Set<String>): IntegrationStatus =
        if (integration.id in enabled) IntegrationStatus.Connected
        else IntegrationStatus.Disconnected

    /**
     * Kick off the OAuth flow for [integration]. Suspends across Custom
     * Tabs — the user-facing UI thread should call this from a
     * `rememberCoroutineScope().launch { … }` so the screen stays
     * responsive while the browser is up.
     */
    fun connect(integration: Integration) {
        viewModelScope.launch {
            _lastAction.value = ActionStatus.InProgress(integration.id, "Connecting…")
            val result = oauthClient.authorize(integration.oauth)
            _lastAction.value = when (result) {
                is OAuthResult.Success -> {
                    tokenStore.put(integration.id, result.tokens)
                    repo.setEnabled(integration.id, enabled = true)
                    ActionStatus.Success(integration.id, "Connected")
                }
                is OAuthResult.UserCancelled ->
                    ActionStatus.Failure(integration.id, "Cancelled.")
                is OAuthResult.ProviderError ->
                    ActionStatus.Failure(
                        integration.id,
                        "${result.code}${result.description?.let { ": $it" } ?: ""}",
                    )
                is OAuthResult.TransportError ->
                    ActionStatus.Failure(integration.id, result.message)
                is OAuthResult.StateMismatch ->
                    // CSRF / stale callback — never trust the tokens.
                    ActionStatus.Failure(integration.id, "State mismatch — try again.")
            }
        }
    }

    /**
     * Forget tokens and mark the integration disabled. We clear tokens
     * first so a crash between the two writes doesn't leave a stale
     * "enabled but tokenless" state.
     */
    fun disconnect(integration: Integration) {
        viewModelScope.launch {
            _lastAction.value = ActionStatus.InProgress(integration.id, "Disconnecting…")
            tokenStore.remove(integration.id)
            repo.setEnabled(integration.id, enabled = false)
            _lastAction.value = ActionStatus.Success(integration.id, "Disconnected")
        }
    }

    fun clearLastAction() {
        _lastAction.value = ActionStatus.Idle
    }
}

internal enum class IntegrationStatus { Disconnected, Connected }

/** UI-layer status for the last Connect/Disconnect attempt. */
internal sealed interface ActionStatus {
    data object Idle : ActionStatus
    data class InProgress(val integrationId: String, val message: String) : ActionStatus
    data class Success(val integrationId: String, val message: String) : ActionStatus
    data class Failure(val integrationId: String, val message: String) : ActionStatus
}
