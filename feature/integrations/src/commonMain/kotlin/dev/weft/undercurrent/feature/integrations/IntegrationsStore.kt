package dev.weft.undercurrent.feature.integrations

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.OAuthResult
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.launch

public data class IntegrationsState(
    public val enabledIds: Set<String> = emptySet(),
    public val pendingRestart: Boolean = false,
    public val lastAction: ActionStatus = ActionStatus.Idle,
)

public sealed interface IntegrationsIntent {
    public data class Connect(public val integration: Integration) : IntegrationsIntent
    public data class Disconnect(public val integration: Integration) : IntegrationsIntent
    public data object ClearLastAction : IntegrationsIntent
}

public sealed interface IntegrationsEffect

public class IntegrationsStore(
    private val repo: IntegrationsRepository,
    private val oauth: OAuthGateway,
    /**
     * The set of integration ids the *currently running* WeftRuntime was
     * built with. Captured at boot — comparing against the live enabled
     * set tells us whether a restart is needed.
     */
    private val initialEnabled: Set<String>,
) : Store<IntegrationsState, IntegrationsIntent, IntegrationsEffect>(
    initialState = IntegrationsState(enabledIds = initialEnabled),
) {
    init {
        viewModelScope.launch {
            repo.enabledIdsFlow.collect { ids ->
                update {
                    it.copy(
                        enabledIds = ids,
                        pendingRestart = ids != initialEnabled,
                    )
                }
            }
        }
    }

    public fun statusFor(integration: Integration, enabled: Set<String>): IntegrationStatus =
        if (integration.id in enabled) IntegrationStatus.Connected
        else IntegrationStatus.Disconnected

    override fun dispatch(intent: IntegrationsIntent) {
        when (intent) {
            is IntegrationsIntent.Connect -> viewModelScope.launch {
                update { it.copy(lastAction = ActionStatus.InProgress(intent.integration.id, "Connecting…")) }
                val result = oauth.authorize(intent.integration.oauth)
                val next = when (result) {
                    is OAuthResult.Success -> {
                        oauth.putTokens(intent.integration.id, result.tokens)
                        repo.setEnabled(intent.integration.id, enabled = true)
                        ActionStatus.Success(intent.integration.id, "Connected")
                    }
                    is OAuthResult.UserCancelled ->
                        ActionStatus.Failure(intent.integration.id, "Cancelled.")
                    is OAuthResult.ProviderError ->
                        ActionStatus.Failure(
                            intent.integration.id,
                            "${result.code}${result.description?.let { ": $it" } ?: ""}",
                        )
                    is OAuthResult.TransportError ->
                        ActionStatus.Failure(intent.integration.id, result.message)
                    is OAuthResult.StateMismatch ->
                        ActionStatus.Failure(intent.integration.id, "State mismatch — try again.")
                }
                update { it.copy(lastAction = next) }
            }
            is IntegrationsIntent.Disconnect -> viewModelScope.launch {
                update { it.copy(lastAction = ActionStatus.InProgress(intent.integration.id, "Disconnecting…")) }
                oauth.removeTokens(intent.integration.id)
                repo.setEnabled(intent.integration.id, enabled = false)
                update { it.copy(lastAction = ActionStatus.Success(intent.integration.id, "Disconnected")) }
            }
            IntegrationsIntent.ClearLastAction -> {
                update { it.copy(lastAction = ActionStatus.Idle) }
            }
        }
    }
}

public enum class IntegrationStatus { Disconnected, Connected }

public sealed interface ActionStatus {
    public data object Idle : ActionStatus
    public data class InProgress(val integrationId: String, val message: String) : ActionStatus
    public data class Success(val integrationId: String, val message: String) : ActionStatus
    public data class Failure(val integrationId: String, val message: String) : ActionStatus
}
