package dev.weft.undercurrent.feature.miniapps

import dev.weft.contracts.UiBridge
import dev.weft.contracts.UIUpdate
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.feature.miniapps.internal.WeftMiniAppViewModel
import dev.weft.undercurrent.shared.mvi.MviContext
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * The HTML mini-app invocation paths — consent gate, instant render, and
 * the approve/deny grant flow. These ran only on Android until the
 * orchestrator was lifted to commonMain; iOS no-opped them. They assert
 * the shared behavior both platforms now run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeftMiniAppInvokeTest : BehaviorSpec({

    fun htmlMiniApp(scopes: Set<String> = emptySet(), consentedAt: Long? = null) = MiniApp(
        id = "feature.html1",
        name = "Tracker",
        emoji = "📊",
        triggerPrompt = "",
        createdAtEpochMs = 0L,
        htmlDocument = "<html><body>hi</body></html>",
        declaredScopes = scopes,
        consentedAt = consentedAt,
    )

    class CapturingContext(override val scope: CoroutineScope) : MviContext<AppState, AppEffect> {
        var state: AppState = AppState.initial()
        override val current: AppState get() = state
        override fun update(reducer: (AppState) -> AppState) { state = reducer(state) }
        override fun emit(effect: AppEffect) = Unit
    }

    fun vmWith(
        scope: CoroutineScope,
        repo: MiniAppsRepository,
        uiBridge: UiBridge,
        context: MviContext<AppState, AppEffect>,
    ) = WeftMiniAppViewModel(
        context = context,
        uiBridge = uiBridge,
        miniAppsRepo = repo,
        navigationVm = mockk<NavigationViewModel>(relaxed = true),
        offerable = OfferableActions.readMostlyDefaults(),
        sendChat = { },
    )

    Given("an HTML mini-app that declares scopes and hasn't been consented") {
        When("it is invoked") {
            Then("the consent prompt is shown and nothing renders yet") {
                runTest {
                    val miniApp = htmlMiniApp(scopes = setOf("http_fetch"))
                    val repo = mockk<MiniAppsRepository>(relaxed = true) {
                        every { miniApps } returns MutableStateFlow(listOf(miniApp))
                    }
                    val uiBridge = mockk<UiBridge>(relaxed = true)
                    val ctx = CapturingContext(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

                    vmWith(ctx.scope, repo, uiBridge, ctx)
                        .dispatch(MiniAppIntent.InvokeMiniApp(miniAppId = miniApp.id, triggerPrompt = ""))
                    advanceUntilIdle()

                    ctx.state.pendingMiniAppConsent.shouldNotBeNull()
                    coVerify(exactly = 0) { uiBridge.emit(any()) }
                    coVerify(exactly = 0) { repo.recordUsage(any()) }
                }
            }
        }
    }

    Given("an HTML mini-app that declares no scopes") {
        When("it is invoked") {
            Then("it renders immediately with no consent prompt") {
                runTest {
                    val miniApp = htmlMiniApp(scopes = emptySet())
                    val repo = mockk<MiniAppsRepository>(relaxed = true) {
                        every { miniApps } returns MutableStateFlow(listOf(miniApp))
                    }
                    val uiBridge = mockk<UiBridge>(relaxed = true)
                    val ctx = CapturingContext(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

                    vmWith(ctx.scope, repo, uiBridge, ctx)
                        .dispatch(MiniAppIntent.InvokeMiniApp(miniAppId = miniApp.id, triggerPrompt = ""))
                    advanceUntilIdle()

                    ctx.state.pendingMiniAppConsent shouldBe null
                    coVerify(exactly = 1) { uiBridge.emit(any<UIUpdate.RenderTree>()) }
                    coVerify(exactly = 1) { repo.recordUsage(miniApp.id) }
                }
            }
        }
    }

    Given("a pending consent prompt for an HTML mini-app") {
        When("the user approves") {
            Then("its declared-and-offerable scopes are recorded and it renders") {
                runTest {
                    val miniApp = htmlMiniApp(scopes = setOf("http_fetch"))
                    val repo = mockk<MiniAppsRepository>(relaxed = true) {
                        every { miniApps } returns MutableStateFlow(listOf(miniApp))
                    }
                    val uiBridge = mockk<UiBridge>(relaxed = true)
                    val ctx = CapturingContext(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

                    vmWith(ctx.scope, repo, uiBridge, ctx)
                        .dispatch(MiniAppIntent.ApproveConsent(miniApp.id))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.recordConsent(miniApp.id, setOf("http_fetch")) }
                    coVerify(exactly = 1) { uiBridge.emit(any<UIUpdate.RenderTree>()) }
                }
            }
        }
        When("the user denies") {
            Then("an empty grant is recorded and it still renders") {
                runTest {
                    val miniApp = htmlMiniApp(scopes = setOf("http_fetch"))
                    val repo = mockk<MiniAppsRepository>(relaxed = true) {
                        every { miniApps } returns MutableStateFlow(listOf(miniApp))
                    }
                    val uiBridge = mockk<UiBridge>(relaxed = true)
                    val ctx = CapturingContext(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

                    vmWith(ctx.scope, repo, uiBridge, ctx)
                        .dispatch(MiniAppIntent.DenyConsent(miniApp.id))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.recordConsent(miniApp.id, emptySet()) }
                    coVerify(exactly = 1) { uiBridge.emit(any<UIUpdate.RenderTree>()) }
                }
            }
        }
    }
})
