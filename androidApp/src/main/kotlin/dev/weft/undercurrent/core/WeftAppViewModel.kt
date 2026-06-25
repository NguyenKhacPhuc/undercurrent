package dev.weft.undercurrent.core

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import dev.weft.android.WeftRuntime
import dev.weft.harness.agents.AgentIntent
import dev.weft.harness.skills.SkillRegistry
import dev.weft.harness.skills.withHelp
import dev.weft.undercurrent.app.AppViewModel
import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.domain.UiBridgeRepository
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.navigation.NavBackStack
import dev.weft.undercurrent.core.navigation.NavigationChannel
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.chat.components.DisplayMessage
import dev.weft.undercurrent.feature.chat.SkillSummary
import dev.weft.undercurrent.feature.chat.agent.AgentSession
import dev.weft.undercurrent.feature.chat.agent.AgentSlot
import dev.weft.undercurrent.feature.chat.agent.WeftAgentFactory
import dev.weft.undercurrent.feature.chat.agent.keyAlias
import dev.weft.undercurrent.shared.mvi.MviContext
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class WeftAppViewModel(
    private val runtime: WeftRuntime,
    themeRepo: ThemeRepository,
    private val onboardingRepo: OnboardingRepository,
    private val providerPrefsRepo: ProviderPrefsRepository,
    navigationChannel: NavigationChannel,
    private val navigationVm: NavigationViewModel,
    agentSlot: AgentSlot,
    private val agentFactory: WeftAgentFactory,
    private val chatVm: ChatViewModel,
    uiBridgeRepo: UiBridgeRepository,
    private val sessionTokenStore: dev.weft.undercurrent.core.domain.SessionTokenStore,
    private val promptConfig: dev.weft.undercurrent.core.domain.prompt.PromptConfigRepository,
) : MviViewModel<AppState, Nothing, AppEffect>(
    initialState = AppState.initial(),
), AppViewModel {

    override val displayMessages: SnapshotStateList<DisplayMessage>
        get() = chatVm.displayMessages

    override val backStack: NavBackStack<Screen> get() = navigationVm.backStack

    private val skillRegistry: SkillRegistry = SkillRegistry(skills = emptyList()).withHelp()
    override val skills: List<SkillSummary> =
        skillRegistry.all().map { SkillSummary(it.name, it.description) }

    val mviContext: MviContext<AppState, AppEffect> =
        object : MviContext<AppState, AppEffect> {
            override val current: AppState get() = this@WeftAppViewModel.current
            override val scope: CoroutineScope get() = viewModelScope
            override fun update(reducer: (AppState) -> AppState) =
                this@WeftAppViewModel.update(reducer)
            override fun emit(effect: AppEffect) =
                this@WeftAppViewModel.emit(effect)
        }

    val agentSession: AgentSession = AgentSession(
        context = mviContext,
        navigationVm = navigationVm,
        agentSlot = agentSlot,
    )

    init {
        themeRepo.prefsFlow.collectInto { copy(themePrefs = it) }
        onboardingRepo.completedFlow.collectInto { copy(onboardingCompleted = it) }
        providerPrefsRepo.activeProvider.collectInto { copy(activeProvider = it) }
        providerPrefsRepo.defaultTier.collectInto { copy(defaultTier = it) }
        navigationChannel.requests.observe { screen ->
            navigationVm.dispatch(NavigationIntent.Navigate(screen))
        }
        snapshotFlow { backStack.lastOrNull() }.observe { top ->
            val next = top ?: Screen.Loading
            if (current.screen != next) {
                update { it.copy(previousScreen = it.screen, screen = next) }
            }
        }
        uiBridgeRepo.renderEvents.observe { _ ->
            if (current.screen !is Screen.RenderedTree) {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.RenderedTree))
            }
        }
    }

    override fun resume() {
        launch { handleResume() }
    }

    override fun dismissPermissionDialog() {
        update { it.copy(pendingPermissionDialog = null) }
    }

    private suspend fun handleResume() {
        // Sign-in gate (mobile-auth-wiring/05 D7): no stored BE session
        // → land on the SignIn screen before any other onboarding step.
        // The boot cascade re-runs after a successful sign-in via
        // SignInRoute.onSignedIn → AppViewModel.resume().
        val sessionToken = withContext(Dispatchers.IO) { sessionTokenStore.read() }
        if (sessionToken == null) {
            agentSession.setRootScreen(Screen.SignIn)
            return
        }
        // Prompt gate (backend-driven-prompt D4): no compiled-in fallback, so
        // block until a base prompt is available. The PromptSetup screen owns
        // the fetch/retry; on success it re-runs this cascade via resume().
        if (promptConfig.current.first() == null) {
            agentSession.setRootScreen(Screen.PromptSetup)
            return
        }
        val onboardingDone = onboardingRepo.completedFlow.first()
        if (!onboardingDone) {
            agentSession.setRootScreen(Screen.Onboarding)
            return
        }
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val storedKey = withContext(Dispatchers.IO) {
            runtime.keyVault.get(activeProvider.keyAlias())
        }
        if (storedKey == null) {
            agentSession.setRootScreen(Screen.KeyPaste)
            return
        }
        val agentSummaries = runtime.agentDeclarations.values
            .filter { it.userAddressable }
            .map {
                dev.weft.undercurrent.core.model.AgentSummary(
                    it.name,
                    it.displayName,
                    it.description,
                )
            }
        val a = agentFactory.build(
            agentName = current.activeAgentName,
            provider = activeProvider,
            apiKey = storedKey,
        )
        a.dispatchAndAwait(AgentIntent.Resume())
        agentSession.setAgent(a, screen = Screen.Chat, availableAgents = agentSummaries)
        chatVm.reload(a.state.value.conversationId)
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        chatVm.sendUiEvent(action, sourceLabel, fieldValues)
    }

    override suspend fun saveKey(key: String) {
        val p = providerPrefsRepo.activeProviderNow()
        runtime.keyVault.put(p.keyAlias(), key)
    }

    override fun dispatch(intent: Nothing) = launch { }
}
