package dev.weft.undercurrent.app

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.navigation.NavBackStack
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.chat.components.DisplayMessage
import dev.weft.undercurrent.feature.chat.SkillSummary
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.providers.ProviderIntent
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class IosAppViewModel(
    private val keyVault: KeyVaultRepository,
    private val onboardingRepo: OnboardingRepository,
    themeRepo: ThemeRepository,
    private val providerPrefsRepo: ProviderPrefsRepository,
    private val navigationVm: NavigationViewModel,
    private val chatVm: ChatViewModel,
) : MviViewModel<AppState, Nothing, AppEffect>(
    initialState = AppState.initial(),
), AppViewModel {

    override val displayMessages: SnapshotStateList<DisplayMessage>
        get() = chatVm.displayMessages

    override val skills: List<SkillSummary> = emptyList()

    override val backStack: NavBackStack<Screen> get() = navigationVm.backStack

    init {
        snapshotFlow { backStack.lastOrNull() }.observe { top ->
            val next = top ?: Screen.Loading
            if (current.screen != next) {
                update { it.copy(previousScreen = it.screen, screen = next) }
            }
        }
        themeRepo.prefsFlow.collectInto { copy(themePrefs = it) }
        onboardingRepo.completedFlow.collectInto { copy(onboardingCompleted = it) }
        providerPrefsRepo.activeProvider.observe { provider ->
            update { it.copy(activeProvider = provider) }
            refreshProviderKeyStatus()
        }
        providerPrefsRepo.defaultTier.collectInto { copy(defaultTier = it) }
        chatVm.state
            .map { it.agentReady }
            .distinctUntilChanged()
            .collectInto { copy(agentReady = it) }
        chatVm.state
            .map { it.currentConversationId }
            .distinctUntilChanged()
            .collectInto { copy(currentConversationId = it) }
    }

    private fun setRootScreen(screen: Screen) {
        navigationVm.dispatch(NavigationIntent.NavigateAndClear(screen))
    }

    @Suppress("UnusedParameter")
    override fun dispatch(intent: Nothing) = launch { }

    override fun resume() {
        launch { handleResume() }
    }

    override fun dismissPermissionDialog() {
        update { it.copy(pendingPermissionDialog = null) }
    }

    internal fun dispatchMiniApp(intent: MiniAppIntent) {
        when (intent) {
            is MiniAppIntent.InvokeMiniApp -> launch { chatVm.send(intent.triggerPrompt) }
            is MiniAppIntent.UiBridgeUpdate -> Unit
        }
    }

    internal fun dispatchProvider(intent: ProviderIntent) {
        when (intent) {
            is ProviderIntent.SetProvider -> launch {
                providerPrefsRepo.setProvider(intent.provider)
                val hasKey = withContext(Dispatchers.Default) {
                    runCatching { keyVault.hasApiKey(intent.provider) }.getOrDefault(false)
                }
                if (!hasKey) {
                    setRootScreen(Screen.KeyPaste)
                    chatVm.clear()
                }
            }
            is ProviderIntent.SubmitKey -> launch {
                val provider = providerPrefsRepo.activeProviderNow()
                runCatching { keyVault.putApiKey(provider, intent.key) }
                    .onFailure { t ->
                        emit(
                            AppEffect.Error(
                                "Couldn't save key: ${t.message ?: t::class.simpleName.orEmpty()}",
                            ),
                        )
                        return@launch
                    }
                refreshProviderKeyStatus()
                chatVm.resume()
            }
            is ProviderIntent.SaveProviderKey -> launch {
                runCatching { keyVault.putApiKey(intent.provider, intent.apiKey) }
                    .onFailure { t ->
                        emit(
                            AppEffect.Error(
                                "Couldn't save key: ${t.message ?: t::class.simpleName.orEmpty()}",
                            ),
                        )
                    }
                refreshProviderKeyStatus()
            }
            is ProviderIntent.RemoveProviderKey -> launch {
                runCatching { keyVault.clearApiKey(intent.provider) }
                refreshProviderKeyStatus()
                if (intent.provider == providerPrefsRepo.activeProviderNow()) {
                    setRootScreen(Screen.KeyPaste)
                    chatVm.clear()
                }
            }
            is ProviderIntent.SetDefaultTier -> launch {
                providerPrefsRepo.setDefaultTier(intent.tier)
            }
            is ProviderIntent.SetModelForTier -> Unit
        }
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) = Unit

    override suspend fun saveKey(key: String) {
        val provider = providerPrefsRepo.activeProviderNow()
        withContext(Dispatchers.Default) {
            runCatching { keyVault.putApiKey(provider, key) }
        }
        refreshProviderKeyStatus()
    }

    private suspend fun handleResume() {
        val onboardingDone = onboardingRepo.completedFlow.first()
        if (!onboardingDone) {
            setRootScreen(Screen.Onboarding)
            return
        }
        val provider = providerPrefsRepo.activeProviderNow()
        val hasKey = withContext(Dispatchers.Default) {
            runCatching { keyVault.hasApiKey(provider) }.getOrDefault(false)
        }
        if (!hasKey) {
            setRootScreen(Screen.KeyPaste)
            return
        }
        chatVm.resume()
    }

    private suspend fun refreshProviderKeyStatus() {
        val status = withContext(Dispatchers.Default) {
            buildMap {
                dev.weft.undercurrent.core.model.ProviderKind.entries.forEach { provider ->
                    if (runCatching { keyVault.hasApiKey(provider) }.getOrDefault(false)) {
                        put(provider, "•••")
                    }
                }
            }
        }
        update { it.copy(providerKeyStatus = status) }
    }
}
