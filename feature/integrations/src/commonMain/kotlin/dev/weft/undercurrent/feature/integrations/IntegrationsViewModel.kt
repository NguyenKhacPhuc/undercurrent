package dev.weft.undercurrent.feature.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.OAuthResult
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
 *  1. **Status** — derives per-integration enabled state from
 *     [IntegrationsRepository.enabledIdsFlow].
 *  2. **OAuth flow** — calls [OAuthGateway.authorize] on Connect.
 *     Suspends across Custom Tabs; on success, persists tokens via
 *     [OAuthGateway.putTokens] and flips the repo flag.
 *  3. **Pending-restart flag** — flips when the enabled set diverges
 *     from what the runtime was built with; the screen surfaces a
 *     "Restart Undercurrent to apply" banner.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/integrations/IntegrationsViewModel.kt`. Now
 * consumes [OAuthGateway] (was Weft's `OAuthClient` + `OAuthTokenStore`).
 */
public class IntegrationsViewModel(
    private val repo: IntegrationsRepository,
    private val oauth: OAuthGateway,
    /**
     * The set of integration ids the *currently running* WeftRuntime
     * was built with. Captured at boot — comparing against the live
     * enabled set tells us whether a restart is needed.
     */
    private val initialEnabled: Set<String>,
) : ViewModel() {

    public val enabledIds: StateFlow<Set<String>> = repo.enabledIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialEnabled)

    public val pendingRestart: StateFlow<Boolean> = enabledIds
        .map { it != initialEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _lastAction = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    public val lastAction: StateFlow<ActionStatus> = _lastAction.asStateFlow()

    public fun statusFor(integration: Integration, enabled: Set<String>): IntegrationStatus =
        if (integration.id in enabled) IntegrationStatus.Connected
        else IntegrationStatus.Disconnected

    public fun connect(integration: Integration) {
        viewModelScope.launch {
            _lastAction.value = ActionStatus.InProgress(integration.id, "Connecting…")
            val result = oauth.authorize(integration.oauth)
            _lastAction.value = when (result) {
                is OAuthResult.Success -> {
                    oauth.putTokens(integration.id, result.tokens)
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
                    ActionStatus.Failure(integration.id, "State mismatch — try again.")
            }
        }
    }

    public fun disconnect(integration: Integration) {
        viewModelScope.launch {
            _lastAction.value = ActionStatus.InProgress(integration.id, "Disconnecting…")
            oauth.removeTokens(integration.id)
            repo.setEnabled(integration.id, enabled = false)
            _lastAction.value = ActionStatus.Success(integration.id, "Disconnected")
        }
    }

    public fun clearLastAction() {
        _lastAction.value = ActionStatus.Idle
    }
}

public enum class IntegrationStatus { Disconnected, Connected }

public sealed interface ActionStatus {
    public data object Idle : ActionStatus
    public data class InProgress(val integrationId: String, val message: String) : ActionStatus
    public data class Success(val integrationId: String, val message: String) : ActionStatus
    public data class Failure(val integrationId: String, val message: String) : ActionStatus
}
