package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises [MiniAppsStore] in BDD style.
 *
 * Each intent (Add, Update, Delete) translates to a single suspend
 * call on the repo. The store doesn't optimistically update local
 * state — it waits for the repo's StateFlow to re-emit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MiniAppsStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun miniApp(
        id: String,
        name: String = "App $id",
        emoji: String = "🦋",
        prompt: String = "do $id",
    ): MiniApp = MiniApp(
        id = id,
        name = name,
        emoji = emoji,
        triggerPrompt = prompt,
        createdAtEpochMs = 0L,
    )

    fun fakeRepo(initial: List<MiniApp> = emptyList()): MiniAppsRepository {
        val repo = mockk<MiniAppsRepository>()
        every { repo.miniApps } returns MutableStateFlow(initial)
        coEvery { repo.add(any(), any(), any()) } returns miniApp("added")
        coEvery { repo.update(any(), any(), any(), any()) } returns Unit
        coEvery { repo.delete(any()) } returns Unit
        return repo
    }

    Given("a repo seeded with two mini-apps") {
        val seed = listOf(miniApp("a"), miniApp("b"))
        val store = MiniAppsStore(fakeRepo(initial = seed))

        Then("the initial state mirrors the seed list") {
            store.state.value shouldBe MiniAppsState(miniApps = seed)
        }
    }

    Given("a repo with no mini-apps") {
        val store = MiniAppsStore(fakeRepo())

        Then("the initial state has an empty list") {
            store.state.value shouldBe MiniAppsState(miniApps = emptyList())
        }
    }

    Given("a store subscribed to a mutable mini-apps flow") {
        val flow = MutableStateFlow<List<MiniApp>>(emptyList())
        val repo = mockk<MiniAppsRepository>().apply {
            every { miniApps } returns flow
            coEvery { add(any(), any(), any()) } returns miniApp("added")
            coEvery { update(any(), any(), any(), any()) } returns Unit
            coEvery { delete(any()) } returns Unit
        }
        val store = MiniAppsStore(repo)

        When("the repo emits a new list") {
            Then("the store's state reflects the new list") {
                runTest {
                    advanceUntilIdle()
                    flow.value = listOf(miniApp("after"))
                    advanceUntilIdle()

                    store.state.value.miniApps shouldBe listOf(miniApp("after"))
                }
            }
        }
    }

    Given("a fresh store with a stubbed repo") {
        When("Add(name='Daily standup', emoji='📝', triggerPrompt='…') is dispatched") {
            Then("repo.add is called with those exact arguments") {
                runTest {
                    val repo = fakeRepo()
                    val store = MiniAppsStore(repo)

                    store.dispatch(
                        MiniAppsIntent.Add(
                            name = "Daily standup",
                            emoji = "📝",
                            triggerPrompt = "Generate today's standup notes",
                        ),
                    )
                    advanceUntilIdle()

                    coVerify(exactly = 1) {
                        repo.add("Daily standup", "📝", "Generate today's standup notes")
                    }
                    coVerify(exactly = 0) { repo.update(any(), any(), any(), any()) }
                    coVerify(exactly = 0) { repo.delete(any()) }
                }
            }
        }

        When("Update(id, name, emoji, triggerPrompt) is dispatched") {
            Then("repo.update is called with the four fields verbatim") {
                runTest {
                    val repo = fakeRepo()
                    val store = MiniAppsStore(repo)

                    store.dispatch(
                        MiniAppsIntent.Update(
                            id = "feature.legacy-id",
                            name = "Renamed",
                            emoji = "✏️",
                            triggerPrompt = "Updated prompt",
                        ),
                    )
                    advanceUntilIdle()

                    coVerify(exactly = 1) {
                        repo.update("feature.legacy-id", "Renamed", "✏️", "Updated prompt")
                    }
                    coVerify(exactly = 0) { repo.add(any(), any(), any()) }
                    coVerify(exactly = 0) { repo.delete(any()) }
                }
            }
        }

        When("Delete is dispatched for id 'miniapp-42'") {
            Then("repo.delete is called once with that id") {
                runTest {
                    val repo = fakeRepo()
                    val store = MiniAppsStore(repo)

                    store.dispatch(MiniAppsIntent.Delete("miniapp-42"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.delete("miniapp-42") }
                    coVerify(exactly = 0) { repo.add(any(), any(), any()) }
                    coVerify(exactly = 0) { repo.update(any(), any(), any(), any()) }
                }
            }
        }

        When("Add, Update, Delete are dispatched in sequence") {
            Then("each method fires exactly once with its own arguments") {
                runTest {
                    val repo = fakeRepo()
                    val store = MiniAppsStore(repo)

                    store.dispatch(MiniAppsIntent.Add("Cat", "🐈", "do cat"))
                    store.dispatch(MiniAppsIntent.Update("id1", "Dog", "🐕", "do dog"))
                    store.dispatch(MiniAppsIntent.Delete("id2"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.add("Cat", "🐈", "do cat") }
                    coVerify(exactly = 1) { repo.update("id1", "Dog", "🐕", "do dog") }
                    coVerify(exactly = 1) { repo.delete("id2") }
                }
            }
        }
    }

    Given("a seeded store with an Update dispatched") {
        Then("local state stays at the seed — the store waits for repo to re-emit") {
            runTest {
                val seed = listOf(miniApp("solo", name = "Original"))
                val repo = fakeRepo(initial = seed)
                val store = MiniAppsStore(repo)
                advanceUntilIdle()

                store.dispatch(MiniAppsIntent.Update("solo", "Renamed", "✏️", "new prompt"))
                advanceUntilIdle()

                store.state.value.miniApps shouldBe seed
            }
        }
    }
})
