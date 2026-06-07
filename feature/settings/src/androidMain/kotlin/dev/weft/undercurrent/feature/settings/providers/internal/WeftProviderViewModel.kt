package dev.weft.undercurrent.feature.settings.providers.internal

import dev.weft.android.WeftRuntime
import dev.weft.harness.agents.AgentIntent
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.chat.agent.AgentSession
import dev.weft.undercurrent.feature.chat.agent.WeftAgentFactory
import dev.weft.undercurrent.feature.chat.agent.keyAlias
import dev.weft.undercurrent.feature.settings.providers.ProviderIntent
import dev.weft.undercurrent.feature.settings.providers.ProviderState
import dev.weft.undercurrent.feature.settings.providers.ProviderStateStore
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import dev.weft.undercurrent.shared.mvi.MviContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provider lifecycle (Android impl): pick the active provider, save /
 * remove keys, default tier + per-tier model overrides. Provider
 * mutations that affect the active agent rebuild it via
 * [WeftAgentFactory] and install it via [AgentSession].
 *
 * Refreshes the [AppState.providerKeyStatus] map after every
 * key-vault write so the providers screen stays in sync.
 */
public class WeftProviderViewModel(
    private val context: MviContext<AppState, AppEffect>,
    private val runtime: WeftRuntime,
    private val agentSession: AgentSession,
    private val agentFactory: WeftAgentFactory,
    private val providerPrefsRepo: ProviderPrefsRepository,
    private val modelPrefsRepo: ModelPrefsRepository,
    private val chatVm: ChatViewModel,
    keyVaultRepo: KeyVaultRepository,
    modelCatalog: ModelCatalogRepository,
    validator: KeyValidationRepository,
) : ProviderViewModel {
    private val store = ProviderStateStore(
        providerPrefs = providerPrefsRepo,
        modelPrefs = modelPrefsRepo,
        catalog = modelCatalog,
        keyVault = keyVaultRepo,
        validator = validator,
    )

    override val state: StateFlow<ProviderState> = store.state

    override fun dispatch(intent: ProviderIntent) {
        when (intent) {
            is ProviderIntent.SubmitKey ->
                context.scope.launch { handleSubmitKey(intent.key); store.refreshKeyStatus() }
            is ProviderIntent.SetProvider ->
                context.scope.launch { handleSetProvider(intent.provider) }
            is ProviderIntent.ValidateAndSaveProviderKey ->
                store.validateAndSave(intent.provider, intent.apiKey) {
                    handleSaveProviderKey(intent.provider, intent.apiKey)
                }
            is ProviderIntent.SaveProviderKey ->
                context.scope.launch {
                    handleSaveProviderKey(intent.provider, intent.apiKey)
                    store.markKeyPresent(intent.provider)
                }
            is ProviderIntent.ClearKeyValidation -> store.clearValidation()
            is ProviderIntent.RemoveProviderKey -> {
                context.scope.launch { handleRemoveProviderKey(intent.provider) }
                store.markKeyRemoved(intent.provider)
            }
            is ProviderIntent.SetDefaultTier ->
                context.scope.launch { providerPrefsRepo.setDefaultTier(intent.tier) }
            is ProviderIntent.SetModelForTier -> context.scope.launch {
                handleSetModelForTier(intent.provider, intent.tier, intent.modelId)
            }
        }
    }

    /**
     * Snapshot the per-provider last-4 of the stored key for the
     * providers screen.
     */
    public suspend fun refreshKeyStatus() {
        val status = withContext(Dispatchers.IO) {
            buildMap {
                ProviderKind.entries.forEach { provider ->
                    val k = runtime.keyVault.get(provider.keyAlias())
                    if (!k.isNullOrBlank()) put(provider, k.takeLast(4))
                }
            }
        }
        context.update { it.copy(providerKeyStatus = status) }
    }

    private suspend fun handleSubmitKey(key: String) {
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val a = agentFactory.build(
            agentName = context.current.activeAgentName,
            provider = activeProvider,
            apiKey = key,
        )
        a.dispatchAndAwait(AgentIntent.Resume())
        refreshKeyStatus()
        agentSession.setAgent(a, screen = Screen.Chat)
    }

    private suspend fun handleSetProvider(provider: ProviderKind) {
        providerPrefsRepo.setProvider(provider)
        val key = withContext(Dispatchers.IO) {
            runtime.keyVault.get(provider.keyAlias())
        }
        if (key != null) {
            val a = agentFactory.build(
                agentName = context.current.activeAgentName,
                provider = provider,
                apiKey = key,
            )
            a.dispatchAndAwait(AgentIntent.Resume())
            agentSession.setAgent(a)
            chatVm.reload(a.state.value.conversationId)
        } else {
            agentSession.forgetAgent()
            context.update {
                it.copy(agentReady = false, currentConversationId = null)
            }
        }
    }

    private suspend fun handleSaveProviderKey(provider: ProviderKind, key: String) {
        withContext(Dispatchers.IO) {
            runtime.keyVault.put(provider.keyAlias(), key)
        }
        refreshKeyStatus()
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            val a = agentFactory.build(
                agentName = context.current.activeAgentName,
                provider = provider,
                apiKey = key,
            )
            a.dispatchAndAwait(AgentIntent.Resume())
            agentSession.setAgent(a)
            chatVm.reload(a.state.value.conversationId)
        }
    }

    private suspend fun handleRemoveProviderKey(provider: ProviderKind) {
        withContext(Dispatchers.IO) {
            runtime.keyVault.remove(provider.keyAlias())
        }
        refreshKeyStatus()
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            agentSession.forgetAgent()
            agentSession.setRootScreen(Screen.KeyPaste)
            context.update {
                it.copy(
                    agentReady = false,
                    currentConversationId = null,
                )
            }
        }
    }

    private suspend fun handleSetModelForTier(
        provider: ProviderKind,
        tier: ModelTier,
        modelId: String?,
    ) {
        modelPrefsRepo.setOverride(provider, tier, modelId)
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            val key = withContext(Dispatchers.IO) {
                runtime.keyVault.get(provider.keyAlias())
            } ?: return
            val a = agentFactory.build(
                agentName = context.current.activeAgentName,
                provider = provider,
                apiKey = key,
            )
            a.dispatchAndAwait(AgentIntent.Resume())
            agentSession.setAgent(a)
            chatVm.reload(a.state.value.conversationId)
        }
    }
}
