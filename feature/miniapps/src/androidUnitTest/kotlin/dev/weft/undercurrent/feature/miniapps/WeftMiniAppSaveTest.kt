package dev.weft.undercurrent.feature.miniapps

import dev.weft.contracts.ComponentNode
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.feature.miniapps.internal.WeftMiniAppViewModel
import dev.weft.undercurrent.shared.mvi.MviContext
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * The "Save as mini-app" flow used to live in the Composable
 * (`MainActivity`), which injected the repo and serialized the tree
 * inline. It now routes through [MiniAppIntent.SaveCurrentRenderAsMiniApp]
 * on [WeftMiniAppViewModel]; these lock that handler's repo interaction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeftMiniAppSaveTest : BehaviorSpec({

    fun fakeContext(scope: CoroutineScope) = object : MviContext<AppState, AppEffect> {
        override val current: AppState = AppState.initial()
        override val scope: CoroutineScope = scope
        override fun update(reducer: (AppState) -> AppState) = Unit
        override fun emit(effect: AppEffect) = Unit
    }

    fun vmWith(scope: CoroutineScope, repo: MiniAppsRepository) = WeftMiniAppViewModel(
        context = fakeContext(scope),
        uiBridge = mockk(relaxed = true),
        miniAppsRepo = repo,
        navigationVm = mockk(relaxed = true),
        offerable = OfferableActions(emptyList()),
        sendChat = { },
    )

    Given("a save request carrying the on-screen tree") {
        Then("repo.add then repo.setCachedRender(createdId, json) fire once each") {
            runTest {
                val repo = mockk<MiniAppsRepository>(relaxed = true)
                coEvery { repo.add("Trip", "✈️", "plan a trip") } returns MiniApp(
                    id = "feature.abc123",
                    name = "Trip",
                    emoji = "✈️",
                    triggerPrompt = "plan a trip",
                    createdAtEpochMs = 0L,
                )

                vmWith(CoroutineScope(UnconfinedTestDispatcher(testScheduler)), repo).dispatch(
                    MiniAppIntent.SaveCurrentRenderAsMiniApp(
                        name = "Trip",
                        emoji = "✈️",
                        triggerPrompt = "plan a trip",
                        renderedTree = ComponentNode(type = "Column"),
                    ),
                )
                advanceUntilIdle()

                coVerify(exactly = 1) { repo.add("Trip", "✈️", "plan a trip") }
                coVerify(exactly = 1) { repo.setCachedRender("feature.abc123", any()) }
            }
        }
    }

    Given("a save request with no rendered tree") {
        Then("repo.add fires but setCachedRender does not (it re-runs the agent instead)") {
            runTest {
                val repo = mockk<MiniAppsRepository>(relaxed = true)
                coEvery { repo.add(any(), any(), any()) } returns MiniApp(
                    id = "feature.x",
                    name = "n",
                    emoji = "e",
                    triggerPrompt = "p",
                    createdAtEpochMs = 0L,
                )

                vmWith(CoroutineScope(UnconfinedTestDispatcher(testScheduler)), repo).dispatch(
                    MiniAppIntent.SaveCurrentRenderAsMiniApp(
                        name = "n",
                        emoji = "e",
                        triggerPrompt = "p",
                        renderedTree = null,
                    ),
                )
                advanceUntilIdle()

                coVerify(exactly = 1) { repo.add("n", "e", "p") }
                coVerify(exactly = 0) { repo.setCachedRender(any(), any()) }
            }
        }
    }
})
